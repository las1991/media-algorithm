package com.sengled.mediaworker.algorithm.exception;

public class StreamingContextNotFoundException extends Exception{
	private static final long serialVersionUID = 3516458904679328900L;
	public StreamingContextNotFoundException(String msg, Throwable cause) {
		super(msg, cause);
	}
	public StreamingContextNotFoundException(String msg) {
		super(msg);
	}
}
