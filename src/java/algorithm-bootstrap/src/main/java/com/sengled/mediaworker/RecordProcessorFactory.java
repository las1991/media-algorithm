package com.sengled.mediaworker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessorFactory;
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

    @Autowired
    FeedListener feedListener;
    @Autowired
    private ProcessorManager processorManager;
    @Autowired
    private RecordCounter recordCounter;
    

	@Override
	public void afterPropertiesSet() throws Exception {
    	LOGGER.info("RecordProcessorFactory afterPropertiesSet...");
		processorManager.setFeedListener(feedListener);
	}
	
    /**
     * Constructor.
     */
    public RecordProcessorFactory() {
        super();    

    }

    @Override
    public IRecordProcessor createProcessor() {
    	LOGGER.info("Create RecordProcessor...");//会根据分片创建多个RecordProcessor
    	return  new RecordProcessor(recordCounter,processorManager);
    }
    public void shutdown(){
    	LOGGER.info("processorManager shutdown.");
    	processorManager.shutdown();
    }
}
