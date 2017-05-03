package com.sengled.mediaworker;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;

@Component
public class RecordCounter implements InitializingBean{
	private static final Logger LOGGER = LoggerFactory.getLogger(RecordProcessor.class);
	
    @Autowired
    private MetricRegistry metricRegistry;
    
	private final static String METRICS_NAME = "algorithm";
	//接收kinesis records统计数
	private AtomicLong  recordCount = new AtomicLong();
	//接收records 中behindLatest > MAX_BEHINDLASTEST_MILLIS 计数
    private AtomicLong  receiveDelayedCount = new AtomicLong();
    //数据中utc时间 与接收时间差值 > MAX_DELAYED_TIME_MSCE 计数
    private AtomicLong  dataDelayedCount = new AtomicLong();
    
    private AtomicLong  s3FailureCount = new AtomicLong();
    //private AtomicLong  dynamodbFailureCount = new AtomicLong();
    private AtomicLong  sqsFailureCount = new AtomicLong();
   
	@Override
	public void afterPropertiesSet() throws Exception {
		LOGGER.info("Initializing...");
		try {
			initialize();
		} catch (Exception e) {
			LOGGER.error(e.getMessage(),e);
			System.exit(1);
		}		
	}
	private void initialize(){
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
	        
	        metricRegistry.register( MetricRegistry.name(METRICS_NAME, "s3FailureCount"), new Gauge<Long>(){
	            @Override
	            public Long getValue() {
	                return s3FailureCount.getAndSet(0);
	            }
	        });
	        metricRegistry.register( MetricRegistry.name(METRICS_NAME, "sqsFailureCount"), new Gauge<Long>(){
	            @Override
	            public Long getValue() {
	                return sqsFailureCount.getAndSet(0);
	            }
	        });
	}
	public long addAndGetRecordCount(long delta) {
		return recordCount.addAndGet(delta);
	}
	
	public long addAndGetReceiveDelayedCount(long delta) {
		return receiveDelayedCount.addAndGet(delta);
	}
	
	public long addAndGetDataDelayedCount(long delta) {
		return dataDelayedCount.addAndGet(delta);
	}
	
	public long addAndGetSqsFailureCount(long delta) {
		return sqsFailureCount.addAndGet(delta);
	}
	
	public long addAndGetS3FailureCount(long delta) {
		return s3FailureCount.addAndGet(delta);
	}
	public String getAllJsonData(){
		JSONObject json = new JSONObject();
		json.put("recordCount", recordCount.get());
		json.put("receiveDelayedCount", receiveDelayedCount.get());
		json.put("dataDelayedCount", dataDelayedCount.get());
		json.put("s3FailureCount", s3FailureCount.get());
		json.put("sqsFailureCount", sqsFailureCount.get());
		return json.toJSONString();
	}
}
