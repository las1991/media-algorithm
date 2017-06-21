package com.sengled.mediaworker.algorithm.service;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.Subscribe;
import com.sengled.mediaworker.RecordCounter;
import com.sengled.mediaworker.algorithm.ProcessorManager;
import com.sengled.mediaworker.algorithm.event.ObjectEvent;
import com.sengled.mediaworker.s3.AmazonS3Template;
import com.sengled.mediaworker.sqs.SQSTemplate;

@Component
public class ObjectEventListener implements InitializingBean {
	private static final Logger LOGGER = LoggerFactory.getLogger(ObjectEventListener.class);
	private static final int THREAD_MAXCOUNT = 150;

	@Value("${AWS_SERVICE_NAME_PREFIX}_${sqs.algorithm.result.queue}")
	private String queue;
	@Value("${aws_screenshot_bucket}")
	private String bucketName;

	@Autowired
	private SQSTemplate sqsTemplate;
	@Autowired
	private AmazonS3Template amazonS3Template;
	@Autowired
	ProcessorManager  processorManagerImpl;
	
	private ThreadPoolExecutor pool;

	@Override
	public void afterPropertiesSet() throws Exception {
		ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(THREAD_MAXCOUNT * 10);
    	pool = new ThreadPoolExecutor(20
										,THREAD_MAXCOUNT
										,60L,TimeUnit.SECONDS
										,queue
										,new ThreadPoolExecutor.CallerRunsPolicy());
	}
	

	@Subscribe
	public void feedEvent(ObjectEvent event) {
		pool.submit(new Runnable() {
			@Override
			public void run() {
				handle(event);
			}
		});
	}

	private void handle(ObjectEvent event) {
		LOGGER.info("Handle ObjectEvent");
		/*根据
		*	1.识别结果：图片中所有的物体及位置
		*	2.Motion结果：zone中移动的坐标
		*结果：zone 中在移动的物体名
		*
		*/
		//encode
		//pushSqs
		//pushS3
	}
	
}
