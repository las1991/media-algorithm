package com.sengled.mediaworker;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.codahale.metrics.MetricRegistry;
import com.sengled.mediaworker.algorithm.ProcessorManager;
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
    
    @Value("${AWS_KINESIS_KEY}")
    private String accessKey;
    
    @Value("${AWS_KINESIS_SECRET}")
    private String secretKey;
    
    @Value("${AWS_KINESIS_REGION}")
    private String region;
    

    @Autowired
    private RecordProcessorFactory recordProcessorFactory;
//    @Autowired
//    private MetricRegistry metricRegistry;
    
    private ProcessorManager processorManager;
    
    @PostConstruct
    public void init(){
        processorManager = new ProcessorManager();
        recordProcessorFactory.setProcessorManager(processorManager);//设置python进程管理线程池
    }
    
    @Override
    public IRecordProcessorFactory getRecordProcessorFactory() {
        return recordProcessorFactory;
    }

    @Override
    public String getStreamName() {
        return streamName;
    }
    
    @Override
    public BasicAWSCredentials getBasicAWSCredentials() {
        return new BasicAWSCredentials(accessKey, secretKey);
    }
    
    @Override
    public String getRegion() {
        return region;
    }
    
    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        super.onApplicationEvent(event);
        if(event instanceof ContextClosedEvent ){
        	LOGGER.info("Shutdown all recordProcessorFactory thread");
        	recordProcessorFactory.shutdown();
            LOGGER.info("Shutdown all python process");
            processorManager.destroyAll();
        }
    }
}
