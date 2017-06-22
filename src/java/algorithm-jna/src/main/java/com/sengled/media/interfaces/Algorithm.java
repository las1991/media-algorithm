package com.sengled.media.interfaces;

public class Algorithm {
	private String algorithmModelId;
	private String parameters;
	
	public Algorithm(String algorithmModelId,String jsonString){
		this.algorithmModelId = algorithmModelId;
		this.parameters = jsonString;
	}
	@SuppressWarnings("unused")
	private Algorithm(){}
	
	public void setParameters(String jsonString){
		this.parameters = jsonString;
	}

	public String getParametersJson(){
		return parameters;
	}
	public String getAlgorithmModelId() {
		return algorithmModelId;
	}
	public void setAlgorithmModelId(String algorithmModelId) {
		this.algorithmModelId = algorithmModelId;
	}
	@Override
	public String toString() {
		return super.toString() + " algorithmModelId:" +algorithmModelId + " parameters:"+parameters;
	}
}
