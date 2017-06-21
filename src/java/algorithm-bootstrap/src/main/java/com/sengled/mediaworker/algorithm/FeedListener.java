package com.sengled.mediaworker.algorithm;

import com.sengled.mediaworker.algorithm.service.dto.MotionFeedResult;

public interface FeedListener {
	void feedResultHandle(StreamingContext context,MotionFeedResult motionFeedResult);
}