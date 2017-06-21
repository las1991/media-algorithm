package com.sengled.mediaworker.object;

/**
 * 物体识别上下文
 * @author media-liwei
 *
 */
public class ObjectContext {
	private String token;
	private boolean isReport = true;
	
	public ObjectContext(String token) {
		this.token = token;
	}
	
	public boolean isSkip(){
		
		return false;
	}
 
}
