package com.sengled.media.interfaces;

import java.util.List;
import com.sengled.media.interfaces.exceptions.AlgorithmIntanceCloseException;
import com.sengled.media.interfaces.exceptions.AlgorithmIntanceCreateException;
import com.sengled.media.interfaces.exceptions.DecodeException;
import com.sengled.media.interfaces.exceptions.EncodeException;
import com.sengled.media.interfaces.exceptions.FeedException;

public interface CFunction {

    List<YUVImage> decode(String token,byte[] nalData)throws DecodeException;
	
	byte[] encode(String token,int width,int height,int dstWidth,int dstHeight,byte[] yuvData) throws EncodeException;
	
	String newAlgorithmModel(String token)throws AlgorithmIntanceCreateException;
	
	String feed(String jsonConfig,String cObjectID, YUVImage yuvImage)throws FeedException;

	void close(String algorithmModelId)throws AlgorithmIntanceCloseException;
}
