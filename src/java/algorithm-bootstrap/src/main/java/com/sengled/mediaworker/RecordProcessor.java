package com.sengled.mediaworker;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
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
import com.sengled.mediaworker.algorithm.decode.KinesisFrameDecoder;
import com.sengled.mediaworker.algorithm.decode.KinesisFrameDecoder.Frame;

/**
 * kinesis stream record 处理器
 * 
 * @author liwei
 * @Date 2017年3月2日 下午3:28:28
 * @Desc
 */
public class RecordProcessor implements IRecordProcessor {
	private static final Logger LOGGER = LoggerFactory.getLogger(RecordProcessor.class);
	private static final String[] UTC_DATE_FORMAT = new String[] { "yyyy-MM-dd HH:mm:ss.SSS" };
    // Checkpointing interval
    private static final long CHECKPOINT_INTERVAL_MILLIS = 1 * 60 * 1000; // 1 minute
    //max BehindLatest
    private static final long MAX_BEHINDLASTEST_MILLIS = 5 * 60 * 1000; // 5 minute
    //max execute time
    private static final long MAX_EXECUTE_MILLIS = 60 * 1000;//60 sec
    
	private String kinesisShardId;
	private RecordCounter recordCounter;
	private ProcessorManager processorManager;
	private Future<?> future;
	private ExecutorService executorService;
    private long nextCheckpointTimeInMillis;
    private boolean isShutdown;
    
	public RecordProcessor(RecordCounter recordCounter,
							ProcessorManager processorManager) {
		this.recordCounter = recordCounter;
		this.processorManager = processorManager;
		this.executorService = Executors.newSingleThreadExecutor();
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
		
		LOGGER.info("kinesisShardId:{},Received records size:{}",kinesisShardId,records.size());
		LOGGER.info("kinesisShardId:{},BehindLatest:{}",kinesisShardId,behindLatest);
		recordCounter.addAndGetRecordCount(records.size());

		if(behindLatest > MAX_BEHINDLASTEST_MILLIS){
			recordCounter.addAndGetReceiveDelayedCount(records.size());
			LOGGER.warn("kinesisShardId:{},BehindLatest:{} > MAX_RECEIVE_DELAYED_MILLIS:{} skip.",kinesisShardId,behindLatest,MAX_BEHINDLASTEST_MILLIS);
			return;
		}
		
		long startTime = System.currentTimeMillis();
        boolean isSubmited = false;
		
        //如果上次提交的任务已执行完成，则提交新任务，否则等待
		while( ! isShutdown){
			if((future == null) || future.isDone() || future.isCancelled()){
				final Multimap<String, Frame> dataMap = unpacking(records);
				future = executorService.submit(new Runnable() {
					@Override
					public void run() {
						submitTask(dataMap,receiveTime);
					}
				});
				LOGGER.debug("Submited records size:{}",records.size());
				isSubmited = true;
				break;
			}else{
				try {
					future.get(1, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					LOGGER.error(e.getMessage(),e);
 				} catch (ExecutionException e) {
 					LOGGER.error(e.getMessage(),e);
 				} catch (TimeoutException e) {
 					LOGGER.warn("Wait submit sleep 1 sce");
 				}
				LOGGER.info("Wait submit. Sleep . Had been waiting for {} msec",(System.currentTimeMillis() - startTime));
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
		threadShutdownNow();
	}
	//提交一批数据，并等待执行结果返回
	private  void submitTask(final Multimap<String, Frame> token2MultipleFrames,long receiveTime) {
		long startTime = System.currentTimeMillis();
		LOGGER.debug("Multimap token2MultipleFrames size:{}",token2MultipleFrames.size());
		
		List<Future<?>> batchTasks = new ArrayList<>(token2MultipleFrames.size());	
		for (final String tokenMask : token2MultipleFrames.keySet()) {//tokenMask : XXXX,{random}
			batchTasks.add(processorManager.submit(receiveTime,tokenMask, token2MultipleFrames.get(tokenMask)));
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
		LOGGER.info("Process Records  finished. Cost:{} ms",(System.currentTimeMillis() - startTime));
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

	private Multimap<String, Frame> unpacking(List<Record> records) {
		final Multimap<String, Frame> dataMap = ArrayListMultimap.create();
		long currentTime = System.currentTimeMillis();
		for (Record record : records) {
			String token = record.getPartitionKey();
	    	int remaining =  record.getData().remaining();
			if ( remaining <= 0) {
				LOGGER.warn("record data size is null. skip...");
				continue;
			}
			byte[] data = new byte[remaining];
			record.getData().get(data);
			
			
			final Frame frame;
			try {
				frame = KinesisFrameDecoder.decode(data);
				
				String utcDateTime = frame.getConfig().getUtcDateTime();
				Date utcDate = DateUtils.parseDate(utcDateTime, UTC_DATE_FORMAT);
				long delay = currentTime  - utcDate.getTime();
				recordCounter.updateReceiveDelay(delay);
				LOGGER.debug("Token:{},unpacking finished.Frame utcDelay:{} Config:{}",token,delay,frame.getConfig());
			} catch (Exception e) {
				LOGGER.error("Token:{},KinesisFrameDecoder falied.",token);
				LOGGER.error(e.getMessage(),e);
				continue;
			}
			dataMap.put(record.getPartitionKey(), frame);
		}
		LOGGER.info("unpacking token size:{}", dataMap.size());
		return dataMap;
	}
	
    private String getToken(String partitionKey){
    	String token =  partitionKey.split(",")[0];
    	if(StringUtils.isNotBlank(token)){
    		return token;
    	}
    	return partitionKey;
    }
    
    private void threadShutdownNow(){
    	LOGGER.info("RecordProcessor executorService shutdown now. for shard: {}",kinesisShardId);
    	isShutdown = true;
    	executorService.shutdownNow();
    }

}
