package com.sengled.media.interfaces.exceptions;

public class AlgorithmIntanceCreateException extends Exception{
	private static final long serialVersionUID = 3902845420449344635L;
 
 
    public AlgorithmIntanceCreateException() {
        super();
    }
 
    public AlgorithmIntanceCreateException(String message) {
        super(message);
    }
    
    public AlgorithmIntanceCreateException(Exception e) {
         super(e);
    }
}
