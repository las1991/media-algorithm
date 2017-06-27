package com.sengled.mediaworker.algorithm.service.dto;

import java.util.Set;

public class ObjectMatchResult {
	//String matchResult = "{'zone_id':54,'type':['persion','car'],'zone_id':12,'type':['persion','dog']}";
	Integer zone_id;
	Set<Integer> types;
	public ObjectMatchResult(Integer zone_id, Set<Integer> types) {
		super();
		this.zone_id = zone_id;
		this.types = types;
	}
	public void addType(Integer type){
		types.add(type);
	}
}
