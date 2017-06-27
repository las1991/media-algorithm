package com.sengled.mediaworker.algorithm.event;

import java.util.Date;
/**
 * motion 事件
 *
 */
public class MotionEvent {

	private String token;
	private Date utcDate;
	private byte[] jpgData;
	private String zoneId;
	
	
	public MotionEvent(String token, Date utcDate, byte[] jpgData, String zoneId) {
		this.token = token;
		this.utcDate = utcDate;
		this.jpgData = jpgData;
		this.zoneId = zoneId;
	}
	private MotionEvent(){}
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
	@Override
	public String toString() {
		return "MotionEvent [token=" + token + ", utcDate=" + utcDate + ", zoneId=" + zoneId + "]";
	}
	
	
 
}
