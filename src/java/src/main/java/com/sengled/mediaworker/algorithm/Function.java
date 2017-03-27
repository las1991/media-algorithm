package com.sengled.mediaworker.algorithm;

import com.sengled.mediaworker.algorithm.pydto.Algorithm;
import com.sengled.mediaworker.algorithm.pydto.YUVImage;

public interface Function {

	/**
	 * 解码
	 * @param src
	 * @return
	 */
	YUVImage decode(String token,byte[] src);
	
	/**
	 * 编码
	 * @param token
	 * @param width
	 * @param height
	 * @param yuvData
	 * @return jpg byte[]
	 */
	byte[] encode(String token,int width,int height,int dstWidth,int dstHeight,byte[] yuvData);
	
	/**
	 *	新建一个算法模型用于新的token
	 * @return 返回结果用于存储与token的对应关系
	 */
	String newAlgorithmModel(String model,String token);
	
	/**
	 * 向算法传入一张 yuv 图片
	 * @param <T>
	 * @param algorithm
	 * @param yuvImage
	 */
	String feed(Algorithm algorithm,YUVImage yuvImage);

	/**
	 * 释放Python中的算法实例
	 * @param algorithm
	 */
	void close(Algorithm algorithm);
	
	/**
	 * 用于验证py4j实例是否可用
	 * @return
	 */
	String hello();

}
