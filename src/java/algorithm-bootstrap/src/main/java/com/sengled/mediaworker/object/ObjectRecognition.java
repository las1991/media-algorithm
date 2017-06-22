package com.sengled.mediaworker.object;

import com.sengled.media.interfaces.YUVImage;
import com.sengled.mediaworker.algorithm.decode.KinesisFrameDecoder.ObjectConfig;
import com.sengled.mediaworker.algorithm.service.dto.MotionFeedResult;


public interface ObjectRecognition {

	String match(final String token,final byte[] nal,final YUVImage yumImage,ObjectConfig objectConfig,MotionFeedResult mfr);
}
