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
	public void setJpgDate(byte[] jpgData) {
		this.jpgData = jpgData;
		
	}
	
	
 
}
