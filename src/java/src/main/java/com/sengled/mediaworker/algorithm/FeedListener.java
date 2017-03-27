package com.sengled.mediaworker.algorithm;

import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.AsyncEventBus;
import com.sengled.mediaworker.algorithm.event.MotionEvent;
import com.sengled.mediaworker.algorithm.event.ObjectEvent;



@Component
public class FeedListener {
	private static final Logger LOGGER = LoggerFactory.getLogger(FeedListener.class);
	private final static int EVENT_BUS_THREAD_COUNT = 10;
	private AsyncEventBus eventBus;
	
	public FeedListener(){
		LOGGER.info("FeedListener init.EVENT_BUS_THREAD_COUNT:{}",EVENT_BUS_THREAD_COUNT);
		eventBus = new AsyncEventBus(Executors.newFixedThreadPool(EVENT_BUS_THREAD_COUNT));
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
