package com.sengled.mediaworker.algorithm;

import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.AsyncEventBus;
import com.sengled.media.interfaces.YUVImage;
import com.sengled.mediaworker.algorithm.decode.KinesisFrameDecoder.ObjectConfig;
import com.sengled.mediaworker.algorithm.event.ObjectEvent;
import com.sengled.mediaworker.algorithm.service.ObjectEventHandler;
import com.sengled.mediaworker.algorithm.service.dto.MotionFeedResult;
import com.sengled.mediaworker.object.ObjectRecognition;


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
	private ObjectRecognition objectRecognition;
	@Autowired
	private ObjectEventHandler objectEventHandler;
	
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
		
		String token = context.getToken();
		byte[] nal = context.getNalData();
		YUVImage yuvImage = context.getYuvImage();
		
		ObjectConfig objectConfig =  context.getConfig().getObjectConfig();
		ObjectConfig finalObjectConfig = new ObjectConfig();
		BeanUtils.copyProperties(objectConfig, finalObjectConfig);
		
		
		long startTime = System.currentTimeMillis();
		String matchResult = objectRecognition.match(token,nal,yuvImage,finalObjectConfig,motionFeedResult);
		LOGGER.info("Token:{},match ObjectRecognition ObjectConfig:{} Cost:{}", token,finalObjectConfig,(System.currentTimeMillis() - startTime));
		
		//TODO 提交结果
		ObjectEvent event = new ObjectEvent(token,"object");
		eventBus.post(event );
	}
}
