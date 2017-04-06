package com.sengled.mediaworker.algorithm.service.dto;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.List;

/**
 * 算法服务处理结果的实体
 * Created by yangyonghui on 2016/11/24.
 */
public class AlgorithmResult {
    public static final String SLS_EVENT_TYPE_MOTION = "event_motion";
    public static final String SLS_EVENT_TYPE_HUMAN = "event_human";
    public static final String SLS_EVENT_TYPE_OBJECT = "event_object";
    

    @JSONField(name="stream_id")
    private String streamId;

    @JSONField(name="zone_id")
    private Long zoneId;

    @JSONField(name="event_type")
    private String eventType;

    @JSONField(name="timestamp")
    private String timeStamp;

    private String bigImage;

    private String smallImage;

    @JSONField(name = "dataList")
    private List<ObjectRecognitionInnerDto> dataList;


    public String getStreamId() {
        return streamId;
    }

    public void setStreamId(String streamId) {
        this.streamId = streamId;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getBigImage() {
        return bigImage;
    }

    public void setBigImage(String bigImage) {
        this.bigImage = bigImage;
    }

    public String getSmallImage() {
        return smallImage;
    }

    public void setSmallImage(String smallImage) {
        this.smallImage = smallImage;
    }

    public List<ObjectRecognitionInnerDto> getDataList() {
        return dataList;
    }

    public void setDataList(List<ObjectRecognitionInnerDto> dataList) {
        this.dataList = dataList;
    }
}
