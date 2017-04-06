package com.sengled.mediaworker.algorithm.action;

import com.sengled.mediaworker.algorithm.FeedListener;
import com.sengled.mediaworker.algorithm.StreamingContext;
import com.sengled.mediaworker.algorithm.pydto.YUVImage;

public abstract class Action {
	public static final String NULL_ALGORITHM_MODEL = "NULL_ALGORITHM_MODEL";
	public static final String NORESULT = "NORESULT";
	public abstract void  feed(StreamingContext context,final YUVImage image,final FeedListener listener)throws Exception;
}
