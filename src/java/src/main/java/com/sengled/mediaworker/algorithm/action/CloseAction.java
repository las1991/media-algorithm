package com.sengled.mediaworker.algorithm.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sengled.mediaworker.algorithm.FeedListener;
import com.sengled.mediaworker.algorithm.StreamingContext;
import com.sengled.mediaworker.algorithm.pydto.YUVImage;

public class CloseAction extends Action{
	private static final Logger LOGGER = LoggerFactory.getLogger(CloseAction.class);

	@Override
	public void feed(final StreamingContext context,final YUVImage yuvImage,final FeedListener listener) {
		LOGGER.debug("CloseAction feed...");
		context.setAction(context.execAction);
		context.feed(yuvImage, listener);
		context.close();	
	}
}
