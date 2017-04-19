package com.sengled.media.interfaces.exceptions;

public class FeedException extends Exception{
	private static final long serialVersionUID = 3902845420449344635L;
 
 
    public FeedException() {
        super();
    }
 
    public FeedException(String message) {
        super(message);
    }
    public FeedException(Exception e){
    	super(e);
    }
}
