package com.sengled.mediaworker.algorithm.event;

import java.util.Date;

import org.apache.commons.lang.StringUtils;

import com.google.common.collect.Multimap;
import com.sengled.mediaworker.algorithm.service.dto.ObjectRecognitionResult.Object;

public class ObjectEvent {

	private String token;
	private Multimap<Integer, Object> result;
	private byte[] jpgData;
	private Date utcDate;
	private int fileExpiresHours;

	public ObjectEvent(String partitionKey, Multimap<Integer, Object> result, byte[] jpgData,int fileExpiresHours, Date utcDate) {
		super();
		this.token = getToken(partitionKey);
		this.result = result;
		this.jpgData = jpgData;
		this.utcDate = utcDate;
		this.fileExpiresHours = fileExpiresHours;
	}

	@Override
	public String toString() {
		return "ObjectEvent [token=" + token + ", result=" + result + "]";
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public Multimap<Integer, Object> getResult() {
		return result;
	}

	public void setResult(Multimap<Integer, Object> result) {
		this.result = result;
	}

	public byte[] getJpgData() {
		return jpgData;
	}

	public void setJpgData(byte[] jpgData) {
		this.jpgData = jpgData;
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
    
    private String getToken(String partitionKey){
    	String token =  partitionKey.split(",")[0];
    	if(StringUtils.isNotBlank(token)){
    		return token;
    	}
    	return partitionKey;
    }

}
