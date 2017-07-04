package com.sengled.mediaworker.algorithm.feedlistener;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sengled.media.interfaces.YUVImage;
import com.sengled.mediaworker.RecordCounter;
import com.sengled.mediaworker.algorithm.ObjectRecognition;
import com.sengled.mediaworker.algorithm.context.ObjectContext;
import com.sengled.mediaworker.algorithm.context.ObjectContextManager;
import com.sengled.mediaworker.algorithm.context.StreamingContext;
import com.sengled.mediaworker.algorithm.decode.KinesisFrameDecoder.ObjectConfig;
import com.sengled.mediaworker.algorithm.service.dto.MotionFeedResult;

/**
 *
 * @author media-liwei
 *
 */
@Component
public class ObjectFeedListenerImpl implements FeedListener {
	private static final Logger LOGGER = LoggerFactory.getLogger(ObjectFeedListenerImpl.class);


	@Autowired
	ObjectRecognition objectRecognitionImpl;

	@Autowired
	RecordCounter recordCounter;
	
    @Value("${max.delayed.time.msce}")
    private long maxDelayedTimeMsce;
    

    
	@Override
	public void feedResultHandle(StreamingContext context, MotionFeedResult motionFeedResult) throws Exception{
		LOGGER.debug("Begin feedResultHandle. StreamingContext:{},motionFeedResult:{}",context,motionFeedResult);
		ObjectConfig objectConfig = context.getConfig().getObjectConfig();
		String token = context.getToken();
		Date utcDate = context.getUtcDateTime();

		if (null == objectConfig) {
			LOGGER.info("Token:{},objectConfig is null config:{}", token, context.getConfig());
			return;
		}

		long delayTime = System.currentTimeMillis() - utcDate.getTime();
		recordCounter.updateObjectReceiveDelay(delayTime);
		recordCounter.addAndGetObjectMotionCount(1);
		if(delayTime > maxDelayedTimeMsce){
			recordCounter.addAndGetObjectDataDelayedCount(1);
			LOGGER.info("Token:{} UTC Delay :{}  > maxDelayedTimeMsce:{}  skip.",token,delayTime,maxDelayedTimeMsce);
			return;
		}
		
		YUVImage yuvImage = context.getYuvImage();
		if( null == yuvImage ){
			return;
		}
		
		byte[] copyNalData = context.getNalData();
		YUVImage copyYuvImage = new YUVImage(yuvImage.getWidth(), yuvImage.getHeight(), yuvImage.getYUVData());		
		Date copyUtcDate = new Date(context.getUtcDateTime().getTime());
		objectRecognitionImpl.submit(token,objectConfig,copyUtcDate,copyYuvImage,copyNalData,motionFeedResult);		
	}
}
