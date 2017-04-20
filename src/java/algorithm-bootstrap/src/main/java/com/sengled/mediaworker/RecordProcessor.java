package com.sengled.mediaworker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ThrottlingException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
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
	private AtomicLong recordCount;
	private ProcessorManager processorManager;
	private Future<?> future;
	private ExecutorService executorService;

    // Checkpointing interval
    private static final long CHECKPOINT_INTERVAL_MILLIS = 60000L; // 1 minute
    private long nextCheckpointTimeInMillis;
    
	public RecordProcessor(AtomicLong recordCount,
							ProcessorManager processorManager) {
		this.recordCount = recordCount;
		this.processorManager = processorManager;
		this.executorService = Executors.newSingleThreadExecutor();
	}

	@Override
	public void initialize(String shardId) {
		LOGGER.info("Initializing record processor for shard: " + shardId);
		nextCheckpointTimeInMillis = System.currentTimeMillis() + CHECKPOINT_INTERVAL_MILLIS;
		this.kinesisShardId = shardId;
	}

	@Override
	public void processRecords(List<Record> records, IRecordProcessorCheckpointer checkpointer) {
		LOGGER.info("received records...{}",records.size());
		recordCount.addAndGet(records.size());	
		long startTime = System.currentTimeMillis();
		
        boolean isDown = false;
 
		while(true){
			if((future == null) || future.isDone() || future.isCancelled()){
				future = executorService.submit(new Runnable() {
					@Override
					public void run() {
						submitTask(records);
					}
				});
				LOGGER.debug("submited records size:{}",records.size());
				isDown = true;
				break;
			}else{
				LOGGER.info("wait submit sleep 1 sec...Had been waiting for {} sec",(System.currentTimeMillis() - startTime)/1000);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					LOGGER.error(e.getMessage(),e);
				}
			}
		}
 
		if(isDown){
	        // Checkpoint once every checkpoint interval
	        if (System.currentTimeMillis() > nextCheckpointTimeInMillis) {
	            checkpoint(checkpointer);
	            nextCheckpointTimeInMillis = System.currentTimeMillis() + CHECKPOINT_INTERVAL_MILLIS;
	        }
		}

	}
	private synchronized void submitTask(List<Record> records) {
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
			dataMap.put(record.getPartitionKey(), data);
		}
		LOGGER.debug("Multimap dataMap size:{}",dataMap.size());
		
		List<Future<?>> batchTasks = new ArrayList<>(dataMap.size());	
		for (final String token : dataMap.keySet()) {
			batchTasks.add(processorManager.submit(token, dataMap.get(token)));
		}
		for (Future<?> task : batchTasks) {
			try {
				task.get(20, TimeUnit.SECONDS);
			} catch (Exception e) {
				LOGGER.error(e.getMessage(), e);
			}
		}
		LOGGER.info("ProcessRecords size:{} finished. Cost:{}",records.size(),(System.currentTimeMillis() - startTime));
	}

	@Override
	public void shutdown(IRecordProcessorCheckpointer checkpointer, ShutdownReason reason) {
		LOGGER.info("Shutting down record processor for shard: " + kinesisShardId);
		if(reason.equals(ShutdownReason.TERMINATE)){
			checkpoint(checkpointer);
		}

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
}
