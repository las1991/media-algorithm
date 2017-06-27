package com.sengled.mediaworker.algorithm.service;

import java.util.Collections;
import java.util.Date;
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
import com.google.common.eventbus.Subscribe;
import com.sengled.mediaworker.RecordCounter;
import com.sengled.mediaworker.algorithm.event.MotionEvent;
import com.sengled.mediaworker.algorithm.exception.S3RuntimeException;
import com.sengled.mediaworker.algorithm.exception.SqsRuntimeException;
import com.sengled.mediaworker.algorithm.service.dto.AlgorithmResult;
import com.sengled.mediaworker.algorithm.service.dto.ObjectRecognitionInnerDto;
import com.sengled.mediaworker.s3.AmazonS3Template;
import com.sengled.mediaworker.sqs.SQSTemplate;

@Component
public class MotionEventHandler implements InitializingBean {
	private static final Logger LOGGER = LoggerFactory.getLogger(MotionEventHandler.class);
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
	RecordCounter recordCounter;
	
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
	

	/**
	 * motion事件
	 * 
	 * @param event
	 */
	@Subscribe
	public void feedEvent(MotionEvent event) {
		pool.submit(new Runnable() {
			@Override
			public void run() {
				handle(event);
			}
		});
	}


	private void handle(MotionEvent event) {
		LOGGER.info("Get MotionEvent:{}",event);
		Date utcDateTime = event.getUtcDate();
		byte[] jpgData = event.getJpgData();
		String token = event.getToken();
		String zoneId = event.getZoneId();
		String imageS3Path = token + "_motion_" + utcDateTime.getTime() + ".jpg";
		Exception ex = null;
		try {
			saveS3(imageS3Path, jpgData);
			recordCounter.addAndGetS3SuccessfulCount(1);
			//saveDynamodb(utcDateTime, token, imageS3Path, zoneId);
			putSqs(utcDateTime, token, zoneId,imageS3Path);
			recordCounter.addAndGetSqsSuccessfulCount(1);
		} catch (S3RuntimeException e) {
			ex = e;
			recordCounter.addAndGetS3FailureCount(1);
		} catch (SqsRuntimeException e) {
			ex = e;
			recordCounter.addAndGetSqsFailureCount(1);
		}
		if( null != ex){
			LOGGER.error("feedEvent failed.",ex);
			LOGGER.info("Token:{},imageS3Path:{},utcDateTime:{},zoneId:{}",token,imageS3Path,utcDateTime,zoneId);
		}
	
		LOGGER.info("Token:{},feedEvent finished",event.getToken());
	}

	private void saveS3(String imageS3Path,byte[] jpgData) throws S3RuntimeException{
		LOGGER.info("imageS3Path:" + imageS3Path);
		try {
			amazonS3Template.putObject(bucketName, imageS3Path, jpgData);
		} catch (Exception e) {
			throw new S3RuntimeException(e.getMessage(),e);
		}
	}
	/*
	private void saveDynamodb(Date utcDateTime,String token,String imageS3Path,String zoneId) throws DynamodbRuntimeException {
		LOGGER.info("saveDynamodb: zoneId:{},token:{},imageS3Path:{} utcDateTime:{}",zoneId,token,imageS3Path,utcDateTime);
		Item item = new Item()
				.withPrimaryKey("token", token, "created",
						DateFormatUtils.format(utcDateTime, "yyyy-MM-dd HH:mm:ss.SSS"))
				.withString("token", token).withString("imgUri", imageS3Path).withString("thumbUri", imageS3Path)
				.withJSON("zone_" + zoneId, "{\"events\":[\"motion\"]}").withJSON("zone_all", "{}");
		try {
			dynamodbTemplate.putItem(tableName, item);
		} catch (Exception e) {
			throw new DynamodbRuntimeException(e.getMessage(),e);
		}
	}
	*/
	private void putSqs(Date utcDateTime,String token,String zoneId,String imageS3Path) throws SqsRuntimeException{
		LOGGER.info("Token:{},putSqs: zoneId:{},imageS3Path:{} utcDate:{}",token,zoneId,imageS3Path,utcDateTime);
		AlgorithmResult result = new AlgorithmResult();
		result.setEventType(AlgorithmResult.SLS_EVENT_TYPE_MOTION);
		result.setDataList(Collections.<ObjectRecognitionInnerDto>emptyList());
		result.setStreamId(token);
		result.setBigImage(imageS3Path);
		result.setSmallImage(imageS3Path);
		result.setTimeStamp(DateFormatUtils.format(utcDateTime, "yyyy-MM-dd HH:mm:ss"));
		result.setZoneId(Long.valueOf(zoneId));
		try {
			sqsTemplate.publish(queue, JSON.toJSONString(result));
		} catch (Exception e) {
			throw new SqsRuntimeException(e.getMessage(),e);
		}
	}
}
