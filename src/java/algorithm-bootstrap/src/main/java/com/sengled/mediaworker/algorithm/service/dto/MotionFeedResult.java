package com.sengled.mediaworker.algorithm.service.dto;

import java.util.List;

import com.alibaba.fastjson.annotation.JSONField;

public class MotionFeedResult {
	@JSONField(name = "motion")
    public List<ZoneInfo> motion;
	
	
	public static class ZoneInfo{
		@JSONField(name = "zone_id")
		public Integer zone_id;
		
		@JSONField(name = "boxs")
		public List<List<Integer>> boxs;
	}
	
	public boolean verify(){
		if(motion ==null || motion.isEmpty()){
			return false;
		}
		for(ZoneInfo zi : motion){
			if(zi.zone_id == null ){
				return false;
			}
		 }
		return true;
	}

	@Override
	public String toString() {
		return "MotionFeedResult [motion=" + motion + "]";
	}
	
}
