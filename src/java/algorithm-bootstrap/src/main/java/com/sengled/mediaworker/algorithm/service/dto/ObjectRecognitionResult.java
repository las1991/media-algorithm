package com.sengled.mediaworker.algorithm.service.dto;

import java.util.List;

import com.alibaba.fastjson.annotation.JSONField;

public class ObjectRecognitionResult {

	@JSONField(name = "objects")
    public List<Object> objects;
	
	public static class Object{
		@JSONField(name = "bbox_pct")
		public List<Integer> bbox_pct;
		@JSONField(name = "type")
		public String type;
		@Override
		public String toString() {
			return "Object [bbox_pct=" + bbox_pct + ", type=" + type + "]";
		}
		
	}

	@Override
	public String toString() {
		return "ObjectRecognitionResult [objects=" + objects  + "]";
	}
	
}
