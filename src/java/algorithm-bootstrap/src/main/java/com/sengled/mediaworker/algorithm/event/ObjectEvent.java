package com.sengled.mediaworker.algorithm.event;

public class ObjectEvent {

	private String token;
	private String model;
	public ObjectEvent(String token, String model) {
		super();
		this.token = token;
		this.model = model;
	}
	@Override
	public String toString() {
		return "ObjectEvent [token=" + token + ", model=" + model + "]";
	}
	
	
	
	
}
