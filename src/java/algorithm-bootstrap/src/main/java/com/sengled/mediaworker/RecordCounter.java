package com.sengled.mediaworker;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;

@Component
public class RecordCounter implements InitializingBean{
    @Autowired
    private MetricRegistry metricRegistry;
    
	private final static String METRICS_NAME = "algorithm";
	private AtomicLong  recordCount = new AtomicLong();
    private AtomicLong  receiveDelayedCount = new AtomicLong();
    private AtomicLong  dataDelayedCount = new AtomicLong();
    
    
	@Override
	public void afterPropertiesSet() throws Exception {
        metricRegistry.register( MetricRegistry.name(METRICS_NAME, "recordCount"), new Gauge<Long>(){
            @Override
            public Long getValue() {
                return recordCount.getAndSet(0);
            }
        }); 
        metricRegistry.register( MetricRegistry.name(METRICS_NAME, "receiveDelayedCount"), new Gauge<Long>(){
            @Override
            public Long getValue() {
                return receiveDelayedCount.getAndSet(0);
            }
        }); 
        metricRegistry.register( MetricRegistry.name(METRICS_NAME, "dataDelayedCount"), new Gauge<Long>(){
            @Override
            public Long getValue() {
                return dataDelayedCount.getAndSet(0);
            }
        }); 
	}

	public AtomicLong getRecordCount() {
		return recordCount;
	}

	public AtomicLong getReceiveDelayedCount() {
		return receiveDelayedCount;
	}

	public AtomicLong getDataDelayedCount() {
		return dataDelayedCount;
	}
	
}
