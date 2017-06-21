package com.sengled.mediaworker.httpclient;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
/**
 * httpclient 长连接工具
 * @author liwei
 * @Date   2016年12月2日 下午3:13:09 
 * @Desc
 */
@Configuration
@ConfigurationProperties
public class HttpClientConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger(HttpClientConfig.class);
	
	//http连接池
	@Value("${httpclient.pool.count}")
	protected Integer httpPoolNum;
	
	//http对每个远程主机最大连接数
	@Value("${httpclient.maxPerRoute}")
	protected Integer maxPerRoute;
	
	//每个主机keeplive时间
	@Value("${httpclient.keepaliveMs}")
	protected Long keepaliveMs;
	//数据传输超时
	@Value("${httpclient.socketTimeoutMs}")
	protected Integer 	socketTimeout;
	
	
	public HttpClient.HttpClientConfig getConfig(){
		HttpClient.HttpClientConfig config =   new HttpClient.HttpClientConfig();
		config.setHttpPoolNum(httpPoolNum);
		config.setKeepaliveMs(keepaliveMs);
		config.setMaxPerRoute(maxPerRoute);
		config.setSocketTimeout(socketTimeout);
		LOGGER.debug("httpclient config:{}"+config.toString());
		return config;
	}
	@Bean
	public IHttpClient getHttpClientUtil(){
		return new HttpClient(getConfig());
	}
	public static void main(String[] args) {
		HttpClient.HttpClientConfig config =   new HttpClient.HttpClientConfig();
		config.setHttpPoolNum(100);
		config.setKeepaliveMs(20000);
		config.setMaxPerRoute(40);
		config.setSocketTimeout(10000);
		IHttpClient client = new HttpClient(config);
		
		
	
		byte[] data = null;
		//File file = new File("D:\\test\\naldata");
		File file = new File("D:\\test\\detection-1.jpg");
		try {
			InputStream is = new FileInputStream(file);
			System.out.println("available:" + is.available());
			data = new byte[is.available()];
			is.read(data);
		} catch (Exception e) {
			e.printStackTrace();
		}
		

		String url = "http://brain.test.sengledcanada.com:8080/api/visual/recognition/object";
		int sum = 0;
		for(int i=0;i<10;i++){
			long startTime = System.currentTimeMillis();
			HttpEntity putEntity = new ByteArrayEntity(data);
			HttpResponseResult obj = client.put(url, putEntity );
			long cost = System.currentTimeMillis()  -  startTime;
			System.out.println(cost + " " + obj.getBody());
			sum += cost;
		}
		System.out.println(sum/10);
		
		
	}

	
}
