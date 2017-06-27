package com.sengled.mediaworker.algorithm.feedlistener;

import java.util.concurrent.Executors;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.Multimap;
import com.google.common.eventbus.AsyncEventBus;
import com.sengled.media.interfaces.YUVImage;
import com.sengled.media.interfaces.exceptions.EncodeException;
import com.sengled.mediaworker.algorithm.ImageUtils;
import com.sengled.mediaworker.algorithm.ProcessorManager;
import com.sengled.mediaworker.algorithm.context.ObjectContext;
import com.sengled.mediaworker.algorithm.context.ObjectContextManager;
import com.sengled.mediaworker.algorithm.context.StreamingContext;
import com.sengled.mediaworker.algorithm.decode.KinesisFrameDecoder.ObjectConfig;
import com.sengled.mediaworker.algorithm.event.ObjectEvent;
import com.sengled.mediaworker.algorithm.service.ObjectEventHandler;
import com.sengled.mediaworker.algorithm.service.dto.MotionFeedResult;
import com.sengled.mediaworker.algorithm.service.dto.ObjectRecognitionResult.Object;
import com.sengled.mediaworker.httpclient.HttpResponseResult;
import com.sengled.mediaworker.httpclient.IHttpClient;


/**
 *
 * @author media-liwei
 *
 */
@Component
public class ObjectFeedListenerImpl implements FeedListener,InitializingBean{
	private static final Logger LOGGER = LoggerFactory.getLogger(ObjectFeedListenerImpl.class);
	
	private final static int EVENT_BUS_THREAD_COUNT = 100;
	private AsyncEventBus eventBus;
	
	@Autowired
	ObjectContextManager objectContextManager;
	@Autowired
	private ObjectEventHandler objectEventHandler;
	@Autowired
	ProcessorManager  processorManager;
	@Autowired
	IHttpClient httpclient;
	
	@Value("${object.recognition.url}")
	private String objectRecognitionUrl;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		try {
			initialize();
		} catch (Exception e) {
			LOGGER.error(e.getMessage(),e);
			System.exit(1);
		}
	}
	private void initialize(){
		LOGGER.info("ObjectFeedListener init.EVENT_BUS_THREAD_COUNT:{}",EVENT_BUS_THREAD_COUNT);
		eventBus = new AsyncEventBus(Executors.newFixedThreadPool(EVENT_BUS_THREAD_COUNT));
		eventBus.register(objectEventHandler);
	}
	@Override
	public void feedResultHandle(StreamingContext context, MotionFeedResult motionFeedResult) {
		ObjectConfig objectConfig =  context.getConfig().getObjectConfig();
		if(null == objectConfig){
			LOGGER.error("Token:{},objectConfig is null config:{}",context.getToken(),context.getConfig());
			return;
		}
		
		long startTime = System.currentTimeMillis();
		ObjectContext objectContext = objectContextManager.findOrCreateStreamingContext(context);
		if (objectContext.isSkip()) {
			return;
		}
		
		HttpEntity putEntity = new ByteArrayEntity(objectContext.getNalData());
		HttpResponseResult result = httpclient.put(objectRecognitionUrl, putEntity);
		if (result.getCode().intValue() != 200) {
			LOGGER.error("object recognition http code {},body:{}", result.getCode(), result.getBody());
			return ;
		}
		
		Multimap<Integer, Object>  matchResult = objectContext.match(result.getBody(), motionFeedResult);
		if(matchResult ==null || matchResult.isEmpty()){
			LOGGER.info("Token:{},Object Match Result is NULL",objectContext.getToken());
			return;
		}

		LOGGER.info("Token:{},match ObjectRecognition ObjectConfig:{} Cost:{}", objectContext.getToken(),objectConfig,(System.currentTimeMillis() - startTime));
		byte[] jpgData;
		try {
			YUVImage yuvImage = objectContext.getYuvImage();
			jpgData = processorManager.encode(objectContext.getToken(), yuvImage.getYUVData(), yuvImage.getWidth(), yuvImage.getHeight(), yuvImage.getWidth(), yuvImage.getHeight());
		} catch (EncodeException e) {
			LOGGER.error(e.getMessage(),e);
			return;
		}
		ObjectEvent event = new ObjectEvent(objectContext.getToken(),matchResult,jpgData,objectContext.getUtcDateTime());
		eventBus.post(event );
		objectContext.setLastObjectTimestamp(objectContext.getUtcDateTime().getTime());
		
		if (LOGGER.isDebugEnabled()) {
			//ImageUtils.draw(event.getToken(), jpgData,objectContext.getYuvImage() objectConfig,objectRecognitionResult, motionFeedResult,objectMatchResult);
			ImageUtils.draw(event.getToken(), jpgData, objectContext.getYuvImage(), objectConfig, motionFeedResult, matchResult);
		}
	}
}
