package com.sengled.mediaworker.algorithm;

import java.util.concurrent.Future;

import com.sengled.mediaworker.algorithm.context.ObjectContext;
import com.sengled.mediaworker.algorithm.service.dto.MotionFeedResult;


public interface ObjectRecognition {

	Future<?>  submit(final ObjectContext token,final MotionFeedResult mfr);
}
