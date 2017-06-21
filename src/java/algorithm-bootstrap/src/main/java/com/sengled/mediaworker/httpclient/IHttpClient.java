package com.sengled.mediaworker.httpclient;

import org.apache.http.HttpEntity;

public interface IHttpClient {
	HttpResponseResult post(String url,String data);

	HttpResponseResult get(String url);

	HttpResponseResult put(String url,HttpEntity putEntity);
}

