package com.sengled.media.worker;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;

/**
 * kinesis stream record 处理器工长类
 * @author liwei
 * @Date   2017年3月2日 下午3:27:32 
 * @Desc
 */
@Component
public class RecordProcessorFactory implements IRecordProcessorFactory {

    private final static String METRICS_NAME = "screenshot";
    
    private ProcessorManager processorManager;
    @Autowired
    private MetricRegistry metricRegistry;
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
        return new RecordProcessor(processorManager,recordCount);
    }

}
