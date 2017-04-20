package com.sengled.mediaworker.algorithm.action;

import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sengled.media.interfaces.YUVImage;
import com.sengled.mediaworker.algorithm.FeedListener;
import com.sengled.mediaworker.algorithm.ProcessorManager;
import com.sengled.mediaworker.algorithm.StreamingContext;
import com.sengled.mediaworker.algorithm.event.MotionEvent;
import com.sengled.mediaworker.algorithm.event.ObjectEvent;
import com.sengled.mediaworker.algorithm.exception.FeedException;

public class ExecAction extends Action {
	private static final int  MOTION_INTERVAL = 15;
	private static final Logger LOGGER = LoggerFactory.getLogger(ExecAction.class);

	@Override
	public void feed(StreamingContext context, YUVImage yuvImage, FeedListener listener) throws Exception {

		final String token = context.getToken();
		final String model = context.getModel();
		
		LOGGER.debug("Token:{},ExecAction feed",context.getToken());
		
		ProcessorManager processor = context.getProcessorManager();
		Date lastMotionDate = context.getLastMotionDate();
		if(lastMotionDate !=null){
			if((context.getLastUtcDateTime().getTime()/1000 - lastMotionDate.getTime()/1000 ) <= MOTION_INTERVAL){
				LOGGER.info("Token:{},Motion MOTION_INTERVAL:{} sec",token,MOTION_INTERVAL);
				return;
			}else{
				context.setLastMotionDate(null);
			}
		}
		
		LOGGER.debug("Token:{},model:{},parameters:{},yuvImage size:{}", token, model, context.getAlgorithm().getParameters(),yuvImage.getYUVData().length);
		
		String text = processor.feed(context.getAlgorithm(), yuvImage);
		if(StringUtils.isBlank(text)){
			LOGGER.debug("Token:{},Feed result NORESULT. ",token);
			return;
		}
//		LOGGER.debug("token:{},model:{},feed return:{}", token, model, text);
//		if (Action.NULL_ALGORITHM_MODEL.equals(text)) {// feed 返回ERROR 时，重新初始化算法模型，丢弃本次接收的数据不再调用feed
//			LOGGER.error("Feed result "+Action.NULL_ALGORITHM_MODEL+". run reloadAlgorithmModel");
//			throw new StreamingContextNotFoundException("NULL ALGORITHM MODEL");
//		}
//		if (Action.NORESULT.equals(text)) {
//			LOGGER.debug("Feed result NORESULT. token:{}",token);
//			return;
//		}
		try {
			handleListenerEvent(text,context, yuvImage, listener);
		} catch (Exception e) {
			throw new FeedException("feed failed.token:["+token+"]", e);
		}
		LOGGER.debug("Token:{},model:{},OpenAction feed finisthed...", token, model);
		
	}
	
	private void handleListenerEvent(String text,final StreamingContext context, final YUVImage yuvImage,final FeedListener listener) throws Exception{
		LOGGER.debug("Token:{},Feed result:{}",context.getToken(),text);
		
		ProcessorManager processor = context.getProcessorManager();
		byte[] jpgData = processor.encode(context.getToken(), yuvImage.getYUVData(), yuvImage.getWidth(), yuvImage.getHeight(), yuvImage.getWidth(), yuvImage.getHeight());
		JSONObject jsonObj = JSON.parseObject(text.trim());
		String zoneId = jsonObj.getString("zone_id");
		String model = context.getModel();
		String token =  context.getToken();
		
		switch(model){
			case "motion":
				MotionEvent event = new MotionEvent(token,model,context.getLastUtcDateTime(),jpgData,zoneId);
				listener.post(event);
				context.setLastMotionDate(context.getLastUtcDateTime());
				break;
			case "object":
				//FIXME
				ObjectEvent objectEvent = new ObjectEvent();
				objectEvent.setToken(token);
				listener.post(objectEvent);
				break;
			default:
				LOGGER.error("Token:{},model:{} nonsupport",token,model);
				return;
		}
	}

}
