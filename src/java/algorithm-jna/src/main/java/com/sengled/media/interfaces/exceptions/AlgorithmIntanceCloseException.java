package com.sengled.media.interfaces.exceptions;

public class AlgorithmIntanceCloseException extends Exception{
	private static final long serialVersionUID = 3902845420449344635L;
 
 
    public AlgorithmIntanceCloseException() {
        super();
    }
 
    public AlgorithmIntanceCloseException(String message) {
        super(message);
    }
    public AlgorithmIntanceCloseException(Exception e) {
        super(e);
    }
}
