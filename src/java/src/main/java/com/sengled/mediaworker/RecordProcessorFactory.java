package com.sengled.mediaworker;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.google.common.eventbus.AsyncEventBus;
import com.sengled.mediaworker.algorithm.Constants;
import com.sengled.mediaworker.algorithm.FeedListener;
import com.sengled.mediaworker.algorithm.ProcessorManager;
import com.sengled.mediaworker.algorithm.service.DynamodbEventListener;
 

/**
 * kinesis stream record 处理器工厂类
 * @author liwei
 * @Date   2017年3月2日 下午3:27:32 
 * @Desc
 */
@Component
public class RecordProcessorFactory implements IRecordProcessorFactory {
	private static final Logger LOGGER = LoggerFactory.getLogger(RecordProcessorFactory.class);
	
    private final static String METRICS_NAME = "algorithm";
    
    
    @Autowired
    private MetricRegistry metricRegistry;
    
    @Autowired
    FeedListener feedListener;
    
    private ProcessorManager processorManager;
    
    @Autowired
    private DynamodbEventListener  dynamodbEventListener;
    
    
    private ExecutorService executor;
    private AtomicLong recordCount = new AtomicLong();
    
    
    public void setProcessorManager(ProcessorManager processorManager){
        this.processorManager = processorManager;
        this.metricRegistry.register( MetricRegistry.name(METRICS_NAME, "recordCount"), new Gauge<Long>(){
            @Override
            public Long getValue() {
                return recordCount.getAndSet(0);
            }
        });
    }
    /**
     * Constructor.
     */
    public RecordProcessorFactory() {
        super();    
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IRecordProcessor createProcessor() {
    	LOGGER.info("Create RecordProcessor...");//会根据分片创建多个RecordProcessor
        AsyncEventBus eventBus = feedListener.getEventBus();
        eventBus.register(dynamodbEventListener);
        
        executor = new ThreadPoolExecutor(Constants.CPU_CORE_COUNT * 2,
														  Constants.CPU_CORE_COUNT * 2,
														  60, TimeUnit.SECONDS,
														  new ArrayBlockingQueue<Runnable>(Constants.CPU_CORE_COUNT * 2),
														  new ThreadPoolExecutor.CallerRunsPolicy());
    	return  new RecordProcessor(executor,processorManager,recordCount,feedListener);
    }
    public void shutdown(){
    	try {
    		executor.shutdown();
			executor.awaitTermination(50, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			LOGGER.error(e.getMessage(),e);
		}
    }
}
