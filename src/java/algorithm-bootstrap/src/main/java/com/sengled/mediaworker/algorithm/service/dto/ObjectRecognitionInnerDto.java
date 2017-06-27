package com.sengled.mediaworker.algorithm.service.dto;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * Created by admin on 2016/12/15.
 */
public class ObjectRecognitionInnerDto {
    private Long zoneId;
    private String pos;
    @JSONField(name="object")
    private Integer targetType;
    private double score;

    public ObjectRecognitionInnerDto(Long zoneId, String pos, Integer targetType, double score) {
		super();
		this.zoneId = zoneId;
		this.pos = pos;
		this.targetType = targetType;
		this.score = score;
	}

	public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }

    public String getPos() {
        return pos;
    }

    public void setPos(String pos) {
        this.pos = pos;
    }

    public Integer getTargetType() {
        return targetType;
    }

    public void setTargetType(Integer targetType) {
        this.targetType = targetType;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    @Override
    public String toString() {
        return "ObjectRecognitionInnerDto{" +
                "zoneId=" + zoneId +
                ", pos='" + pos + '\'' +
                ", targetType=" + targetType +
                ", score=" + score +
                '}';
    }
}
