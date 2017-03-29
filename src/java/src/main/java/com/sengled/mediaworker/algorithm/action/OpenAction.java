package com.sengled.mediaworker.algorithm.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sengled.mediaworker.algorithm.FeedListener;
import com.sengled.mediaworker.algorithm.StreamingContext;
import com.sengled.mediaworker.algorithm.pydto.YUVImage;

public class OpenAction extends Action{
	private static final Logger LOGGER = LoggerFactory.getLogger(OpenAction.class);
	
	@Override
	public void feed(final StreamingContext context,final YUVImage yuvImage, final FeedListener listener) {
		context.close();
		context.reloadAlgorithmModel("Open action");
		context.setAction(context.execAction);
		context.feed(yuvImage, listener);
	}
}
