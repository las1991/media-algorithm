package com.sengled.mediaworker.algorithm.exception;

public class S3IOException extends Exception{

	private static final long serialVersionUID = 6742011872353977183L;

	public S3IOException(String msg, Throwable cause) {
		super(msg, cause);
	}
	public S3IOException(String msg) {
		super(msg);
	}

}
