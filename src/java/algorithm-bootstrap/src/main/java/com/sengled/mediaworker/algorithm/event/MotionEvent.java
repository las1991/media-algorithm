package com.sengled.mediaworker.algorithm.event;

import java.util.Date;
/**
 * motion 事件
 *
 */
public class MotionEvent {

	private String token;
	private String model;
	private Date utcDate;
	private byte[] jpgData;
	private String zoneId;
	
	
	public MotionEvent(String token, String model, Date utcDate, byte[] jpgData, String zoneId) {
		this.token = token;
		this.model = model;
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
	public String getModel() {
		return model;
	}
	public void setModel(String model) {
		this.model = model;
	}
	public Date getUtcDate() {
		return utcDate;
	}
	public void setUtcDate(Date utcDate) {
		this.utcDate = utcDate;
	}
	@Override
	public String toString() {
		return "MotionEvent [token=" + token + ", model=" + model + ", utcDate=" + utcDate + ", zoneId=" + zoneId + "]";
	}
	
	
 
}
