package com.sengled.mediaworker.algorithm.feedlistener;

import java.util.Date;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.sengled.media.interfaces.YUVImage;
import com.sengled.mediaworker.RecordCounter;
import com.sengled.mediaworker.algorithm.MotionAndObjectReportManager;
import com.sengled.mediaworker.algorithm.ObjectRecognition;
import com.sengled.mediaworker.algorithm.context.AlgorithmConfigWarpper.ObjectConfig;
import com.sengled.mediaworker.algorithm.context.StreamingContext;
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
    public void feedResultHandle(StreamingContext context, 
                                 final byte[] nalData, 
                                 final Map<Integer, YUVImage> yuvImageResultMap,
                                 final Map<Integer, MotionFeedResult> motionFeedResultMap) {
        final ObjectConfig objectConfig = context.getConfig().getObjectConfig();
        String tokenMask = context.getTokenMask();
        String token = tokenMask.split(",")[0];
        Date utcDate = context.getUtcDateTime();

        if (null == objectConfig) {
            LOGGER.info("[{}] skip. objectConfig is null config:{}", tokenMask, context.getConfig());
            return;
        }
        if( ! MotionAndObjectReportManager.isAllowObjectReport(token) ){
            LOGGER.info("[{}] skip. object lasttime report is :{}",token, MotionAndObjectReportManager.getObjectRportTime(token));
        }

        long delayTime = System.currentTimeMillis() - utcDate.getTime();
        recordCounter.updateObjectReceiveDelay(delayTime);
        recordCounter.addAndGetObjectMotionCount(1);
        if(delayTime > maxDelayedTimeMsce){
            recordCounter.addAndGetObjectDataDelayedCount(1);
            LOGGER.warn("Token:{} UTC Delay :{}  > maxDelayedTimeMsce:{}  skip.",tokenMask,delayTime,maxDelayedTimeMsce);
            return;
        }
 
        final Date finalUtcDate = new Date(context.getUtcDateTime().getTime());
        final int fileExpiresHours = context.getFileExpiresHours();
        
        objectRecognitionImpl.submit(tokenMask, objectConfig, finalUtcDate, yuvImageResultMap, nalData, fileExpiresHours, motionFeedResultMap); 
    }
}
