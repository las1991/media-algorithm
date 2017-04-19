package com.sengled.media.interfaces.exceptions;

public class EncodeException extends Exception{
 
	private static final long serialVersionUID = -4432734148737487035L;

	public EncodeException() {
        super();
    }
 
    public EncodeException(String message) {
        super(message);
    }
}
