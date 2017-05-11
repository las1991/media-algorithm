package com.sengled.mediaworker;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.stereotype.Component;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;

@Component
public class ServicesMetrics implements InitializingBean {
	private final String sqsFailureCount = "sqs.failure.count";
	private final String s3FailureCount = "s3.failure.count";
	private final String dataDelayedCount = "data.delayed.count";
	private final String receiveDelayedCount = "receive.delayed.count";
	private final String receiveCount = "receive.count";
	@Autowired
	private CounterService counter;
    @Autowired
    private MetricRegistry metricRegistry;
	@Override
	public void afterPropertiesSet() throws Exception {
		metricRegistry.register(sqsFailureCount, new Counter());
		metricRegistry.register(s3FailureCount, new Counter());
		metricRegistry.register(dataDelayedCount, new Counter());
		metricRegistry.register(receiveDelayedCount, new Counter());
		metricRegistry.register(receiveCount, new Counter());
	}

	public void incrementSqsFailure(long num){
		while(--num >=0)
			counter.increment(sqsFailureCount);
	}
	
	public void incrementS3Failure(long num){
		while(--num >=0)
			counter.increment(s3FailureCount);
	}
	
	public void incrementDataDelayed(long num){
		while(--num >=0)
			counter.increment(dataDelayedCount);
	}
	
	public void incrementReceiveDelayed(long num){
		while(--num >=0)
			counter.increment(receiveDelayedCount);
	}

	public void incrementReceiveCount(long num){
		while(--num >=0)
			counter.increment(receiveCount);
	}
}
