package com.sengled.mediaworker.algorithm;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.sengled.mediaworker.algorithm.service.dto.MotionFeedResult;
import com.sengled.mediaworker.object.ObjectRecognition;


/**
 * Feed结果监听器
 * @author media-liwei
 *
 */
@Component
public class ObjectFeedListenerImpl implements FeedListener,InitializingBean{
	private static final Logger LOGGER = LoggerFactory.getLogger(ObjectFeedListenerImpl.class);
	
	@Autowired
	private ObjectRecognition objectRecognition;
	@Override
	public void afterPropertiesSet() throws Exception {
		try {
			initialize();
		} catch (Exception e) {
			LOGGER.error(e.getMessage(),e);
			System.exit(1);
		}
	}
	private void initialize(){
		LOGGER.info("FeedListener init....");
	}
	@Override
	public void feedResultHandle(StreamingContext context, MotionFeedResult motionFeedResult) {

		String token = context.getToken();
		byte[] nal = context.getNalData();
		Map<String,Object> objectConfig = new HashMap<>();
		objectConfig.putAll((Map<String,Object>)context.getConfig().get("object"));
		
		LOGGER.info("Token:{},submit ObjectRecognition:{}", token);
		objectRecognition.sumbit(token,nal,objectConfig,motionFeedResult);
	}
}
