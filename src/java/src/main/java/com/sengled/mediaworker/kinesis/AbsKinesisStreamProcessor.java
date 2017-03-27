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
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker;


/**
 * KinesisStream Processor 
 * @author liwei
 * @Date 2017年2月28日 上午11:25:25
 * @Desc 
 */
public abstract class AbsKinesisStreamProcessor implements ApplicationListener<ApplicationEvent>{
    private static final Logger LOGGER = LoggerFactory.getLogger(AbsKinesisStreamProcessor.class);
    
    public abstract String getStreamName();
    public abstract String getRegion();
    public abstract BasicAWSCredentials getBasicAWSCredentials();
    public abstract IRecordProcessorFactory getRecordProcessorFactory();
    
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    
    private Worker worker;
    
    public  ClientConfiguration initConfig(){
        return new ClientConfiguration()
                .withGzip(false) // 本身时图片，不使用Gzip压缩
                .withTcpKeepAlive(true) // 使用长连接
                .withProtocol(Protocol.HTTPS) // 默认使用 HTTPS
                .withMaxConnections(50) // 默认 50
                .withConnectionTTL(5 * 60 * 1000) // 长连接 TTL 为 5min
                .withConnectionMaxIdleMillis(5 * 60 * 1000) // idle 为 5min
                .withConnectionTimeout(30 * 1000) //
                .withMaxErrorRetry(1)
                .withClientExecutionTimeout(30 * 1000)
                .withCacheResponseMetadata(false);
    }    
    
    public KinesisClientLibConfiguration createKclConfig(){
        BasicAWSCredentials credentials = getBasicAWSCredentials();
        StaticCredentialsProvider provider = new StaticCredentialsProvider(credentials);
        
        String streamName = getStreamName();
        String workerId = String.valueOf(UUID.randomUUID());
        String applicationName = "amazon-kinesis-"+getStreamName();
        LOGGER.info("streamName:{},applicationName:{}",streamName,applicationName);
        return new KinesisClientLibConfiguration(applicationName, streamName, provider, workerId)
                .withRegionName(getRegion())
                .withInitialPositionInStream(InitialPositionInStream.LATEST)
                .withKinesisClientConfig(initConfig());
//                .withIdleTimeBetweenReadsInMillis(1000);
    }
    public void start() {
        final Worker worker = new Worker(getRecordProcessorFactory(), createKclConfig());
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
            LOGGER.info("Shutdown 1 kinesisStream Processos");
            if(worker !=null ){
                worker.shutdown();  
            }
            LOGGER.info("Shutdown 2 kinesisStream Processos");
            executor.shutdown();
            try {
				executor.awaitTermination(50, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				LOGGER.error(e.getMessage(),e);
			}
            LOGGER.info("KinesisStream Worker shutdown finished.");
        }
    }
}
