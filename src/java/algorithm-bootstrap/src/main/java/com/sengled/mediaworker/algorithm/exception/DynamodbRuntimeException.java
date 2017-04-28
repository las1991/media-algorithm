package com.sengled.mediaworker.algorithm.exception;

public class DynamodbRuntimeException extends Exception{

	private static final long serialVersionUID = 6742011872353977183L;

	public DynamodbRuntimeException(String msg, Throwable cause) {
		super(msg, cause);
	}
	public DynamodbRuntimeException(String msg) {
		super(msg);
	}

}
