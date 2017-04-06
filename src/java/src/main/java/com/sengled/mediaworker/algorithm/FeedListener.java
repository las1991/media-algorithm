package com.sengled.mediaworker.algorithm;

import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.AsyncEventBus;
import com.sengled.mediaworker.algorithm.event.MotionEvent;
import com.sengled.mediaworker.algorithm.event.ObjectEvent;
import com.sengled.mediaworker.algorithm.service.DynamodbEventListener;



@Component
public class FeedListener implements InitializingBean{
	private static final Logger LOGGER = LoggerFactory.getLogger(FeedListener.class);
	private final static int EVENT_BUS_THREAD_COUNT = 100;
	private AsyncEventBus eventBus;
	@Autowired
	private DynamodbEventListener dynamodbEventListener;
	
	public FeedListener(){
		LOGGER.info("FeedListener init.EVENT_BUS_THREAD_COUNT:{}",EVENT_BUS_THREAD_COUNT);
		eventBus = new AsyncEventBus(Executors.newFixedThreadPool(EVENT_BUS_THREAD_COUNT));
        
	}
	@Override
	public void afterPropertiesSet() throws Exception {
		eventBus.register(dynamodbEventListener);
	}

	public void post(MotionEvent event){
		eventBus.post(event);
	}

	public void post(ObjectEvent event){
		eventBus.post(event);
	}
	
	public AsyncEventBus getEventBus() {
		return this.eventBus;
	}
 
	public void setEventBus(AsyncEventBus eventBus) {
		this.eventBus = eventBus;
	}



}
