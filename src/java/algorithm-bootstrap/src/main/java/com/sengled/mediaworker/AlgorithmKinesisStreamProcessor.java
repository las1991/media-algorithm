package com.sengled.mediaworker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ContextClosedEvent;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessorFactory;
import com.sengled.mediaworker.kinesis.AbsKinesisStreamProcessor;
/**
 * ScreenShot KinesisStream Processor
 * @author liwei
 * @Date   2017年2月28日 上午11:41:25 
 * @Desc
 */
@SpringBootApplication
public class AlgorithmKinesisStreamProcessor  extends AbsKinesisStreamProcessor{
   
    private static final Logger LOGGER = LoggerFactory.getLogger(AlgorithmKinesisStreamProcessor.class);
    
    @Value("${AWS_SERVICE_NAME_PREFIX}_${aws_kinesis_stream_algorithm}")
    private String streamName;
    
    @Value("${AWS_KINESIS_REGION}")
    private String region;
    
    @Value("${PRIVATE_IPV4}")
    private String privateIp;
    
    @Autowired
    private RecordProcessorFactory recordProcessorFactory;

    @Override
    public IRecordProcessorFactory getRecordProcessorFactory() {
        return recordProcessorFactory;
    }

    @Override
    public String getStreamName() {
        return streamName;
    }
    
	@Override
	public String getWorkerIdPrefix() {
		return privateIp;
	}
	
    @Override
    public String getRegion() {
        return region;
    }
    
    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        super.onApplicationEvent(event);
        if(event instanceof ContextClosedEvent ){
        	LOGGER.info("RecordProcessorFactory shutdown");
        	recordProcessorFactory.shutdown();
        }
    }

    
    @Value("${connectionTimeout}")
    private int connectionTimeout;
    
    @Value("${clientExecutionTimeout}")
    private int clientExecutionTimeout;
    
    @Value("${requestTimeout}")
    private int requestTimeout;
    
    @Value("${socketTimeout}")
    private int socketTimeout;
    
	@Override
	public ClientConfiguration initConfig() {
		ClientConfiguration config = super.initConfig();
		return config
        		.withConnectionTimeout(connectionTimeout)
        		.withClientExecutionTimeout(clientExecutionTimeout)
        		.withRequestTimeout(requestTimeout)
        		.withSocketTimeout(socketTimeout);
	}
}
