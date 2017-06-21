package com.sengled.mediaworker.object;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.sengled.mediaworker.algorithm.event.ObjectEvent;
import com.sengled.mediaworker.algorithm.service.dto.ObjectRecognitionResult;
import com.sengled.mediaworker.httpclient.HttpResponseResult;
import com.sengled.mediaworker.httpclient.IHttpClient;

/**
 * 物体识别线程池
 * @author media-liwei
 *
 */
@Component
public class ObjectRecognitionImpl implements InitializingBean,ObjectRecognition{
		
	private static final Logger LOGGER = LoggerFactory.getLogger(ObjectRecognitionImpl.class);
	
	@Autowired
	ObjectContextManager objectContextManager;
	@Autowired
	IHttpClient httpclient;
	
	@Value("${object.recognition.url}")
	private String objectRecognitionUrl;

	private ExecutorService threadPool;
	
	public void afterPropertiesSet() throws Exception {
		try {
			threadPool = Executors.newWorkStealingPool(200);
		} catch (Exception e) {
			LOGGER.error(e.getMessage(),e);
			System.exit(1);
		}		
	}
	public void sumbit(final String token,final byte[] nal,final Map<String,Object> objectConfig){
		threadPool.submit(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				handle(token,nal,objectConfig);
				return null;
			}
		});
	}

	private void handle(String token,byte[] nal,Map<String,Object> objectConfig) {
		ObjectContext objectContext = objectContextManager.findOrCreateStreamingContext(token);
		if(objectContext.isSkip()){
			return;
		}
		HttpEntity putEntity = new ByteArrayEntity(nal);
		HttpResponseResult result = httpclient.put(objectRecognitionUrl, putEntity);
		
		if(result.getCode().intValue() != 200){
			LOGGER.error("object recognition http code {},body:{}",result.getCode(),result.getBody());
			return;
		}
		String responseText = result.getBody();
		JSONObject jsonObj = JSONObject.parseObject(responseText);
		if(jsonObj.getJSONArray("objects").isEmpty()){
			LOGGER.info("object recognition NORESULT.");
			return;
		}
		
		ObjectRecognitionResult objectResult = JSONObject.toJavaObject(jsonObj, ObjectRecognitionResult.class);
		LOGGER.info("recognition object JSON result:{},javaBean Result{}",jsonObj.toJSONString(),objectResult.toString());
		
		
		ObjectEvent objectEvent = new ObjectEvent();
		
		//处理物体识别事件
		//TODO 分析结果，找出在zone中的结果，提交sqs s3
		matchZone(token,objectResult);
		
		
		
	}
	private void matchZone(String token,ObjectRecognitionResult orr){
		
	}
}





