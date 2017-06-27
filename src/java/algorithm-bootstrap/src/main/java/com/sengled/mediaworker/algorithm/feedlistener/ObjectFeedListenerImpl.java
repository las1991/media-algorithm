package com.sengled.mediaworker.algorithm.feedlistener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sengled.mediaworker.algorithm.ObjectRecognition;
import com.sengled.mediaworker.algorithm.context.ObjectContext;
import com.sengled.mediaworker.algorithm.context.ObjectContextManager;
import com.sengled.mediaworker.algorithm.context.StreamingContext;
import com.sengled.mediaworker.algorithm.decode.KinesisFrameDecoder.ObjectConfig;
import com.sengled.mediaworker.algorithm.service.dto.MotionFeedResult;

/**
 *
 * @author media-liwei
 *
 */
@Component
public class ObjectFeedListenerImpl implements FeedListener, InitializingBean {
	private static final Logger LOGGER = LoggerFactory.getLogger(ObjectFeedListenerImpl.class);

	@Autowired
	ObjectContextManager objectContextManager;

	@Autowired
	ObjectRecognition objectRecognitionImpl;

	@Override
	public void afterPropertiesSet() throws Exception {
		try {
			initialize();
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			System.exit(1);
		}
	}

	private void initialize() {

	}

	@Override
	public void feedResultHandle(StreamingContext context, MotionFeedResult motionFeedResult) {
		ObjectConfig objectConfig = context.getConfig().getObjectConfig();
		if (null == objectConfig) {
			LOGGER.error("Token:{},objectConfig is null config:{}", context.getToken(), context.getConfig());
			return;
		}

		ObjectContext objectContext = objectContextManager.findOrCreateStreamingContext(context);
		if (objectContext.isSkip()) {
			return;
		}

		objectRecognitionImpl.submit(objectContext, motionFeedResult);
	}
}
