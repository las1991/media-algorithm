package com.sengled.mediaworker.algorithm;

import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.AsyncEventBus;
import com.sengled.media.interfaces.YUVImage;
import com.sengled.media.interfaces.exceptions.EncodeException;
import com.sengled.mediaworker.algorithm.event.MotionEvent;
import com.sengled.mediaworker.algorithm.service.MotionEventHandler;
import com.sengled.mediaworker.algorithm.service.dto.MotionFeedResult;
import com.sengled.mediaworker.algorithm.service.dto.MotionFeedResult.ZoneInfo;


/**
 * Feed结果监听器
 * @author media-liwei
 *
 */
@Component
public class MotionFeedListenerImpl implements FeedListener,InitializingBean{
	private static final Logger LOGGER = LoggerFactory.getLogger(MotionFeedListenerImpl.class);
	private final static int EVENT_BUS_THREAD_COUNT = 100;
	private AsyncEventBus eventBus;
	@Autowired
	private MotionEventHandler motionEventHandler;
	@Autowired
	ProcessorManager processorManagerImpl;
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
		LOGGER.info("MotionFeedListener init.EVENT_BUS_THREAD_COUNT:{}",EVENT_BUS_THREAD_COUNT);
		eventBus = new AsyncEventBus(Executors.newFixedThreadPool(EVENT_BUS_THREAD_COUNT));
		eventBus.register(motionEventHandler);
	}

	@Override
	public void feedResultHandle(StreamingContext context, MotionFeedResult motionFeedResult) {
		String token =  context.getToken();
		String model = context.getModel();
		
		if(!context.isReport()){
			LOGGER.debug("Token:{} get Motion.But isReport is false.",token);
			return;
		}
		ZoneInfo  zone = motionFeedResult.motion.get(0);
		YUVImage yuvImage = context.getYuvImage();
		
		byte[] jpgData;
		try {
			jpgData = processorManagerImpl.encode(context.getToken(), yuvImage.getYUVData(), yuvImage.getWidth(), yuvImage.getHeight(), yuvImage.getWidth(), yuvImage.getHeight());
		} catch (EncodeException e) {
			LOGGER.error(e.getMessage(),e);
			return;
		}
		LOGGER.info("Token:{},Get {}. zoneId:{},",token,model,zone.zone_id);
		MotionEvent event = new MotionEvent(token,model,context.getUtcDateTime(),jpgData,zone.zone_id+"");
		eventBus.post(event );
		context.setLastMotionTimestamp(context.getUtcDateTime().getTime());
	}
}
