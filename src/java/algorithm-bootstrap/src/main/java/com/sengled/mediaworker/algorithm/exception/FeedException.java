package com.sengled.mediaworker.algorithm.exception;

public class FeedException extends Exception{
 
	private static final long serialVersionUID = 3516458904679328900L;
	public FeedException(String msg, Throwable cause) {
		super(msg, cause);
	}
	public FeedException(String msg) {
		super(msg);
	}
}
