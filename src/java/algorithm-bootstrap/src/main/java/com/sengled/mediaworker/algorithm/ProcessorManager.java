package com.sengled.mediaworker.algorithm;

import java.util.Collection;
import java.util.concurrent.Future;

import com.sengled.media.interfaces.Algorithm;
import com.sengled.media.interfaces.YUVImage;
import com.sengled.media.interfaces.exceptions.AlgorithmIntanceCloseException;
import com.sengled.media.interfaces.exceptions.AlgorithmIntanceCreateException;
import com.sengled.media.interfaces.exceptions.EncodeException;
import com.sengled.media.interfaces.exceptions.FeedException;
 

public interface ProcessorManager {	
	Future<?> submit(String token, Collection<byte[]> data);
	void   setFeedListener(FeedListenerImpl feedListener);
	String newAlgorithmModel(String token,String model) throws AlgorithmIntanceCreateException;
	void   close(StreamingContext context)throws AlgorithmIntanceCloseException ;
	String feed(Algorithm algorithm, YUVImage yuvImage) throws FeedException;
	byte[] encode(String token,byte[] yuvData,int width,int  height,int  dstWidth,int  dstHeight)throws EncodeException;
	void shutdown();
}
