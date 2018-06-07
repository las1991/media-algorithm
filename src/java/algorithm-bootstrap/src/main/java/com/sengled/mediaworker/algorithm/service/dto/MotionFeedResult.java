package com.sengled.mediaworker.algorithm.service.dto;

import java.util.List;

import com.alibaba.fastjson.annotation.JSONField;

public class MotionFeedResult {
	@JSONField(name = "motion")
    private List<ZoneInfo> motion;
	
	
	public static class ZoneInfo{
		@JSONField(name = "zone_id")
		private Integer zone_id;
		
		@JSONField(name = "boxs")
		private List<List<Integer>> boxs;

        public Integer getZone_id() {
            return zone_id;
        }

        public void setZone_id(Integer zone_id) {
            this.zone_id = zone_id;
        }

        public List<List<Integer>> getBoxs() {
            return boxs;
        }

        public void setBoxs(List<List<Integer>> boxs) {
            this.boxs = boxs;
        }
		
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

    public List<ZoneInfo> getMotion() {
        return motion;
    }

    public void setMotion(List<ZoneInfo> motion) {
        this.motion = motion;
    }

    @Override
    public String toString() {
        return "MotionFeedResult [motion=" + motion + "]";
    }
	
	
}
