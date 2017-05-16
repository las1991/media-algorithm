package com.sengled.mediaworker.metrics.custom;

import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
public class ServicesMetrics implements InitializingBean {
	private static final Logger LOGGER = LoggerFactory.getLogger(ServicesMetrics.class);
	
	private final ConcurrentHashMap<String, LastMinuteCount> minuteCountMap = new ConcurrentHashMap<String, LastMinuteCount>();
	
	public final static String RECEIVE = "algorithm.receive.lastMinute";
	public final static String SQS_FAILURE = "algorithm.s3Failure.lastMinute";
	public final static String S3_FAILURE = "algorithm.sqsFailure.lastMinute";
	public final static String DATA_DELAYED = "algorithm.dataDelayed.lastMinute";
	public final static String RECEIVE_DELAYED = "algorithm.receiveDelayed.lastMinute";
	
	
	@Override
	public void afterPropertiesSet() throws Exception {
		add();
	}
	
	private void add(){
		minuteCountMap.put(RECEIVE, new LastMinuteCount());
		minuteCountMap.put(SQS_FAILURE, new LastMinuteCount());
		minuteCountMap.put(S3_FAILURE, new LastMinuteCount());
		minuteCountMap.put(DATA_DELAYED, new LastMinuteCount());
		minuteCountMap.put(RECEIVE_DELAYED, new LastMinuteCount());
	}
	public void mark(String key,long num){
		LastMinuteCount mc = minuteCountMap.get(key);
		if(mc == null){
			LOGGER.error("minuteCountMap hasnot key:{}",key);
			return;
		}
		mc.markCount(num);
	}
	public long getValue(String key){
		LastMinuteCount mc = minuteCountMap.get(key);
		return mc.getPreMinuteCount();
	}

}
