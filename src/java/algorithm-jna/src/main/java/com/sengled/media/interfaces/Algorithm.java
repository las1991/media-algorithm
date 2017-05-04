package com.sengled.media.interfaces;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

public class Algorithm {
	private String algorithmModelId;
	private Map<String,Object> parameters;
	
	public Algorithm(String algorithmModelId,Map<String,Object> parameters){
		this.algorithmModelId = algorithmModelId;
		this.parameters = parameters;
	}
	@SuppressWarnings("unused")
	private Algorithm(){}
	
	public void setParameters(Map<String,Object> parameters){
		this.parameters = parameters;
	}

	public Map<String,Object>  getParameters(){
		return parameters;
	}
	public String getParametersJson(){
		return JSON.toJSONString(parameters);
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
