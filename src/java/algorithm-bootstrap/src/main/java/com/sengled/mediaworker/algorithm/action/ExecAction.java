package com.sengled.mediaworker.algorithm.action;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.sengled.media.interfaces.YUVImage;
import com.sengled.mediaworker.algorithm.FeedListener;
import com.sengled.mediaworker.algorithm.ProcessorManager;
import com.sengled.mediaworker.algorithm.StreamingContext;
import com.sengled.mediaworker.algorithm.service.dto.MotionFeedResult;

public class ExecAction extends Action {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ExecAction.class);

	@Override
	public void feed(StreamingContext context, FeedListener[] listeners) throws Exception {

		final String token = context.getToken();
		final String model = context.getModel();
		ProcessorManager processor = context.getProcessorManager();
		
		YUVImage yuvImage = processor.decode(token, context.getNalData());
		context.setYuvImage(yuvImage);
		LOGGER.debug("Token:{},Feed model:{},parameters:{},yuvImage size:{}", token, model, context.getAlgorithm().getParameters(),yuvImage.getYUVData().length);
		
		long startTime = System.currentTimeMillis();
		String text = processor.feed(context.getAlgorithm(), yuvImage);
		LOGGER.debug("Token:{},Feed cost:{} msec ",token,(System.currentTimeMillis() - startTime));
		
		if(StringUtils.isBlank(text.trim())){
			LOGGER.debug("Token:{},Feed result NORESULT. ",token);
			return;
		}
		MotionFeedResult motionFeedResult = JSONObject.parseObject(text.trim(), MotionFeedResult.class);
		if(motionFeedResult ==null || !motionFeedResult.verify()){
			LOGGER.info("Token:{},Feed result NORESULT. feed result:{} ",token,text);
			return;
		}
		for(FeedListener listener : listeners){
			listener.feedResultHandle(context,motionFeedResult);
		}
//		
//		if(context.isReport()){
//			try {
//				handleListenerEvent(text.trim(),context, yuvImage, listeners);
//			} catch (Exception e) {
//				throw new FeedException("feed failed.token:["+token+"]", e);
//			}
//		}else{
//			LOGGER.debug("Token:{} get Motion.But isReport is false.",token);
//		}
//		
//		LOGGER.debug("Token:{},Feed finished. model:{}", token, model);
//		Map<String,Object> objectConfig = (Map<String,Object>)context.getConfig().get("object");
		//交给物体检测程序运行。 
		//listener.submitObjectRecognition(token,nalData,objectConfig );	
	}

	/*
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
				//listener.post(event);
				context.setLastMotionTimestamp(context.getUtcDateTime().getTime());
				break;
//			case "object":
//				//FIXME
//				ObjectEvent objectEvent = new ObjectEvent();
//				objectEvent.setToken(token);
//				listener.post(objectEvent);
//				break;
			default:
				LOGGER.error("Token:{},model:{} nonsupport",token,model);
				return;
		}
	}
*/
}
