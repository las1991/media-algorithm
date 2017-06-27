package com.sengled.mediaworker.httpclient;

public class HttpResponseResult {
	/**
	 * 服务端返回状态码
	 */
	private Integer code = 555;
	
	/**
	 * 服务端返回内容
	 */
	private String body = "";
	public Integer getCode() {
		return code;
	}
	public void setCode(Integer code) {
		this.code = code;
	}
	public String getBody() {
		return body;
	}
	public void setBody(String body) {
		this.body = body;
	}
	@Override
	public String toString() {
		return "code:" + code + " body:"+body;
	}

}

