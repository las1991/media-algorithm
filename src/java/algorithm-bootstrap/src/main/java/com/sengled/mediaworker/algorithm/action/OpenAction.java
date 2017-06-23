package com.sengled.mediaworker.algorithm.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sengled.mediaworker.algorithm.context.StreamingContext;
import com.sengled.mediaworker.algorithm.context.StreamingContextManager;
import com.sengled.mediaworker.algorithm.feedlistener.FeedListener;

public class OpenAction extends Action{
	private static final Logger LOGGER = LoggerFactory.getLogger(OpenAction.class);

	@Override
	public void feed(StreamingContext context, FeedListener[] listeners)throws Exception {
		LOGGER.debug("Token:{},OpenAction feed.StreamingContext reload.",context.getToken());
		
		StreamingContextManager manager = context.getStreamingContextManager();
		Long contextCreateTimestamp = context.getContextCreateTimestamp();
		Long contextUpdateTimestamp = context.getContextUpdateTimestamp();
		if( ! contextCreateTimestamp.equals(contextUpdateTimestamp)){//非本次创建 的上下文，当接收到OPEN时，需要reload
			manager.reload(context);	
		}
		context.setAction(context.execAction);
		context.feed(listeners);
	}
}
