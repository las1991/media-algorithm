package com.sengled.mediaworker.algorithm.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sengled.mediaworker.algorithm.FeedListener;
import com.sengled.mediaworker.algorithm.StreamingContext;
import com.sengled.mediaworker.algorithm.StreamingContextManager;

public class CloseAction extends Action{
	private static final Logger LOGGER = LoggerFactory.getLogger(CloseAction.class);

	@Override
	public void feed(StreamingContext context, final byte[] nalData, FeedListener listener)throws Exception {
		LOGGER.debug("Token:{} CloseAction feed",context.getToken());
		
		StreamingContextManager manager = context.getStreamingContextManager();
		manager.close(context);
	}
}
