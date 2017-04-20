package com.sengled.mediaworker.algorithm.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sengled.media.interfaces.YUVImage;
import com.sengled.mediaworker.algorithm.FeedListener;
import com.sengled.mediaworker.algorithm.StreamingContext;
import com.sengled.mediaworker.algorithm.StreamingContextManager;

public class OpenAction extends Action{
	private static final Logger LOGGER = LoggerFactory.getLogger(OpenAction.class);

	@Override
	public void feed(StreamingContext context, YUVImage yuvImage, FeedListener listener)throws Exception {
		LOGGER.debug("Token:{},OpenAction feed ",context.getToken());
		StreamingContextManager manager = context.getStreamingContextManager();
		context = manager.reload(context);
		context.setAction(context.execAction);
		context.feed(yuvImage, listener);
	}
}
