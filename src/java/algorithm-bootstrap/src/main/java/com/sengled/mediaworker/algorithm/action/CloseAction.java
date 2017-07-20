package com.sengled.mediaworker.algorithm.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sengled.mediaworker.algorithm.context.StreamingContext;
import com.sengled.mediaworker.algorithm.context.StreamingContextManager;
import com.sengled.mediaworker.algorithm.decode.KinesisFrameDecoder.Frame;
import com.sengled.mediaworker.algorithm.feedlistener.FeedListener;

public class CloseAction extends Action{
	private static final Logger LOGGER = LoggerFactory.getLogger(CloseAction.class);

	@Override
	public void feed(StreamingContext context, final Frame frame,final FeedListener[] listeners) throws Exception {
		LOGGER.debug("Token:{} CloseAction feed",context.getToken());
		
		StreamingContextManager manager = context.getStreamingContextManager();
		manager.close(context);
	}
}
