package com.sengled.mediaworker.algorithm.feedlistener;

import com.sengled.media.interfaces.YUVImage;
import com.sengled.mediaworker.algorithm.context.StreamingContext;
import com.sengled.mediaworker.algorithm.service.dto.MotionFeedResult;

public interface FeedListener {
	void feedResultHandle(StreamingContext context,final YUVImage yuvImage, final byte[] nalData,MotionFeedResult motionFeedResult) throws Exception;
}