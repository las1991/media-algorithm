package com.sengled.mediaworker.httpclient;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpClient implements IHttpClient {
	private static final Logger LOGGER = LoggerFactory.getLogger(HttpClient.class);
	
	private  PoolingHttpClientConnectionManager cm = null;
	private  RequestConfig requestConfig = null;
	private long keepaliveMs = 1000;
	private CloseableHttpClient client;
	public HttpClient(HttpClientConfig config){
		cm = new PoolingHttpClientConnectionManager();
		requestConfig = RequestConfig.custom()
				 .setConnectionRequestTimeout(3000)//从连接池获取连接超时
				 .setSocketTimeout(config.getSocketTimeout())//数据传输超时
				 .setConnectTimeout(3000)//建立连接超时
				 .build();
		cm.setMaxTotal(config.getHttpPoolNum());//连接池SIZE
		cm.setDefaultMaxPerRoute(config.getMaxPerRoute()); //连接每个远程主机数据的SIZE 
		this.keepaliveMs = config.getKeepaliveMs();
		client = HttpClients.custom()
				.setConnectionManager(cm)
				.useSystemProperties()
				.disableConnectionState()
				.setDefaultRequestConfig(requestConfig)
				.setKeepAliveStrategy(new ConnectionKeepAliveStrategy(){
					@Override
					public long getKeepAliveDuration(HttpResponse arg0, HttpContext arg1) {
						return keepaliveMs;
					}
				}).build();
	}

	public   HttpResponseResult  get(String url){
		LOGGER.debug("request url:" + url);
		HttpResponseResult hrr = new HttpResponseResult();
		HttpGet httpGet = new HttpGet(url);
		try {
			// 执行get请求
			HttpResponse httpResponse = client.execute(httpGet);
			// 获取响应消息实体
			HttpEntity entity = httpResponse.getEntity();
			// 响应状态
			LOGGER.debug("status:" + httpResponse.getStatusLine());
			hrr.setCode(httpResponse.getStatusLine().getStatusCode());
			// 判断响应实体是否为空
			if (entity != null) {
				String body = EntityUtils.toString(entity, "UTF-8");
				LOGGER.debug("contentEncoding:" + entity.getContentEncoding());
				LOGGER.debug("response content:" + body);
				hrr.setBody(body);
				return hrr;
			}
		} catch (Exception e) {
			LOGGER.error("get {}, {}", url, e.getMessage());
		} 
		return hrr;
	}
	
	public   HttpResponseResult post(String url,String postdata){
		LOGGER.debug("request url:" + url);
		LOGGER.debug("request postdata:" + postdata);
		HttpResponseResult hrr = new HttpResponseResult();

		try {
			HttpPost httpPost = new HttpPost(url);
			StringEntity postEntity = new StringEntity(postdata, ContentType.APPLICATION_JSON);
			httpPost.setEntity(postEntity);
			
			// 执行get请求
			HttpResponse httpResponse = client.execute(httpPost);
			// 获取响应消息实体
			HttpEntity entity = httpResponse.getEntity();
			String body = null != entity ? EntityUtils.toString(entity, "UTF-8") : null;
			LOGGER.debug("{} {} {}", httpResponse.getStatusLine(), url, body);

			
			hrr.setCode(httpResponse.getStatusLine().getStatusCode());
			// 判断响应实体是否为空
			if (entity != null) {
				hrr.setBody(body);
				return hrr;
			}
		} catch (Exception e) {
			LOGGER.error("post {}, {}", url, e.getMessage());
		} 
		return hrr;
	}

	public HttpResponseResult put(String url,HttpEntity putEntity){
		LOGGER.debug("request url:" + url);
		
		HttpResponseResult hrr = new HttpResponseResult();

		HttpPut putRequest = new HttpPut(url);
		putRequest.setEntity(putEntity );
		CloseableHttpResponse  httpResponse = null;
		try {
			// 执行put请求
			httpResponse = client.execute(putRequest);
			// 获取响应消息实体
			HttpEntity entity = httpResponse.getEntity();
			// 响应状态
			LOGGER.debug("status:" + httpResponse.getStatusLine());
			hrr.setCode(httpResponse.getStatusLine().getStatusCode());
			// 判断响应实体是否为空
			if (entity != null) {
				String body = EntityUtils.toString(entity, "UTF-8");
				LOGGER.debug("response contentEncoding:" + entity.getContentEncoding());
				LOGGER.debug("response content:" + body);
				hrr.setBody(body);
				return hrr;
			}
			
		} catch (Exception e) {
			LOGGER.error("put {}, {}", url, e.getMessage());
		}finally{
			if( null != httpResponse){
				try {
					httpResponse.close();
				} catch (IOException e2) {
					LOGGER.error(e2.getMessage(),e2);
				}
			}
		}
		System.out.println("cm:"+cm.getTotalStats());
		return hrr;
	}
	
	
	static class HttpClientConfig {
		private int httpPoolNum;
		private int maxPerRoute;
		private long keepaliveMs;
		private int socketTimeout;
		
		
		public int getSocketTimeout() {
			return socketTimeout;
		}
		public void setSocketTimeout(int socketTimeout) {
			this.socketTimeout = socketTimeout;
		}
		public void setKeepaliveMs(long keepaliveMs) {
			this.keepaliveMs = keepaliveMs;
		}
		public int getHttpPoolNum() {
			return httpPoolNum;
		}
		public void setHttpPoolNum(int httpPoolNum) {
			this.httpPoolNum = httpPoolNum;
		}
		public int getMaxPerRoute() {
			return maxPerRoute;
		}
		public void setMaxPerRoute(int maxPerRoute) {
			this.maxPerRoute = maxPerRoute;
		}
		public long getKeepaliveMs() {
			return keepaliveMs;
		}
		public void setKeepaliveMs(Long keepaliveMs) {
			this.keepaliveMs = keepaliveMs;
		}
		@Override
		public String toString() {
			return super.toString() + "httpPoolNum:"+httpPoolNum + " maxPerRoute:"+maxPerRoute+" keepaliveMs:"+keepaliveMs;
		}
		
	}
	public int getAvailable(){
		return cm.getTotalStats().getAvailable();
	}	
}
