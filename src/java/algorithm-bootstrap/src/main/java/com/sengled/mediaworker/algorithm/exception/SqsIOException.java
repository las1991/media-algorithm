package com.sengled.mediaworker.algorithm.exception;

public class SqsIOException extends Exception{

	private static final long serialVersionUID = 6742011872353977183L;

	public SqsIOException(String msg, Throwable cause) {
		super(msg, cause);
	}
	public SqsIOException(String msg) {
		super(msg);
	}

}
