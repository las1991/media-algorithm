package com.sengled.mediaworker.algorithm;

import com.sengled.mediaworker.algorithm.event.MotionEvent;
import com.sengled.mediaworker.algorithm.event.ObjectEvent;

public interface FeedListener {
	void post(MotionEvent event);
	void post(ObjectEvent event);
}