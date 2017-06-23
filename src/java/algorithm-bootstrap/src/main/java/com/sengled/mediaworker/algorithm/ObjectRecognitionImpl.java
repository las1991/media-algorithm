package com.sengled.mediaworker.algorithm;

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
import com.google.common.eventbus.AsyncEventBus;
import com.sengled.media.interfaces.YUVImage;
import com.sengled.mediaworker.algorithm.context.ObjectContext;
import com.sengled.mediaworker.algorithm.context.ObjectContextManager;
import com.sengled.mediaworker.algorithm.decode.KinesisFrameDecoder.ObjectConfig;
import com.sengled.mediaworker.algorithm.service.ObjectEventHandler;
import com.sengled.mediaworker.algorithm.service.dto.MotionFeedResult;
import com.sengled.mediaworker.algorithm.service.dto.ObjectRecognitionResult;
import com.sengled.mediaworker.httpclient.HttpResponseResult;
import com.sengled.mediaworker.httpclient.IHttpClient;

/**
 * 物体识别
 * @author media-liwei
 *
 */
@Component
public class ObjectRecognitionImpl implements InitializingBean,ObjectRecognition{
	private static final Logger LOGGER = LoggerFactory.getLogger(ObjectRecognitionImpl.class);
	
	private final static int EVENT_BUS_THREAD_COUNT = 50;
	
	@Autowired
	ObjectContextManager objectContextManager;
	@Autowired
	IHttpClient httpclient;
	@Autowired
	ObjectEventHandler objectEventHandler;
	@Autowired
	ProcessorManager  processorManager;
	@Autowired(required=false)
	RectangleUtils drawZoneObjectMotionFrame;
	
	@Value("${object.recognition.url}")
	private String objectRecognitionUrl;
	
	private AsyncEventBus eventBus;

	public void afterPropertiesSet() throws Exception {
		try {
			initialize();
		} catch (Exception e) {
			LOGGER.error(e.getMessage(),e);
			System.exit(1);
		}		
	}
	
	private void initialize(){
		LOGGER.info("ObjectRecognition init.EVENT_BUS_THREAD_COUNT:{}",EVENT_BUS_THREAD_COUNT);
		eventBus = new AsyncEventBus(Executors.newFixedThreadPool(EVENT_BUS_THREAD_COUNT));
		eventBus.register(objectEventHandler);
	}

	public String match(String token,byte[] nal,YUVImage yuvImage,ObjectConfig objectConfig,MotionFeedResult motionFeedResult) {
		ObjectContext objectContext = objectContextManager.findOrCreateStreamingContext(token);
		if(objectContext.isSkip()){
			return null;
		}
		HttpEntity putEntity = new ByteArrayEntity(nal);
		HttpResponseResult result = httpclient.put(objectRecognitionUrl, putEntity);
		
		if(result.getCode().intValue() != 200){
			LOGGER.error("object recognition http code {},body:{}",result.getCode(),result.getBody());
			return null;
		}
		String responseText = result.getBody();
		JSONObject jsonObj = JSONObject.parseObject(responseText);
		if(jsonObj.getJSONArray("objects").isEmpty()){
			LOGGER.info("object recognition NORESULT.");
			return null;
		}
		
		ObjectRecognitionResult objectResult = JSONObject.toJavaObject(jsonObj, ObjectRecognitionResult.class);
		LOGGER.info("recognition object JSON result:{},javaBean Result{}",jsonObj.toJSONString(),objectResult.toString());
		
		return matchZone(token,yuvImage,objectConfig,objectResult,motionFeedResult);
		
	}
	private String matchZone(String token,YUVImage yuvImage,ObjectConfig objectConfig ,ObjectRecognitionResult objectRecognitionResult,MotionFeedResult motionFeedResult){
		LOGGER.info("Token:{},objectConfig:{},ObjectRecognitionResult:{},MotionFeedResult:{}",token,JSONObject.toJSON(objectConfig),JSONObject.toJSON(objectRecognitionResult),JSONObject.toJSON(motionFeedResult));
		if(LOGGER.isDebugEnabled()){
			drawZoneObjectMotionFrame.draw(token, yuvImage, objectConfig, objectRecognitionResult, motionFeedResult);	
		}
		String matchResult = "{'zone_id':54,'type':['persion','car'],'zone_id':12,'type':['persion','dog']}";
		return matchResult;
	}
}



