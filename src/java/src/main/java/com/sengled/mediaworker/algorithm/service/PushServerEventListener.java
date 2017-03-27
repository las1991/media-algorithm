package com.sengled.mediaworker.algorithm.service;

import org.springframework.stereotype.Component;

import com.google.common.eventbus.Subscribe;
import com.sengled.mediaworker.algorithm.event.MotionEvent;
@Component
public class PushServerEventListener {
	@Subscribe
	public void feedEvent(MotionEvent event){
	}

}
