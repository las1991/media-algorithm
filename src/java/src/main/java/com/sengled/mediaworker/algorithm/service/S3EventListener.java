package com.sengled.mediaworker.algorithm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.Subscribe;
import com.sengled.mediaworker.algorithm.event.MotionEvent;
import com.sengled.mediaworker.s3.AmazonS3Template;
@Component
public class S3EventListener {
	@Autowired
	AmazonS3Template amazonS3Template;
	@Subscribe
	public void feedEvent(MotionEvent event){
		
	}

}
