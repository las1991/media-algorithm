package com.sengled.mediaworker.algorithm.action;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
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

		final String token = context.getTokenMask();
		ProcessorManager processor = context.getProcessorManager();
		
		final List<YUVImage> yuvImageList = processor.decode(token, frame.getNalData());
		if( CollectionUtils.isEmpty(yuvImageList)){
		    LOGGER.warn("[{}] decode result is empty.",token);
		    return;
		}
		LOGGER.debug("[{}] decode result size:{}",token,yuvImageList.size());
		
		final Map<Integer,MotionFeedResult>  motionFeedResultMap = new HashMap<>();//<frameIndex,feedResult>
		final Map<Integer,YUVImage>  yuvImageResultMap = new HashMap<>();//<freameIndex,YumImage>
		int frameIndex = 0;
		for (YUVImage yuvImage : yuvImageList) {
		    LOGGER.debug("Token:{},Feed ,parameters:{},yuvImage size:{}", token, context.getAlgorithm().getParametersJson(),yuvImage.getYUVData().length);
		    long startTime = System.currentTimeMillis();
		    MotionFeedResult motionFeedResult = processor.feed(context.getAlgorithm(), yuvImage,MotionFeedResult.class);
	        LOGGER.debug("Token:{},Feed cost:{} msec  result:{}",token,(System.currentTimeMillis() - startTime),motionFeedResult);
	        
	        if(motionFeedResult ==null || !motionFeedResult.verify()){
                LOGGER.info("Token:{},Feed result NORESULT. feed result:{} ",token, motionFeedResult);
                if( null != context.getAlgorithm() ){
                    LOGGER.info("Token:{}, Algorithm:{} ",token, context.getAlgorithm().getParametersJson());    
                }
                continue;
            }
	        motionFeedResultMap.put(frameIndex,motionFeedResult);
            yuvImageResultMap.put(frameIndex, yuvImage);
            frameIndex++;
        }
		
		if( ! CollectionUtils.isEmpty(motionFeedResultMap) ){
	        for(FeedListener listener : listeners){
	            try {
	                listener.feedResultHandle(context,frame.getNalData(),yuvImageResultMap,motionFeedResultMap);
	            } catch (Exception e) {
	                LOGGER.error(e.getMessage(),e);
	            }
	        }
		}
	}
}
