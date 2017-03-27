package com.sengled.mediaworker;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
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
import com.sengled.mediaworker.algorithm.Constants;
import com.sengled.mediaworker.algorithm.FeedListener;
import com.sengled.mediaworker.algorithm.ProcessorManager;
import com.sengled.mediaworker.algorithm.StreamingContext;
import com.sengled.mediaworker.algorithm.decode.KinesisFrameDecoder;
import com.sengled.mediaworker.algorithm.decode.KinesisFrameDecoder.Frame;
import com.sengled.mediaworker.algorithm.pydto.YUVImage;

/**
 * kinesis stream record 处理器
 * 
 * @author liwei
 * @Date 2017年3月2日 下午3:28:28
 * @Desc
 */
public class RecordProcessor implements IRecordProcessor {
	private static final Logger LOGGER = LoggerFactory.getLogger(RecordProcessor.class);
	private static final long CHECKPOINT_INTERVAL_MILLIS = 60000L; // 1 minute
	private static final List<String> MODEL_LIST = Arrays.asList("motion");
	private String kinesisShardId;
	private ProcessorManager processorManager;
	private Map<String, StreamingContext> contextMap;
	private FeedListener feedListener;

	private long nextCheckpointTimeInMillis;
	private AtomicLong recordCount;
    private ExecutorService handleThread;
    
	
	public RecordProcessor(ExecutorService executor,
						   ProcessorManager processorManager, 
			               AtomicLong recordCount, 
						   FeedListener feedListener) {
		this.processorManager = processorManager;
		this.recordCount = recordCount;
		this.feedListener = feedListener;
		this.handleThread = executor;
		this.contextMap = new HashMap<String, StreamingContext>();
	}


	@Override
	public void initialize(String shardId) {
		LOGGER.info("Initializing record processor for shard: " + shardId);
		this.kinesisShardId = shardId;
		this.nextCheckpointTimeInMillis = CHECKPOINT_INTERVAL_MILLIS;
	}

	@Override
	public void processRecords(List<Record> records, IRecordProcessorCheckpointer checkpointer) {
		LOGGER.info("received records...{}",records.size());
		recordCount.addAndGet(records.size());
		if(handleThread.isShutdown()){
			try {
				LOGGER.warn("handleThread isShutdown true. wait...");
				checkpoint(checkpointer);
				Thread.currentThread().wait();
			} catch (InterruptedException e) {
				LOGGER.debug(e.getMessage(),e);
			}
		}
		for (Record record : records) {
			processRecord(record);
		}
		if (System.currentTimeMillis() > nextCheckpointTimeInMillis) {
			checkpoint(checkpointer);
			nextCheckpointTimeInMillis = System.currentTimeMillis() + CHECKPOINT_INTERVAL_MILLIS;
		}
	}

	private void processRecord(final Record record) {
		ByteBuffer buffer = record.getData();
		if (buffer.remaining() <= 0) {
			LOGGER.error("record data size is null.PartitionKey:" + record.getPartitionKey());
			return;
		}
		final byte[] data = new byte[buffer.remaining()];
		buffer.get(data);
		if(data.length <=0 ){
			LOGGER.warn("Record d ata is null");
			return;
		}
		handleThread.submit(new Runnable() {
			@Override
			public void run() {
				try {
					pushData(record.getPartitionKey(), data);
				}catch (InterruptedException e1){
					LOGGER.error("InterruptedException");
				} catch (Exception e) {
					LOGGER.error("pushData "+e.getMessage(),e);
				}
			}
		});
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void shutdown(IRecordProcessorCheckpointer checkpointer, ShutdownReason reason) {
		LOGGER.info("Shutting down record processor for shard: " + kinesisShardId);
		if (reason == ShutdownReason.TERMINATE) {
			checkpoint(checkpointer);
		}
	}

	private void checkpoint(IRecordProcessorCheckpointer checkpointer) {
		LOGGER.info("Checkpointing shard " + kinesisShardId);
		try {
			checkpointer.checkpoint();
		} catch (ShutdownException se) {
			LOGGER.info("Caught shutdown exception, skipping checkpoint.", se);
		} catch (ThrottlingException e) {
			LOGGER.error("Caught throttling exception, skipping checkpoint.", e);
		} catch (InvalidStateException e) {
			LOGGER.error("Cannot save checkpoint to the DynamoDB table used by the Amazon Kinesis Client Library.", e);
		}
	}

	private void pushData(String token, byte[] data) throws Exception {
		LOGGER.info("Receive record...");
		Frame frame = KinesisFrameDecoder.decode(data);

		Map<String, Object> params = frame.getConfigs();
		if(params == null || params.size() <=0){
			throw new Exception("Frame params is null");
		}
		byte[] imageData = frame.getData();
		YUVImage image = processorManager.decode(token,imageData);
		
		for(String model : MODEL_LIST){
			String key =  token + "_" + model;//key format e.g: TOKEN_motion
			if (params.containsKey(model)) {
				StreamingContext context = contextMap.get(key);
				if (context == null) {
					context = processorManager.newAlgorithmContext(model, token, (Map<String,Object>)params.get(model));
					contextMap.put(key, context);
				}
				
				String utcDate = (String)params.get("utcDateTime");
				String action  = (String)params.get("action");
				
				context.setUtcDate(utcDate);
				switch(action){
					case "open":
						context.setAction(context.openAction);
						break;
					case "exec":
						context.setAction(context.execAction);
						break;
					case "close":
						context.setAction(context.closeAction);
						contextMap.remove(key);
						break;
					default :
						LOGGER.error("action:{} not supported",action);
						continue;
				}
				
				context.feed(image, feedListener);
			}
		}
	}
}
