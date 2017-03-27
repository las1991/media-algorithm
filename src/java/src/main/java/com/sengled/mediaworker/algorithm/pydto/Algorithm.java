package com.sengled.mediaworker.algorithm.pydto;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Algorithm {
	private static final Logger LOGGER = LoggerFactory.getLogger(Algorithm.class);
	
	private String pythonObjectId;
	private Map<String,Object> parameters;
	

	public Algorithm(String pythonObjectId,Map<String,Object> parameters){
		this.pythonObjectId = pythonObjectId;
		this.parameters = parameters;
	}
	
	public void setParameters(Map<String,Object> parameters){
		this.parameters = parameters;
	}
	public String getPythonObjectId(){
		return pythonObjectId;
	}
	public void setPythonObjectId(String pythonObjectId){
		this.pythonObjectId = pythonObjectId;
	}
	public Map<String,Object>  getParameters(){
		return parameters;
	}

}
