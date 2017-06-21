package com.sengled.media.interfaces;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sengled.media.interfaces.exceptions.AlgorithmIntanceCloseException;
import com.sengled.media.interfaces.exceptions.AlgorithmIntanceCreateException;
import com.sengled.media.interfaces.exceptions.DecodeException;
import com.sengled.media.interfaces.exceptions.EncodeException;
import com.sengled.media.interfaces.exceptions.FeedException;
import com.sengled.media.jna.jpg_encoder.JPGFrame;
import com.sengled.media.jna.jpg_encoder.Jpg_encoderLibrary;
import com.sengled.media.jna.nal_decoder.Nal_decoderLibrary;
import com.sengled.media.jna.nal_decoder.YUVFrame;
import com.sengled.media.jna.sengled_algorithm_base.Sengled_algorithm_baseLibrary;
import com.sengled.media.jna.sengled_algorithm_base.algorithm_base_result;
import com.sengled.media.jni.JNIFunction;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

public class JnaInterface implements Function{
	private static final Logger LOGGER = LoggerFactory.getLogger(JnaInterface.class);
	
	private static Nal_decoderLibrary decoderLibrary;
	private static Sengled_algorithm_baseLibrary algorithmLibrary;
	private static Jpg_encoderLibrary encoderLibrary;
	
	private ConcurrentHashMap<String, Pointer> pointerMap = new ConcurrentHashMap<>();
	
	static{
		try {
			/*
			LOGGER.info("init...");
			String jnaHome = System.getProperty("jna.library.path");
			LOGGER.info("jna.library.path={}", jnaHome);
			
			decoderLibrary = Nal_decoderLibrary.INSTANCE;
			decoderLibrary.Init();
			decoderLibrary.SetLogCallback(new Pointer(JNIFunction.getInstance().getLog4CFunction()));
			
			algorithmLibrary = Sengled_algorithm_baseLibrary.INSTANCE;
			algorithmLibrary.SetLogCallback(new Pointer(JNIFunction.getInstance().getLog4CFunction()));
			
			encoderLibrary = Jpg_encoderLibrary.INSTANCE;
			encoderLibrary.Init();
			encoderLibrary.SetLogCallback(new Pointer(JNIFunction.getInstance().getLog4CFunction()));
			*/
			LOGGER.info("init finished");
		} catch (Exception e) {
			LOGGER.error(e.getMessage(),e);
			LOGGER.error("JnaInterface init failed. System exit.");
			System.exit(1);
		}
	}
	@Override
	public YUVImage decode(String token, byte[] nalData) throws DecodeException {
		if(null == nalData || nalData.length == 0){
			LOGGER.error("Token:{} decode params error.",token);
			throw new IllegalArgumentException("decode params error.");
		}
		LOGGER.debug("decode token:{},nalData length:{}",token,nalData.length);

		YUVFrame yuv_frame = new YUVFrame();
		try {
			ByteBuffer data_buffer  = ByteBuffer.wrap(nalData);
			int len = nalData.length;
			long startTime = System.currentTimeMillis();
			
			int code = decoderLibrary.DecodeNal(data_buffer, len, token, yuv_frame);
			
			LOGGER.debug("decode code:{} Cost:{}",code,(System.currentTimeMillis() - startTime ));
			if( 0 != code){
				LOGGER.error("decode failed. code:{} token:{}",code,token);
				throw new Exception("return code error.");
			}
			byte[] yuvData = yuv_frame.data.getByteArray(0, yuv_frame.size);
			if(null == yuvData ||  0 == yuvData.length ){
				LOGGER.error("decode failed. yuvData empty. code:{} token:{}",code,token);
				throw new Exception("yuvData empty.");
			}
			LOGGER.debug("Token:{},decode finished. width:{},height:{}",token,yuv_frame.width,yuv_frame.height);
			return new  YUVImage(yuv_frame.width,yuv_frame.height,yuvData);
		}catch(Exception e){
			throw new DecodeException("DecodeException "+e.getMessage());
		}finally{
			decoderLibrary.Destroy(yuv_frame);
			LOGGER.debug("Destroy yuv_frame");
		}
	}

