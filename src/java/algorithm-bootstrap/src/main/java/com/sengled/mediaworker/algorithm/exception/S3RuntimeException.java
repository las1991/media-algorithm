package com.sengled.mediaworker.algorithm.exception;

public class S3RuntimeException extends Exception{

	private static final long serialVersionUID = 6742011872353977183L;

	public S3RuntimeException(String msg, Throwable cause) {
		super(msg, cause);
	}
	public S3RuntimeException(String msg) {
		super(msg);
	}

}
