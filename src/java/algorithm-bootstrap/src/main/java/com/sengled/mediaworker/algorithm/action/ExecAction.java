package com.sengled.mediaworker.algorithm.action;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sengled.media.interfaces.YUVImage;
import com.sengled.mediaworker.algorithm.FeedListener;
import com.sengled.mediaworker.algorithm.ProcessorManager;
import com.sengled.mediaworker.algorithm.StreamingContext;
import com.sengled.mediaworker.algorithm.StreamingContextManager;
import com.sengled.mediaworker.algorithm.event.MotionEvent;
import com.sengled.mediaworker.algorithm.event.ObjectEvent;
import com.sengled.mediaworker.algorithm.exception.FeedException;

public class ExecAction extends Action {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ExecAction.class);

	@Override
	public void feed(StreamingContext context, final byte[] nalData, FeedListener listener) throws Exception {

		final String token = context.getToken();
		final String model = context.getModel();
		ProcessorManager processor = context.getProcessorManager();
		YUVImage yuvImage = processor.decode(token, nalData);
		LOGGER.debug("Token:{},Feed model:{},parameters:{},yuvImage size:{}", token, model, context.getAlgorithm().getParameters(),yuvImage.getYUVData().length);
		long startTime = System.currentTimeMillis();
		String text = processor.feed(context.getAlgorithm(), yuvImage);
		LOGGER.debug("Token:{},Feed cost:{} msec ",token,(System.currentTimeMillis() - startTime));
		if(StringUtils.isBlank(text)){
			LOGGER.debug("Token:{},Feed result NORESULT. ",token);
			return;
		}
		if(context.isReport()){
			try {
				handleListenerEvent(text.trim(),context, yuvImage, listener);
			} catch (Exception e) {
				throw new FeedException("feed failed.token:["+token+"]", e);
			}
		}else{
			LOGGER.debug("Token:{} get Motion.But isReport is false.",token);
		}
		
		LOGGER.debug("Token:{},Feed finished. model:{}", token, model);
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
				LOGGER.info("Token:{},Get {}. zoneId:{},",token,model,zoneId);
				MotionEvent event = new MotionEvent(token,model,context.getUtcDateTime(),jpgData,zoneId);
				listener.post(event);
				context.setLastMotionTimestamp(context.getUtcDateTime().getTime());
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
