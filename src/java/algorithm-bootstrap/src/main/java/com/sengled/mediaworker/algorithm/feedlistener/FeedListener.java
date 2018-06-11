package com.sengled.mediaworker.algorithm.feedlistener;

import java.util.Map;
import com.sengled.media.interfaces.YUVImage;
import com.sengled.mediaworker.algorithm.context.StreamingContext;
import com.sengled.mediaworker.algorithm.service.dto.MotionFeedResult;

public interface FeedListener {
    /**
     * 
     * @param context
     * @param nalData   nal数据包，可包含多张图
     * @param yuvImageResultMap nal decode yuv 对应的多张图 <frameIndex,YUVImage>
     * @param motionFeedResultMap   feed 后的motion 结果 <frameIndex,motionResult>
     * @throws Exception
     */
    void feedResultHandle(StreamingContext context, 
                          final byte[] nalData,
                          final Map<Integer, YUVImage> yuvImageResultMap, 
                          final Map<Integer, MotionFeedResult> motionFeedResultMap)  throws Exception;
}