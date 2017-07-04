package com.sengled.mediaworker.algorithm;

import java.util.Date;
import java.util.concurrent.Future;

import com.sengled.media.interfaces.YUVImage;
import com.sengled.mediaworker.algorithm.context.ObjectContext;
import com.sengled.mediaworker.algorithm.decode.KinesisFrameDecoder.ObjectConfig;
import com.sengled.mediaworker.algorithm.service.dto.MotionFeedResult;


public interface ObjectRecognition {

	Future<?>  submit(final String token,final ObjectConfig config,final Date utcDate,final YUVImage yuvImage,final byte[] nalData,final MotionFeedResult mfr);
}
