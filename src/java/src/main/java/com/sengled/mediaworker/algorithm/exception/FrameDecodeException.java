package com.sengled.mediaworker.algorithm.exception;

public class FrameDecodeException extends Exception{
 
	private static final long serialVersionUID = 6102728459920628925L;
	public FrameDecodeException(String msg, Throwable cause) {
		super(msg, cause);
	}
	public FrameDecodeException(String msg) {
		super(msg);
	}
}
