package com.sengled.mediaworker.algorithm.event;

public class ObjectEvent {

	private String token;
	private String result;
	private byte[] jpgData;

	
	
	public ObjectEvent(String token, String result, byte[] jpgData) {
		super();
		this.token = token;
		this.result = result;
		this.jpgData = jpgData;
	}



	@Override
	public String toString() {
		return "ObjectEvent [token=" + token + ", result=" + result + "]";
	}
	
}
