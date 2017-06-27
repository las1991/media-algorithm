package com.sengled.mediaworker.algorithm.service.dto;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson.annotation.JSONField;
/**
 * 物体识别结果
 * @author media-liwei
 *
 */
public class ObjectRecognitionResult {

	@JSONField(name = "objects")
    public List<Object> objects;
	
	public static class Object{
		@JSONField(name = "bbox_pct")
		public List<Integer> bbox_pct;
		
		@JSONField(name = "type")
		public String type;
		
		@JSONField(name = "score")
		public double score;

		@Override
		public String toString() {
			return "Object [bbox_pct=" + bbox_pct + ", type=" + type + ", score=" + score + "]";
		}
	}

	@Override
	public String toString() {
		return "ObjectRecognitionResult [objects=" + objects  + "]";
	}
	public List<String> getObjectBoxPct(){
		List<String> box = new ArrayList<>();
		for(Object object : objects){
			if(null == object.bbox_pct){
				continue;
			}
			if(4 != object.bbox_pct.size()){
				continue;
			}
			String pos = object.bbox_pct.get(0)+","+object.bbox_pct.get(1)+","+object.bbox_pct.get(2)+","+object.bbox_pct.get(3);
			box.add(pos);
		}
		return box;
	}
}
