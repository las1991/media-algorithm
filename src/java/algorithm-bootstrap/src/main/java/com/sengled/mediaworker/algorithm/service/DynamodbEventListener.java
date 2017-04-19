package com.sengled.mediaworker.algorithm.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;

import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.google.common.eventbus.Subscribe;
import com.sengled.mediaworker.algorithm.event.MotionEvent;
import com.sengled.mediaworker.algorithm.event.ObjectEvent;
import com.sengled.mediaworker.algorithm.service.dto.AlgorithmResult;
import com.sengled.mediaworker.algorithm.service.dto.ObjectRecognitionInnerDto;
import com.sengled.mediaworker.dynamodb.DynamodbTemplate;
import com.sengled.mediaworker.s3.AmazonS3Template;
import com.sengled.mediaworker.sqs.SQSTemplate;

@Component
public class DynamodbEventListener {
	private static final Logger LOGGER = LoggerFactory.getLogger(DynamodbEventListener.class);

	public final static String tableName = "m_algorithm_results";

	@Value("${AWS_SERVICE_NAME_PREFIX}_${sqs.algorithm.dispatcher.result.queue}")
	private String queue;
	@Value("${aws_screenshot_bucket}")
	private String bucketName;

	@Autowired
	private DynamodbTemplate dynamodbTemplate;
	@Autowired
	private SQSTemplate sqsTemplate;
	@Autowired
	private AmazonS3Template amazonS3Template;

	/**
	 * 物体识别
	 * 
	 * @param event
	 */
	@Subscribe
	public void feedEvent(ObjectEvent event) {
		// FIXME
		LOGGER.info("ObjectEvent ...");
	}

	/**
	 * motion事件
	 * 
	 * @param event
	 */
	@Subscribe
	public void feedEvent(MotionEvent event) {
		LOGGER.info("Get MotionEvent:{}",event);
		Date utcDateTime = event.getUtcDate();
		byte[] jpgData = event.getJpgData();
		String token = event.getToken();
		String zoneId = event.getZoneId();
		String imageS3Path = token + "_motion_" + utcDateTime.getTime() + ".jpg";
		try {
			saveS3(imageS3Path, jpgData);
			saveDynamodb(utcDateTime, token, imageS3Path, zoneId);
			putSqs(utcDateTime, token, zoneId,imageS3Path);
		} catch (Exception e) {
			LOGGER.info("token:{},imageS3Path:{},utcDateTime:{},zoneId:{}",token,imageS3Path,utcDateTime,zoneId);
			LOGGER.error("feedEvent failed.",e);
		}
		
		LOGGER.info("feedEvent finished token:{}",event.getToken());
	}



	private void saveS3(String imageS3Path,byte[] jpgData) throws Exception{
		LOGGER.info("imageS3Path:" + imageS3Path);
		amazonS3Template.putObject(bucketName, imageS3Path, jpgData);
	}
	private void saveDynamodb(Date utcDateTime,String token,String imageS3Path,String zoneId)throws Exception {
		LOGGER.info("saveDynamodb: zoneId:{},token:{},imageS3Path:{} utcDateTime:{}",zoneId,token,imageS3Path,utcDateTime);
		Item item = new Item()
				.withPrimaryKey("token", token, "created",
						DateFormatUtils.format(utcDateTime, "yyyy-MM-dd HH:mm:ss.SSS"))
				.withString("token", token).withString("imgUri", imageS3Path).withString("thumbUri", imageS3Path)
				.withJSON("zone_" + zoneId, "{\"events\":[\"motion\"]}").withJSON("zone_all", "{}");
		dynamodbTemplate.putItem(tableName, item);
	}
	
	private void putSqs(Date utcDateTime,String token,String zoneId,String imageS3Path) throws Exception{
		LOGGER.info("putSqs: zoneId:{},token:{},imageS3Path:{} utcDate:{}",zoneId,token,imageS3Path,utcDateTime);
		AlgorithmResult result = new AlgorithmResult();
		result.setEventType(AlgorithmResult.SLS_EVENT_TYPE_MOTION);
		result.setDataList(Collections.<ObjectRecognitionInnerDto>emptyList());
		result.setStreamId(token);
		result.setBigImage(imageS3Path);
		result.setSmallImage(imageS3Path);
		result.setTimeStamp(DateFormatUtils.format(utcDateTime, "yyyy-MM-dd HH:mm:ss"));
		result.setZoneId(Long.valueOf(zoneId));
		sqsTemplate.publish(queue, JSON.toJSONString(result));
	}
}
