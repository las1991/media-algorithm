package com.sengled.mediaworker.algorithm.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sengled.media.interfaces.YUVImage;
import com.sengled.mediaworker.algorithm.FeedListener;
import com.sengled.mediaworker.algorithm.ProcessorManager;
import com.sengled.mediaworker.algorithm.StreamingContext;
import com.sengled.mediaworker.algorithm.StreamingContextManager;

public class CloseAction extends Action{
	private static final Logger LOGGER = LoggerFactory.getLogger(CloseAction.class);

	@Override
	public void feed(StreamingContext context, YUVImage yuvImage, ProcessorManager processor, FeedListener listener)throws Exception {
		context.setAction(context.execAction);
		context.feed(yuvImage, listener);
		StreamingContextManager manager = context.getStreamingContextManager();
		manager.close(context);
	}
}
