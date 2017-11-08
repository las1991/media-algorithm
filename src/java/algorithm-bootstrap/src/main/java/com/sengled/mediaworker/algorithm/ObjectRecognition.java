package com.sengled.mediaworker.algorithm;

import java.util.Date;

import com.sengled.media.interfaces.YUVImage;
import com.sengled.mediaworker.algorithm.decode.KinesisFrameDecoder.ObjectConfig;
import com.sengled.mediaworker.algorithm.service.dto.MotionFeedResult;


public interface ObjectRecognition {

	void  submit(final String token,final ObjectConfig config,final Date utcDate,final YUVImage yuvImage,final byte[] nalData,final int fileExpiresHours,final MotionFeedResult mfr);
}
