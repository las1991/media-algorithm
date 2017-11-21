package com.sengled.mediaworker.algorithm.event;

import java.util.Date;

import org.apache.commons.lang.StringUtils;

/**
 * motion 事件
 *
 */
public class MotionEvent {

    private String token;
    private Date utcDate;
    private byte[] jpgData;
    private String zoneId;
    private int fileExpiresHours;

    public MotionEvent(String partitionKey, Date utcDate, byte[] jpgData, int fileExpiresHours, String zoneId) {
        this.token = getToken(partitionKey);
        this.utcDate = utcDate;
        this.jpgData = jpgData;
        this.fileExpiresHours = fileExpiresHours;
        this.zoneId = zoneId;
    }

    private MotionEvent() {
    }

    public byte[] getJpgData() {
        return jpgData;
    }

    public void setJpgData(byte[] jpgData) {
        this.jpgData = jpgData;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Date getUtcDate() {
        return utcDate;
    }

    public void setUtcDate(Date utcDate) {
        this.utcDate = utcDate;
    }

    public int getFileExpiresHours() {
        return fileExpiresHours;
    }

    public int getFileExpiresDays() {
        return   fileExpiresHours % 24 == 0 ? (fileExpiresHours / 24) : (fileExpiresHours / 24 + 1);
    }

    @Override
    public String toString() {
        return "MotionEvent [token=" + token + ", utcDate=" + utcDate + ", zoneId=" + zoneId + "]";
    }

    private String getToken(String partitionKey){
    	String token =  partitionKey.split(",")[0];
    	if(StringUtils.isNotBlank(token)){
    		return token;
    	}
    	return partitionKey;
    }
}
