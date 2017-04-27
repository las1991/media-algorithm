package com.sengled.mediaworker.algorithm.exception;

public class DecodeException extends Exception{

	private static final long serialVersionUID = 6742011872353977183L;

	public DecodeException(String msg, Throwable cause) {
		super(msg, cause);
	}
	public DecodeException(String msg) {
		super(msg);
	}

}
