package com.sengled.mediaworker.kinesis;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessorFactory;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker;
import com.amazonaws.services.kinesis.metrics.interfaces.MetricsLevel;

public abstract class AbsKinesisStreamProcessor implements ApplicationListener<ApplicationEvent>{
    private static final Logger LOGGER = LoggerFactory.getLogger(AbsKinesisStreamProcessor.class);
    public static final int SOCKET_TIMEOUT = 30 * 1000;
    public static final int REQUEST_TIMEOUT = 20 * 1000;
    public static final int CLIENT_EXECUTION_TIMEOUT = 70 * 1000;
    public static final int CONNECTION_TIMEOUT = 10 * 1000;
    
    public abstract String getStreamName();
    public abstract String getWorkerIdPrefix();
    public abstract String getRegion();
    public abstract BasicAWSCredentials getBasicAWSCredentials();
    public abstract IRecordProcessorFactory getRecordProcessorFactory();
    
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    
    private Worker worker;

    public  ClientConfiguration initConfig(){
    	return new ClientConfiguration()
        		.withConnectionTimeout(CONNECTION_TIMEOUT)
        		.withClientExecutionTimeout(CLIENT_EXECUTION_TIMEOUT)
        		.withRequestTimeout(REQUEST_TIMEOUT)
        		.withSocketTimeout(SOCKET_TIMEOUT)
        		.withTcpKeepAlive(true)
        		.withMaxErrorRetry(3)
        		.withMaxConnections(150)
        		.withConnectionTTL(5 * 60 * 1000)
        		.withProtocol(Protocol.HTTPS)
        		.withGzip(false);
    }    
    
    public KinesisClientLibConfiguration createKclConfig(){
        AWSCredentialsProvider provider = DefaultAWSCredentialsProviderChain.getInstance();
        String streamName = getStreamName();
        String workerId = String.valueOf(getWorkerIdPrefix() + "_" + UUID.randomUUID());
        String applicationName = "amazon-kinesis-"+getStreamName();
        LOGGER.info("streamName:{},applicationName:{}",streamName,applicationName);
        return new KinesisClientLibConfiguration(applicationName, streamName, provider, workerId)
                .withRegionName(getRegion())
                .withInitialPositionInStream(InitialPositionInStream.LATEST)
                .withKinesisClientConfig(initConfig())
                .withMaxRecords(1000)
                .withMetricsLevel(MetricsLevel.SUMMARY);
    }
    public void start() {
    	final Worker worker = new Worker.Builder()
    			.recordProcessorFactory(getRecordProcessorFactory())
    			.config(createKclConfig())
    			.build();
        executor.submit(worker);
        this.worker = worker;
    }
    
    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if(event instanceof ApplicationReadyEvent){
            LOGGER.info("Start kinesisStream Processos");
            start();
        }
        if(event instanceof ContextClosedEvent ){
            LOGGER.info("Shutdown kinesisStream Processos");
            if(worker !=null ){
                worker.shutdown();  
            }
            executor.shutdown();
            try {
				executor.awaitTermination(5, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				LOGGER.error(e.getMessage(),e);
			}
            LOGGER.info("KinesisStream Worker shutdown finished.");
        }
    }
}
