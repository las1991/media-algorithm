package com.sengled.mediaworker;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SlidingWindowReservoir;
import com.sengled.mediaworker.metrics.custom.ServicesMetrics;

@Component
public class RecordCounter implements InitializingBean{
	private static final Logger LOGGER = LoggerFactory.getLogger(RecordProcessor.class);
	
	private final static String METRICS_NAME = "algorithm";
	private final static String OBJECT_METRICS_NAME = "object";
	private final static int HISTOGRAM_MAX_STORE = 2000;
	
    @Autowired
    private MetricRegistry metricRegistry;
    @Autowired
    private ServicesMetrics servicesMetrics;
	
	//接收kinesis records统计数
	private AtomicLong  recordCount = new AtomicLong();
	//接收records 中behindLatest > MAX_BEHINDLASTEST_MILLIS 计数
    private AtomicLong  receiveDelayedCount = new AtomicLong();
    //数据中utc时间 与接收时间差值 > MAX_DELAYED_TIME_MSCE 计数
    private AtomicLong  dataDelayedCount = new AtomicLong();
    
    private AtomicLong  s3FailureCount = new AtomicLong();
    private AtomicLong  s3SuccessfulCount = new AtomicLong();
    //private AtomicLong  dynamodbFailureCount = new AtomicLong();
    private AtomicLong  sqsFailureCount = new AtomicLong();
    private AtomicLong  sqsSuccessfulCount = new AtomicLong();
    //单数据处理时间
    private Histogram singleDataProcessCostHistogram;
    //单数据等待被处理的时间
    private Histogram waitProcessCostHistogram;
    //数据中的utc时间与当前时间差值 
    private Histogram receiveDelayHistogram;
    
    //以下为物体识别统计
    private AtomicLong  objectMotionCount = new AtomicLong();
    private AtomicLong  objectDataDelayedCount = new AtomicLong();
    private AtomicLong  objectErrorCount = new AtomicLong();
    private Histogram   objectReceiveDelayHistogram;
    private Histogram   objectSingleDataProcessCostHistogram;
    
    
    
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
	        metricRegistry.register( MetricRegistry.name(METRICS_NAME, "s3SuccessfulCount"), new Gauge<Long>(){
	            @Override
	            public Long getValue() {
	                return s3SuccessfulCount.getAndSet(0);
	            }
	        });
	        metricRegistry.register( MetricRegistry.name(METRICS_NAME, "sqsSuccessfulCount"), new Gauge<Long>(){
	            @Override
	            public Long getValue() {
	                return sqsSuccessfulCount.getAndSet(0);
	            }
	        });
	        
	        singleDataProcessCostHistogram = metricRegistry.register(MetricRegistry.name(METRICS_NAME, "processCost"),new Histogram(new SlidingWindowReservoir(HISTOGRAM_MAX_STORE)));
	        waitProcessCostHistogram = metricRegistry.register(MetricRegistry.name(METRICS_NAME, "waitProcessCost"),new Histogram(new SlidingWindowReservoir(HISTOGRAM_MAX_STORE)));
	        receiveDelayHistogram = metricRegistry.register(MetricRegistry.name(METRICS_NAME, "receiveDelay"),new Histogram(new SlidingWindowReservoir(HISTOGRAM_MAX_STORE)));
	        
	        //以下为物体识别
	        metricRegistry.register( MetricRegistry.name(OBJECT_METRICS_NAME, "objectMotionCount"), new Gauge<Long>(){
	            @Override
	            public Long getValue() {
	                return objectMotionCount.getAndSet(0);
	            }
	        });
	        metricRegistry.register( MetricRegistry.name(OBJECT_METRICS_NAME, "objectDataDelayedCount"), new Gauge<Long>(){
	            @Override
	            public Long getValue() {
	                return objectDataDelayedCount.getAndSet(0);
	            }
	        });
	        metricRegistry.register( MetricRegistry.name(OBJECT_METRICS_NAME, "objectErrorCount"), new Gauge<Long>(){
	            @Override
	            public Long getValue() {
	                return objectErrorCount.getAndSet(0);
	            }
	        });
	        objectReceiveDelayHistogram = metricRegistry.register(MetricRegistry.name(OBJECT_METRICS_NAME, "objectReceiveDelay"),new Histogram(new SlidingWindowReservoir(HISTOGRAM_MAX_STORE)));
	        objectSingleDataProcessCostHistogram = metricRegistry.register(MetricRegistry.name(OBJECT_METRICS_NAME, "objectSingleDataProcessCost"),new Histogram(new SlidingWindowReservoir(HISTOGRAM_MAX_STORE)));
	}
	public long addAndGetRecordCount(long delta) {
		servicesMetrics.mark(ServicesMetrics.RECEIVE, delta);
		return recordCount.addAndGet(delta);
	}
	
	public long addAndGetReceiveDelayedCount(long delta) {
		servicesMetrics.mark(ServicesMetrics.RECEIVE_DELAYED, delta);
		return receiveDelayedCount.addAndGet(delta);
	}
	
	public long addAndGetDataDelayedCount(long delta) {
		servicesMetrics.mark(ServicesMetrics.DATA_DELAYED, delta);
		return dataDelayedCount.addAndGet(delta);
	}
	
	public long addAndGetSqsFailureCount(long delta) {
		servicesMetrics.mark(ServicesMetrics.SQS_FAILURE, delta);
		return sqsFailureCount.addAndGet(delta);
	}
	
	public long addAndGetS3FailureCount(long delta) {
		servicesMetrics.mark(ServicesMetrics.S3_FAILURE, delta);
		return s3FailureCount.addAndGet(delta);
	}
	
	public long addAndGetSqsSuccessfulCount(long delta) {
		return sqsSuccessfulCount.addAndGet(delta);
	}
	
	public long addAndGetS3SuccessfulCount(long delta) {
		return s3SuccessfulCount.addAndGet(delta);
	}
	public void updateSingleDataProcessCost(long value){
		singleDataProcessCostHistogram.update(value);
	}
	public void updateWaitProcessCost(long value){
		waitProcessCostHistogram.update(value);
	}
	public void updateReceiveDelay(long value){
		receiveDelayHistogram.update(value);
	}
	
	
	//object 
	public void addAndGetObjectMotionCount(long value){
		objectMotionCount.addAndGet(value);
	}
	public void addAndGetObjectErrorCount(long value) {
		objectErrorCount.addAndGet(value);
	}
	public void addAndGetObjectDataDelayedCount(long value){
		objectDataDelayedCount.addAndGet(value);
	}
	public void updateObjectReceiveDelay(long value){
		objectReceiveDelayHistogram.update(value);
	}
	public void updateObjectSingleDataProcessCost(long value){
		objectSingleDataProcessCostHistogram.update(value);
	}

	
}
