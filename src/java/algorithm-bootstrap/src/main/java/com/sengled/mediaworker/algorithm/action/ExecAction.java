package com.sengled.mediaworker.algorithm.action;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.sengled.media.interfaces.YUVImage;
import com.sengled.mediaworker.algorithm.ProcessorManager;
import com.sengled.mediaworker.algorithm.context.StreamingContext;
import com.sengled.mediaworker.algorithm.decode.KinesisFrameDecoder.Frame;
import com.sengled.mediaworker.algorithm.feedlistener.FeedListener;
import com.sengled.mediaworker.algorithm.service.dto.MotionFeedResult;

public class ExecAction extends Action {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ExecAction.class);

	@Override
	public void feed(StreamingContext context, final Frame frame,final FeedListener[] listeners) throws Exception {

		final String token = context.getToken();
		ProcessorManager processor = context.getProcessorManager();
		
		final YUVImage yuvImage = processor.decode(token, frame.getNalData());
		
		LOGGER.debug("Token:{},Feed ,parameters:{},yuvImage size:{}", token, context.getAlgorithm().getParametersJson(),yuvImage.getYUVData().length);

		long startTime = System.currentTimeMillis();
		String text = processor.feed(context.getAlgorithm(), yuvImage);
		LOGGER.debug("Token:{},Feed cost:{} msec  result:{}",token,(System.currentTimeMillis() - startTime),text.trim());
		
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
			try {
				listener.feedResultHandle(context,yuvImage,frame.getNalData(),motionFeedResult);
			} catch (Exception e) {
				LOGGER.error(e.getMessage(),e);
			}
		}
	}
}
