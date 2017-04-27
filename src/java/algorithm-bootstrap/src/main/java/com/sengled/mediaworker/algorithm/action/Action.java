package com.sengled.mediaworker.algorithm.action;

import com.sengled.media.interfaces.YUVImage;
import com.sengled.mediaworker.algorithm.FeedListener;
import com.sengled.mediaworker.algorithm.StreamingContext;

public abstract class Action {
	public static final String NULL_ALGORITHM_MODEL = "NULL_ALGORITHM_MODEL";
	public static final String NORESULT = "NORESULT";
	
	public abstract void  feed(StreamingContext context,final YUVImage yuvImage,final FeedListener listener)throws Exception;
}
