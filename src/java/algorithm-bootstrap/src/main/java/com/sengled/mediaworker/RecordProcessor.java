package com.sengled.mediaworker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ThrottlingException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.types.InitializationInput;
import com.amazonaws.services.kinesis.clientlibrary.types.ProcessRecordsInput;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownInput;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.sengled.mediaworker.algorithm.ProcessorManager;

/**
 * kinesis stream record 处理器
 * 
 * @author liwei
 * @Date 2017年3月2日 下午3:28:28
 * @Desc
 */
public class RecordProcessor implements IRecordProcessor {
	private static final Logger LOGGER = LoggerFactory.getLogger(RecordProcessor.class);
	private String kinesisShardId;
	private RecordCounter recordCounter;
	private ProcessorManager processorManager;
	private Future<?> future;
	private ExecutorService executorService;

    // Checkpointing interval
    private static final long CHECKPOINT_INTERVAL_MILLIS = 1 * 60 * 1000; // 1 minute
    //max BehindLatest
    private static final long MAX_BEHINDLASTEST_MILLIS = 10 * 1000; // 10 sec
    //max execute time
    private static final long MAX_EXECUTE_MILLIS = 20 * 1000;//20 sec
    private long nextCheckpointTimeInMillis;
    private boolean isShutdown;
    
	public RecordProcessor(RecordCounter recordCounter,
							ProcessorManager processorManager) {
		this.recordCounter = recordCounter;
		this.processorManager = processorManager;
		this.executorService = Executors.newSingleThreadExecutor();
	}

	//提交一批数据，并等待执行结果返回
	private  void submitTask(List<Record> records,long receiveTime) {
		final Multimap<String, byte[]> dataMap = ArrayListMultimap.create();
		long startTime = System.currentTimeMillis();
		for (Record record : records) {
	    	int remaining =  record.getData().remaining();
			if ( remaining <= 0) {
				LOGGER.warn("record data size is null. skip...");
				continue;
			}
			byte[] data = new byte[remaining];
			record.getData().get(data);
			dataMap.put(getToken(record.getPartitionKey()), data);
		}
		LOGGER.debug("Multimap dataMap size:{}",dataMap.size());
		
		List<Future<?>> batchTasks = new ArrayList<>(dataMap.size());	
		for (final String token : dataMap.keySet()) {
			batchTasks.add(processorManager.submit(receiveTime,token, dataMap.get(token)));
		}
		for (Future<?> task : batchTasks) {
			try {
				task.get(MAX_EXECUTE_MILLIS, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				LOGGER.error(e.getMessage(), e);
			} catch (ExecutionException e) {
				LOGGER.error(e.getCause().getMessage(), e.getCause());
			} catch (TimeoutException e) {
				LOGGER.error(e.getMessage(), e);
			}
		}
		LOGGER.info("Process Records size:{} finished. Cost:{} ms",records.size(),(System.currentTimeMillis() - startTime));
	}

    private void checkpoint(IRecordProcessorCheckpointer checkpointer) {
        LOGGER.info("Checkpointing shard " + kinesisShardId);
        try {
            checkpointer.checkpoint();
        } catch (ShutdownException se) {
            // Ignore checkpoint if the processor instance has been shutdown (fail over).
        	LOGGER.info("Caught shutdown exception, skipping checkpoint.", se);
        } catch (ThrottlingException e) {
            // Skip checkpoint when throttled. In practice, consider a backoff and retry policy.
        	LOGGER.error("Caught throttling exception, skipping checkpoint.", e);
        } catch (InvalidStateException e) {
            // This indicates an issue with the DynamoDB table (check for table, provisioned IOPS).
        	LOGGER.error("Cannot save checkpoint to the DynamoDB table used by the Amazon Kinesis Client Library.", e);
        }
    }
    private void shutdownNow(){
    	LOGGER.info("RecordProcessor executorService shutdown now. for shard: {}",kinesisShardId);
    	isShutdown = true;
    	executorService.shutdownNow();
    }
    private String getToken(String partitionKey){
    	String token =  partitionKey.split(",")[0];
    	if(StringUtils.isNotBlank(token)){
    		return token;
    	}
    	return partitionKey;
    }

	@Override
	public void initialize(InitializationInput initializationInput) {
		LOGGER.info("Initializing...");
		String shardId = initializationInput.getShardId();
		this.kinesisShardId = shardId;
		nextCheckpointTimeInMillis = System.currentTimeMillis() + CHECKPOINT_INTERVAL_MILLIS;
		LOGGER.info("Initializing record processor for shard:{} checkpoint interval:{} ms",shardId,CHECKPOINT_INTERVAL_MILLIS);
	}

	@Override
	public void processRecords(ProcessRecordsInput processRecordsInput) {
		List<Record> records = processRecordsInput.getRecords();
		IRecordProcessorCheckpointer checkpointer = processRecordsInput.getCheckpointer();
		Long behindLatest = processRecordsInput.getMillisBehindLatest();
		long receiveTime = System.currentTimeMillis();
		LOGGER.info("Received records size:{}",records.size());
		LOGGER.info("BehindLatest:{}",behindLatest);
		
		recordCounter.addAndGetRecordCount(records.size());
		
		if(behindLatest > MAX_BEHINDLASTEST_MILLIS){
			recordCounter.addAndGetReceiveDelayedCount(records.size());
			LOGGER.warn("BehindLatest:{} > MAX_RECEIVE_DELAYED_MILLIS:{} skip.",behindLatest,MAX_BEHINDLASTEST_MILLIS);
			return;
		}
		
		long startTime = System.currentTimeMillis();
        boolean isSubmited = false;
        //如果上次提交的任务已执行完成，则提交新任务，否则等待
		while( ! isShutdown){
			if((future == null) || future.isDone() || future.isCancelled()){
				future = executorService.submit(new Runnable() {
					@Override
					public void run() {
						submitTask(records,receiveTime);
					}
				});
				LOGGER.debug("Submited records size:{}",records.size());
				isSubmited = true;
				break;
			}else{
				LOGGER.info("Wait submit. Sleep . Had been waiting for {} msec",(System.currentTimeMillis() - startTime));
				try {
					future.get(1, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					LOGGER.error(e.getMessage(),e);
 				} catch (ExecutionException e) {
 					LOGGER.error(e.getMessage(),e);
 				} catch (TimeoutException e) {
 					LOGGER.warn("Wait submit sleep 1 sce");
 				}
 
			}
		}
 
		if(isSubmited){
	        //Checkpoint once every checkpoint interval
	        if (System.currentTimeMillis() > nextCheckpointTimeInMillis) {
	            checkpoint(checkpointer);
	            nextCheckpointTimeInMillis = System.currentTimeMillis() + CHECKPOINT_INTERVAL_MILLIS;
	        }
		}
	}

	@Override
	public void shutdown(ShutdownInput shutdownInput) {
		IRecordProcessorCheckpointer checkpointer = shutdownInput.getCheckpointer();
		ShutdownReason reason = shutdownInput.getShutdownReason();
		LOGGER.info("Shutting down record processor for shard: {}. Reason:{}",kinesisShardId,reason);
		if(reason.equals(ShutdownReason.TERMINATE)){
			LOGGER.info("Shard:{} state SHARD_END. shutdown RecordProcessor",kinesisShardId);
			checkpoint(checkpointer);
		}
		shutdownNow();
	}
}
