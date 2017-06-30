package com.sengled.mediaworker.algorithm.feedlistener;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
	ObjectContextManager objectContextManager;

	@Autowired
	ObjectRecognition objectRecognitionImpl;

	@Autowired
	RecordCounter recordCounter;
	
    @Value("${max.delayed.time.msce}")
    private long maxDelayedTimeMsce;
    
    @Value("${object.interval.time.msce}")
    private Long objectIntervalTimeMsce;
    
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
	
		ObjectContext objectContext = objectContextManager.findOrCreateStreamingContext(context);
		if (objectContext.isSkip(objectIntervalTimeMsce)) {
			return;
		}
		
		objectRecognitionImpl.submit(objectContext, motionFeedResult);		
	}
}
