package com.sengled.mediaworker.algorithm.exception;

public class SqsRuntimeException extends Exception{

	private static final long serialVersionUID = 6742011872353977183L;

	public SqsRuntimeException(String msg, Throwable cause) {
		super(msg, cause);
	}
	public SqsRuntimeException(String msg) {
		super(msg);
	}

}
