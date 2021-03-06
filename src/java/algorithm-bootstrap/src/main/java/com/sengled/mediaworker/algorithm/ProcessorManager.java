package com.sengled.mediaworker.algorithm;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

import com.sengled.media.interfaces.Algorithm;
import com.sengled.media.interfaces.YUVImage;
import com.sengled.media.interfaces.exceptions.AlgorithmIntanceCloseException;
import com.sengled.media.interfaces.exceptions.AlgorithmIntanceCreateException;
import com.sengled.media.interfaces.exceptions.DecodeException;
import com.sengled.media.interfaces.exceptions.EncodeException;
import com.sengled.media.interfaces.exceptions.FeedException;
import com.sengled.mediaworker.algorithm.decode.KinesisFrameDecoder.Frame;
 

public interface ProcessorManager {	
	Future<?> submit(long receiveTime,String token, Collection<Frame> data);
	String newAlgorithmModel(String token) throws AlgorithmIntanceCreateException;
	void   close(String algorithmModelId)throws AlgorithmIntanceCloseException ;
	<T> T feed(Algorithm algorithm, YUVImage yuvImage,Class<T> responseType) throws FeedException;
	List<YUVImage> decode(final String token,final byte[] nalData) throws DecodeException;
	byte[] encode(String token,byte[] yuvData,int width,int  height,int  dstWidth,int  dstHeight)throws EncodeException;
	void shutdown();
}
