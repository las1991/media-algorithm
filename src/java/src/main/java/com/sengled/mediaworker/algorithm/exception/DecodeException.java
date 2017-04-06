package com.sengled.mediaworker.algorithm.exception;

import java.util.concurrent.ExecutionException;

public class DecodeException extends ExecutionException{

	private static final long serialVersionUID = 6742011872353977183L;

	public DecodeException(String msg, Throwable cause) {
		super(msg, cause);
	}
	public DecodeException(String msg) {
		super(msg);
	}

}
