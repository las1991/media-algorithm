package com.sengled.mediaworker.algorithm.feedlistener;

import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.google.common.collect.Maps;
import com.sengled.media.interfaces.YUVImage;
import com.sengled.mediaworker.RecordCounter;
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
            LOGGER.warn("Token:{} UTC Delay :{}  > maxDelayedTimeMsce:{}  skip.",token,delayTime,maxDelayedTimeMsce);
            return;
        }
        
//        Map<Integer, YUVImage> copyYUVmageMap =  Maps.newHashMap();
//        
//        for( Entry<Integer, YUVImage> entry : yuvImageResultMap.entrySet()) {
//            YUVImage yuvImage = entry.getValue();
//            copyYUVmageMap.put(entry.getKey(), new YUVImage(yuvImage.getWidth(), yuvImage.getHeight(), yuvImage.getYUVData()));
//        }
 
        final Date finalUtcDate = new Date(context.getUtcDateTime().getTime());
        final int fileExpiresHours = context.getFileExpiresHours();
        
        
        objectRecognitionImpl.submit(token, objectConfig, finalUtcDate, yuvImageResultMap, nalData, fileExpiresHours, motionFeedResultMap); 
    }
}
