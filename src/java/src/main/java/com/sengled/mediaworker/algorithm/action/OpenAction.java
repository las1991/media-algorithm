package com.sengled.mediaworker.algorithm.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sengled.mediaworker.algorithm.FeedListener;
import com.sengled.mediaworker.algorithm.StreamingContext;
import com.sengled.mediaworker.algorithm.pydto.YUVImage;

public class OpenAction extends Action{
	private static final Logger LOGGER = LoggerFactory.getLogger(OpenAction.class);
	
	@Override
	public void feed(final StreamingContext context,final YUVImage yuvImage, final FeedListener listener)throws Exception {
<<<<<<< HEAD
=======
		context.reloadAlgorithmModel("Open action");
>>>>>>> b8922f60a2b57dfefa857cb933c099136ecc2152
		context.setAction(context.execAction);
		context.feed(yuvImage, listener);
	}
}
