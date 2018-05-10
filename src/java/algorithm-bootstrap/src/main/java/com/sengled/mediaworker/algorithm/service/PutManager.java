package com.sengled.mediaworker.algorithm.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson.JSON;
import com.amazonaws.services.s3.model.Tag;
import com.google.common.collect.Sets;
import com.sengled.mediaworker.RecordCounter;
import com.sengled.mediaworker.algorithm.exception.S3IOException;
import com.sengled.mediaworker.algorithm.service.dto.AlgorithmResult;
import com.sengled.mediaworker.s3.AmazonS3Template;
import com.sengled.mediaworker.sqs.SQSTemplate;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

@Component
public class PutManager implements InitializingBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(PutManager.class);
    
    private static final int MAX_MERGE_FILE_NUM = 10;
    
    private static final int THREAD_MAXCOUNT = 30;

    @Value("${AWS_SERVICE_NAME_PREFIX}_${sqs.algorithm.result.queue}")
    private String queue;
    
    @Value("${aws_screenshot_bucket}")
    private String bucketName;

    @Autowired
    private SQSTemplate sqsTemplate;

    @Autowired
    private AmazonS3Template amazonS3Template;
    
    @Autowired
    private RecordCounter recordCounter;
    
    private ExecutorService putThreadPool;

    // 定时执行合并,上传S3及sqs
    Set<ImageS3Info> imageS3InfoSet1day = Sets.newConcurrentHashSet();
    Set<ImageS3Info> imageS3InfoSet2day = Sets.newConcurrentHashSet();
    Set<ImageS3Info> imageS3InfoSet7day = Sets.newConcurrentHashSet();
    Set<ImageS3Info> imageS3InfoSet30day = Sets.newConcurrentHashSet();

    Timer timer = new Timer();

    @Override
    public void afterPropertiesSet() throws Exception {
        putThreadPool = Executors.newFixedThreadPool(THREAD_MAXCOUNT);
        
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                mergeAndUpload(imageS3InfoSet1day);
                mergeAndUpload(imageS3InfoSet2day);
                mergeAndUpload(imageS3InfoSet7day);
                mergeAndUpload(imageS3InfoSet30day);
            }
        }, 1000, 5000);
    }

    private void mergeAndUpload(Set<ImageS3Info> images) {
        Iterator<ImageS3Info> imageIterator = images.iterator();
        ByteBuf buf = Unpooled.buffer();
        long offset = 0;
        int count = 0;
        List<AlgorithmResult> arList = new ArrayList<>();
        while ( imageIterator.hasNext() ) {
            ImageS3Info imageS3Info = imageIterator.next();
            imageIterator.remove();
            
            //check
            if( imageS3Info.getJpgData() == null  || imageS3Info.getJpgData().length <=0 || imageS3Info.getResult() == null) {
                LOGGER.error("imageS3Info verify error.");
                continue;
            }
            //write jpgData to buf
            try {
                buf.writeBytes(imageS3Info.getJpgData());
                int size = imageS3Info.getJpgData().length;
                AlgorithmResult result = imageS3Info.getResult();
                result.setRangeStr("@"+offset+"-"+(offset+size));
                arList.add(result);
                offset += size;
            } catch (Exception e) {
                LOGGER.error("merge error." + e.getMessage(), e);
                continue;
            }

            //async put s3,sqs
            if (++count >= MAX_MERGE_FILE_NUM || ! imageIterator.hasNext()) {// 条件满足则上传一次（s3,sqs）
                String filePrefix = UUID.randomUUID().toString().replace("-", "").toUpperCase();
                String fileSuffix = ".dat";
                List<Serializable> AlgorithmResultJsonlist = new ArrayList<>();
                arList.stream().forEach(new Consumer<AlgorithmResult>() {
                    @Override
                    public void accept(AlgorithmResult t) {
                        t.setBigImage(filePrefix + t.getRangeStr() + fileSuffix);
                        t.setSmallImage(t.getBigImage());
                        AlgorithmResultJsonlist.add(JSON.toJSONString(t));
                    }
                });
                
                //async put
                byte[] imageData = new byte[buf.readableBytes()];
                buf.readBytes(imageData);
                submit(AlgorithmResultJsonlist, filePrefix + fileSuffix, imageData, imageS3Info.getS3tag());
                
                //reset buf
                buf.clear();
                arList.clear();
                offset = 0;
            }
        }
    }

    public void put1(ImageS3Info imageinfo) {
        imageS3InfoSet1day.add(imageinfo);
    }

    public void put2(ImageS3Info imageinfo) {
        imageS3InfoSet2day.add(imageinfo);
    }

    public void put7(ImageS3Info imageinfo) {
        imageS3InfoSet7day.add(imageinfo);
    }

    public void put30(ImageS3Info imageinfo) {
        imageS3InfoSet30day.add(imageinfo);
    }

    private void submit(final List<Serializable> arList,String s3key,final byte[] data,final Tag s3tag) {
        putThreadPool.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                LOGGER.info("put s3 key:{} sqs:{}" , s3key, arList);
                try {
                    amazonS3Template.putObject(bucketName, s3key, data, Arrays.asList(s3tag));
                    recordCounter.addAndGetS3SuccessfulCount(1);
                } catch (S3IOException e) {
                    LOGGER.error("put s3 error.",e);
                    recordCounter.addAndGetS3FailureCount(1);
                    return null;//上传s3失败，则退出
                }
                for (Serializable ar: arList) {
                    try {
                        sqsTemplate.publish(queue, ar);
                        recordCounter.addAndGetSqsSuccessfulCount(1);
                    } catch (Exception e) {
                        LOGGER.error("put sqs error.",e);
                        recordCounter.addAndGetSqsFailureCount(1);
                    }
                }
                
                return null;
            }
        });
    }
    public static class ImageS3Info {
        byte[] jpgData;
        Tag s3tag;
        AlgorithmResult result;

        public ImageS3Info(byte[] jpgData2, Tag s3tag, AlgorithmResult result) {
            this.jpgData = jpgData2;
            this.s3tag = s3tag;
            this.result = result;
        }

        public AlgorithmResult getResult() {
            return result;
        }

        public void setResult(AlgorithmResult result) {
            this.result = result;
        }
        
        public byte[] getJpgData() {
            return jpgData;
        }

        public void setJpgData(byte[] jpgData) {
            this.jpgData = jpgData;
        }

        public Tag getS3tag() {
            return s3tag;
        }

        public void setS3tag(Tag s3tag) {
            this.s3tag = s3tag;
        }
    }
}
