package com.sengled.mediaworker.algorithm.exception;

public class StreamingContextInitException extends Exception{
 
	private static final long serialVersionUID = 1876561519496658412L;

	public StreamingContextInitException(String msg, Throwable cause) {
		super(msg, cause);
	}
	public StreamingContextInitException(String msg) {
		super(msg);
	}
}
