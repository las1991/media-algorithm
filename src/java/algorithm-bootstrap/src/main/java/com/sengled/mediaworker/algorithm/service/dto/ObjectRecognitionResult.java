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
    private List<TargetObject> objects;
	
	public static class TargetObject{
		@JSONField(name = "bbox_pct")
		private List<Integer> bbox_pct;
		
		@JSONField(name = "type")
		private String type;
		
		@JSONField(name = "score")
		private double score;

		@JSONField(name = "frame")
		private int frame = 0;

        public List<Integer> getBbox_pct() {
            return bbox_pct;
        }

        public void setBbox_pct(List<Integer> bbox_pct) {
            this.bbox_pct = bbox_pct;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public double getScore() {
            return score;
        }

        public void setScore(double score) {
            this.score = score;
        }

        public int getFrame() {
            return frame;
        }

        public void setFrame(int frame) {
            this.frame = frame;
        }

        @Override
        public String toString() {
            return "Object [bbox_pct=" + bbox_pct + ", type=" + type + ", score=" + score + ", frame=" + frame + "]";
        }
		
	}

 
	@Override
    public String toString() {
        return "ObjectRecognitionResult [objects=" + objects + "]";
    }


    public List<String> getObjectBoxPct(){
		List<String> box = new ArrayList<>();
		for(TargetObject object : objects){
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


    public List<TargetObject> getObjects() {
        return objects;
    }


    public void setObjects(List<TargetObject> objects) {
        this.objects = objects;
    }
    
}
