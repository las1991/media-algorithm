package com.sengled.mediaworker.object;

import java.util.Map;

import com.sengled.mediaworker.algorithm.service.dto.MotionFeedResult;

public interface ObjectRecognition {

	void sumbit(final String token,final byte[] nal,Map<String,Object> objectConfig,MotionFeedResult mfr);
}
