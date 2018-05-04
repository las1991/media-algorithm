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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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
import com.sengled.mediaworker.algorithm.service.dto.AlgorithmResult;
import com.sengled.mediaworker.s3.AmazonS3Template;
import com.sengled.mediaworker.sqs.SQSTemplate;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

@Component
public class PutManager implements InitializingBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(PutManager.class);
    
    private static final int MAX_MERGE_FILE_NUM = 5;
    
    private static final int THREAD_MAXCOUNT = 150;

    @Value("${AWS_SERVICE_NAME_PREFIX}_${sqs.algorithm.result.queue}")
    private String queue;
    
    @Value("${aws_screenshot_bucket}")
    private String bucketName;

    @Autowired
    private SQSTemplate sqsTemplate;

    @Autowired
    private AmazonS3Template amazonS3Template;
    
    private ThreadPoolExecutor putThreadPool;

    // 定时执行合并,上传S3及sqs
    Set<ImageS3Info> imageS3InfoSet1day = Sets.newConcurrentHashSet();
    Set<ImageS3Info> imageS3InfoSet2day = Sets.newConcurrentHashSet();
    Set<ImageS3Info> imageS3InfoSet7day = Sets.newConcurrentHashSet();
    Set<ImageS3Info> imageS3InfoSet30day = Sets.newConcurrentHashSet();

    Timer timer = new Timer();

    @Override
    public void afterPropertiesSet() throws Exception {
        ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(THREAD_MAXCOUNT * 10);
        putThreadPool = new ThreadPoolExecutor(20
                                        ,THREAD_MAXCOUNT
                                        ,60L,TimeUnit.SECONDS
                                        ,queue
                                        ,new ThreadPoolExecutor.CallerRunsPolicy());
        
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
        List<AlgorithmResult> arList = new ArrayList<>();
        long offset = 0;
        int count = 0;
        while (imageIterator.hasNext()) {
            ImageS3Info imageS3Info = imageIterator.next();
            imageIterator.remove();
            
            buf.writeBytes(imageS3Info.getJpgData());
            int size = imageS3Info.getJpgData().length;
            AlgorithmResult result = imageS3Info.getResult();
            result.setRangeStr("@"+offset+"-"+(offset+size));
            arList.add(result);
            LOGGER.info("begin:{},size:{}", offset, size);
            
            if (++count >= MAX_MERGE_FILE_NUM || ! imageIterator.hasNext()) {// 上传一次
                String filePrefix = UUID.randomUUID().toString().replace("-", "").toUpperCase();
                String fileSuffix = ".dat";
                List<Serializable> list = new ArrayList<>();
                arList.stream().forEach(new Consumer<AlgorithmResult>() {
                    @Override
                    public void accept(AlgorithmResult t) {
                        t.setBigImage(filePrefix + t.getRangeStr() + fileSuffix);
                        t.setSmallImage(t.getBigImage());
                        list.add(JSON.toJSONString(t));
                    }
                });
                byte[] data = new byte[buf.readableBytes()];
                buf.readBytes(data);
                Tag s3tag = imageS3Info.getS3tag();
                submit(list, filePrefix + fileSuffix, data, s3tag);

                // clear
                buf.clear();
                arList.clear();
                offset = 0;
                continue;
            }
            offset += size;
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
                LOGGER.info("put s3 sqs...");
                amazonS3Template.putObject(bucketName, s3key, data, Arrays.asList(s3tag));
                sqsTemplate.publish(queue, arList);
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
