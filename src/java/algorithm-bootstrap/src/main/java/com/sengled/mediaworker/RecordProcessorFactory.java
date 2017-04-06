package com.sengled.mediaworker;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.sengled.mediaworker.algorithm.FeedListener;
import com.sengled.mediaworker.algorithm.ProcessorManager;

/**
 * kinesis stream record 处理器工厂类
 * @author liwei
 * @Date   2017年3月2日 下午3:27:32 
 * @Desc
 */
@Component
public class RecordProcessorFactory implements IRecordProcessorFactory,InitializingBean{
	private static final Logger LOGGER = LoggerFactory.getLogger(RecordProcessorFactory.class);
    private final static String METRICS_NAME = "algorithm";
    
    @Autowired
    private MetricRegistry metricRegistry;
    @Autowired
    FeedListener feedListener;
    @Autowired
    private ProcessorManager processorManager;
    
    private ExecutorService executor;
    private AtomicLong recordCount;
    

	@Override
	public void afterPropertiesSet() throws Exception {
    	LOGGER.info("RecordProcessorFactory afterPropertiesSet...");
        executor = Executors.newWorkStealingPool();
		recordCount = new AtomicLong();
        metricRegistry.register( MetricRegistry.name(METRICS_NAME, "recordCount"), new Gauge<Long>(){
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
    	return  new ForkJoinRecordProcessor(executor,processorManager,recordCount,feedListener);
    }
    public void shutdown(){
    	try {
    		executor.shutdown();
    		processorManager.stop();
		} catch (Exception e) {
			LOGGER.error(e.getMessage(),e);
		}
    }



}