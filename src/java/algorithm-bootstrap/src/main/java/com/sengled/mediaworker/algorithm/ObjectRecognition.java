package com.sengled.mediaworker.algorithm;

import java.util.Date;
import java.util.Map;
import com.sengled.media.interfaces.YUVImage;
import com.sengled.mediaworker.algorithm.context.AlgorithmConfigWarpper.ObjectConfig;
import com.sengled.mediaworker.algorithm.service.dto.MotionFeedResult;


public interface ObjectRecognition {
    /**
     * 
     * @param token
     * @param objectConfig  物体识别配置
     * @param copyUtcDate   
     * @param copyYUVmageMap
     * @param nalData
     * @param fileExpiresHours
     * @param motionFeedResultMap
     */
    void submit(final String token, 
                final ObjectConfig finalObjectConfig, 
                final Date finalUtcDate, 
                final Map<Integer, YUVImage> finalYUVmageMap, 
                final byte[] nalData,
                final int finalFileExpiresHours, 
                final Map<Integer, MotionFeedResult> finalMotionFeedResultMap);
}