	@Override
	public byte[] encode(String token, int width, int height, int dstWidth, int dstHeight, byte[] yuvData) throws EncodeException{
		int yuvDataLength = yuvData.length;
		if( null == yuvData || 0 == yuvDataLength){
			LOGGER.error("Token:{} encode params error.",token);
			throw new EncodeException("encode params error.");
		}
		
		LOGGER.debug("Token:{} encode ,yuvData length:{}",token,yuvData.length);
		JPGFrame jpg_frame =  new JPGFrame();
		DisposeableMemory pointer = new DisposeableMemory(yuvDataLength);
		pointer.write(0, yuvData, 0, yuvDataLength);
		com.sengled.media.jna.jpg_encoder.YUVFrame yuv_frame =  new com.sengled.media.jna.jpg_encoder.YUVFrame(width,height,pointer,yuvDataLength);
		
		try {
			int  code =  encoderLibrary.EncodeJPG(yuv_frame, dstWidth, dstHeight, token, jpg_frame);
			if( 0 != code ){
				LOGGER.error("Token:{} encode failed. code:{}",token,code);
				throw new Exception("return code error.");
			}
			return jpg_frame.data.getByteArray(0, jpg_frame.size);
		} catch (Exception e) {
			throw new EncodeException("DecodeException "+e.getMessage());
		}finally{
			encoderLibrary.Destroy(jpg_frame);
			pointer.dispose();
		}
	}
	@Override
	public String  newAlgorithmModel( String token,String model) throws AlgorithmIntanceCreateException{
		LOGGER.debug("Token:{},model:{} newAlgorithmModel",token,model);
		String algorithmModelId;
		Pointer oldPointer = null;
		try {
			Pointer pointer =   algorithmLibrary.create_instance(token+"_"+model);
			algorithmModelId = UUID.randomUUID().toString();
			oldPointer = pointerMap.put(algorithmModelId, pointer);
		} catch (Exception e) {
			throw new AlgorithmIntanceCreateException(e);
		}finally{
			try {
				if(null != oldPointer){
					algorithmLibrary.delete_instance(oldPointer);
				}
			} catch (Exception e) {
				LOGGER.warn("delete oldPointer error.",e);
			}
		}
		
		

		return algorithmModelId;
	}

	@Override
	public String feed(String jsonConfig,String algorithmModelId, YUVImage yuvImage) throws FeedException{
		LOGGER.debug("feed AlgorithmModelId:{},jsonConfig:{} ",algorithmModelId,jsonConfig);
		if( null == jsonConfig  || null == yuvImage || null == algorithmModelId ){
			LOGGER.error("jsonConfig:{},cObjectID:{},yuvImage:{}",jsonConfig,algorithmModelId,yuvImage);
			throw new IllegalArgumentException("feed params exception.");
		}
		
		Pointer algorithmModelPointer = pointerMap.get(algorithmModelId);
		if( null ==  algorithmModelPointer){
			LOGGER.info("jsonConfig:{},algorithmModelId:{}",jsonConfig,algorithmModelId);
			throw new FeedException("Not fonud algorithmModelPointer from pointerMap");
		}
		
		algorithm_base_result result = new algorithm_base_result();
		
		int length;
		DisposeableMemory algorithm_params;
		try {
			length = jsonConfig.getBytes("utf-8").length;
			algorithm_params = new DisposeableMemory(length);
			algorithm_params.write(0, jsonConfig.getBytes("utf-8"), 0, length);
		} catch (UnsupportedEncodingException e1) {
			throw new  FeedException(e1);
		}

		byte[] yuvData = yuvImage.getYUVData();
		int yuvDataLength = yuvData.length;
		if(0 == yuvDataLength){
			throw new FeedException("yuvDataLength is empay");
		}
		
		DisposeableMemory yuvDataPointer = new DisposeableMemory(yuvDataLength);
		try {
			yuvDataPointer.write(0, yuvData, 0, yuvDataLength);
			LOGGER.debug("yuvDataPointer data length:{}",yuvDataPointer.getByteArray(0, yuvDataLength).length);
			algorithmLibrary.feed(algorithmModelPointer, yuvDataPointer, yuvImage.getWidth(), yuvImage.getHeight(), algorithm_params, result);
			if(result.bresult != 0 ){
				return  new String(result.result,0,(10 * 1024),"utf-8");
			}
			return "";
		} catch (Exception e) {
			throw new FeedException(e);
		}finally{
			yuvDataPointer.dispose();
			algorithm_params.dispose();
		}
	}

	@Override
	public void close(String algorithmModelId) throws AlgorithmIntanceCloseException{
		LOGGER.debug("close algorithmModelId:{}",algorithmModelId);
		
		LOGGER.debug("pointerMap size:{}",pointerMap.size());
		
		if(null == algorithmModelId || "".equals(algorithmModelId)){
			throw new AlgorithmIntanceCloseException("parmas error.");
		}
		
		try {
			Pointer pointer = pointerMap.remove(algorithmModelId);
			if( null != pointer){
				algorithmLibrary.delete_instance(pointer);
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage(),e);
			throw new AlgorithmIntanceCloseException(e);
		}
	}
}

