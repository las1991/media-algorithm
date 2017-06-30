package com.sengled.mediaworker.algorithm.feedlistener;

import com.sengled.mediaworker.algorithm.context.StreamingContext;
import com.sengled.mediaworker.algorithm.service.dto.MotionFeedResult;

public interface FeedListener {
	void feedResultHandle(StreamingContext context,MotionFeedResult motionFeedResult)throws Exception;
}