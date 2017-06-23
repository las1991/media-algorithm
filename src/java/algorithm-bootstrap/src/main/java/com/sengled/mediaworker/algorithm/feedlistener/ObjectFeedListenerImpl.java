package com.sengled.mediaworker.algorithm.feedlistener;

import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.amazonaws.util.StringUtils;
import com.google.common.eventbus.AsyncEventBus;
import com.sengled.media.interfaces.YUVImage;
import com.sengled.media.interfaces.exceptions.EncodeException;
import com.sengled.mediaworker.algorithm.ObjectRecognition;
import com.sengled.mediaworker.algorithm.ProcessorManager;
import com.sengled.mediaworker.algorithm.context.ObjectContext;
import com.sengled.mediaworker.algorithm.context.ObjectContextManager;
import com.sengled.mediaworker.algorithm.context.StreamingContext;
import com.sengled.mediaworker.algorithm.decode.KinesisFrameDecoder.ObjectConfig;
import com.sengled.mediaworker.algorithm.event.ObjectEvent;
import com.sengled.mediaworker.algorithm.service.ObjectEventHandler;
import com.sengled.mediaworker.algorithm.service.dto.MotionFeedResult;
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
		
		String token = context.getToken();
		byte[] nal = context.getNalData();
		YUVImage yuvImage = context.getYuvImage();
		
		ObjectConfig objectConfig =  context.getConfig().getObjectConfig();
		ObjectConfig finalObjectConfig = new ObjectConfig();
		BeanUtils.copyProperties(objectConfig, finalObjectConfig);
		long startTime = System.currentTimeMillis();
		
		ObjectContext objectContext = objectContextManager.findOrCreateStreamingContext(context);
		String matchResult = objectContext.match(processorManager, httpclient, objectRecognitionUrl, token, nal, yuvImage, finalObjectConfig, motionFeedResult);
		//TODO if matchResult 无结果，则返回
		LOGGER.info("Token:{},match ObjectRecognition ObjectConfig:{} Cost:{}", token,finalObjectConfig,(System.currentTimeMillis() - startTime));
		byte[] jpgData;
		try {
			jpgData = processorManager.encode(token, yuvImage.getYUVData(), yuvImage.getWidth(), yuvImage.getHeight(), yuvImage.getWidth(), yuvImage.getHeight());
		} catch (EncodeException e) {
			LOGGER.error(e.getMessage(),e);
			return;
		}
		//TODO 提交物体识别结果
		ObjectEvent event = new ObjectEvent(token,matchResult,jpgData);
		eventBus.post(event );
	}
}
