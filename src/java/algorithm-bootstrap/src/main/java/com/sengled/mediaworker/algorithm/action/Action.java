package com.sengled.mediaworker.algorithm.action;

import com.sengled.mediaworker.algorithm.context.StreamingContext;
import com.sengled.mediaworker.algorithm.feedlistener.FeedListener;

public abstract class Action {
	public static final String NULL_ALGORITHM_MODEL = "NULL_ALGORITHM_MODEL";
	public static final String NORESULT = "NORESULT";
	
	public abstract void  feed(StreamingContext context,final FeedListener[] listener)throws Exception;
}
