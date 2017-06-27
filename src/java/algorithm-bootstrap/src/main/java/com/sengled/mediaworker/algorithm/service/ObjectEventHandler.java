package com.sengled.mediaworker.algorithm.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.Subscribe;
import com.sengled.mediaworker.algorithm.ObjectType;
import com.sengled.mediaworker.algorithm.ProcessorManager;
import com.sengled.mediaworker.algorithm.event.ObjectEvent;
import com.sengled.mediaworker.algorithm.exception.S3RuntimeException;
import com.sengled.mediaworker.algorithm.exception.SqsRuntimeException;
import com.sengled.mediaworker.algorithm.service.dto.AlgorithmResult;
import com.sengled.mediaworker.algorithm.service.dto.ObjectRecognitionInnerDto;
import com.sengled.mediaworker.algorithm.service.dto.ObjectRecognitionResult.Object;
import com.sengled.mediaworker.s3.AmazonS3Template;
import com.sengled.mediaworker.sqs.SQSTemplate;

@Component
public class ObjectEventHandler implements InitializingBean {
	private static final Logger LOGGER = LoggerFactory.getLogger(ObjectEventHandler.class);
	private static final int THREAD_MAXCOUNT = 150;

	@Value("${AWS_SERVICE_NAME_PREFIX}_${sqs.algorithm.result.queue}")
	private String queue;
	@Value("${aws_screenshot_bucket}")
	private String bucketName;

	@Autowired
	private SQSTemplate sqsTemplate;
	@Autowired
	private AmazonS3Template amazonS3Template;
	@Autowired
	ProcessorManager  processorManagerImpl;
	
	private ThreadPoolExecutor pool;

	@Override
	public void afterPropertiesSet() throws Exception {
		ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(THREAD_MAXCOUNT * 10);
    	pool = new ThreadPoolExecutor(20
										,THREAD_MAXCOUNT
										,60L,TimeUnit.SECONDS
										,queue
										,new ThreadPoolExecutor.CallerRunsPolicy());
	}
	

	@Subscribe
	public void feedEvent(ObjectEvent event) {
		pool.submit(new Runnable() {
			@Override
			public void run() {
				handle(event);
			}
		});
	}

	private void handle(ObjectEvent event) {
		LOGGER.info("Handle ObjectEvent {}",event);
		String token = event.getToken();
		Date utcDateTime = event.getUtcDate();
		try {
			String imageS3Path = token + "_object_" + utcDateTime.getTime() + ".jpg";
			byte[] jpgData = event.getJpgData();
			saveS3(imageS3Path, jpgData);
			putSqs(utcDateTime, token, imageS3Path, event.getResult());
		} catch (S3RuntimeException e) {
			LOGGER.error(e.getMessage(),e);
		} catch (SqsRuntimeException e) {
			LOGGER.error(e.getMessage(),e);
		}
		
		
	}
	private void saveS3(String imageS3Path,byte[] jpgData) throws S3RuntimeException{
		LOGGER.info("imageS3Path:" + imageS3Path);
		LOGGER.info("saveS3:{}",imageS3Path);
		try {
			amazonS3Template.putObject(bucketName, imageS3Path, jpgData);
		} catch (Exception e) {
			throw new S3RuntimeException(e.getMessage(),e);
		}
	}
	private void putSqs(Date utcDateTime,String token,String imageS3Path,Multimap<Integer, Object> zoneToObject) throws SqsRuntimeException{
		LOGGER.info("putsqs:{}",zoneToObject);
		AlgorithmResult result;
		try {
			result = new AlgorithmResult();
			List<ObjectRecognitionInnerDto> dataList = new ArrayList<>();
			Map<Integer, Collection<Object>> zoneToObjectMap = zoneToObject.asMap();
			for (Entry<Integer, Collection<Object>> entry : zoneToObjectMap.entrySet()) {
				int zoneid = entry.getKey();
				for (Object object : entry.getValue()) {
					String pos = object.bbox_pct.get(0) + ","+object.bbox_pct.get(1)+","+object.bbox_pct.get(2)+","+object.bbox_pct.get(3);
					int targetType = ObjectType.findByName(object.type).value;
					ObjectRecognitionInnerDto orid = new ObjectRecognitionInnerDto(Long.valueOf(zoneid),pos , targetType, object.score);
					dataList.add(orid);
				}
			}
			
			result.setEventType(AlgorithmResult.SLS_EVENT_TYPE_OBJECT);
			result.setDataList(dataList);
			result.setStreamId(token);
			result.setBigImage(imageS3Path);
			result.setSmallImage(imageS3Path);
			result.setTimeStamp(DateFormatUtils.format(utcDateTime, "yyyy-MM-dd HH:mm:ss"));
			
			sqsTemplate.publish(queue, JSON.toJSONString(result));
			LOGGER.info("putsqs:{}",JSON.toJSONString(result));
		} catch (Exception e) {
			throw new SqsRuntimeException(e.getMessage(),e);
		}

	
	
	}
	
}
