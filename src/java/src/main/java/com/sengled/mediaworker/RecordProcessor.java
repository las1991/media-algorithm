package com.sengled.mediaworker;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
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
	private static final List<String> MODEL_LIST = Arrays.asList("motion");
	private String kinesisShardId;
	private ProcessorManager processorManager;
	private FeedListener feedListener;
	private ExecutorService handleThread;
	
	private AtomicLong recordCount;
    
	
	public RecordProcessor(ExecutorService executor,
						   ProcessorManager processorManager, 
			               AtomicLong recordCount, 
						   FeedListener feedListener) {
		this.processorManager = processorManager;
		this.recordCount = recordCount;
		this.handleThread = executor;
		this.feedListener = feedListener;
	}


	@Override
	public void initialize(String shardId) {
		LOGGER.info("Initializing record processor for shard: " + shardId);
		this.kinesisShardId = shardId;
	}

	@Override
	public void processRecords(List<Record> records, IRecordProcessorCheckpointer checkpointer) {
		LOGGER.info("received records...{}",records.size());
		recordCount.addAndGet(records.size());		
		
		final Multimap<String, Record> RecordMap = ArrayListMultimap.create();
		for (Record record : records) {
			RecordMap.put(record.getPartitionKey(), record);
		}
		
		List<Future<?>> batchTasks =  new ArrayList<>(RecordMap.size());
		for(final String key : RecordMap.keySet()){
			batchTasks.add(handleThread.submit(new Runnable() {
				@Override
				public void run() {
					for(Record record : RecordMap.get(key)){
						ByteBuffer buffer = record.getData();
						try {
							Frame frame = KinesisFrameDecoder.decode(buffer);
							pushData(record.getPartitionKey(), frame);
						} catch (Exception e) {
							LOGGER.error(e.getMessage(),e);
							return;
						}
					}
				}
			}));
		}
		for(Future<?> task : batchTasks){
			try {
				task.get(3, TimeUnit.SECONDS);
			} catch (Exception e) {
				LOGGER.error(e.getMessage(),e);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void shutdown(IRecordProcessorCheckpointer checkpointer, ShutdownReason reason) {
		LOGGER.info("Shutting down record processor for shard: " + kinesisShardId);
	}
	
	private void pushData(final String token, final Frame frame) throws Exception {
		Map<String, Object> params = frame.getConfigs();
		if(params == null || params.size() <=0){
			throw new Exception("Frame params is null");
		}
		LOGGER.debug("Receive record...token:{},params:{}",token,frame.getConfigs());

		byte[] imageData = frame.getData();
		
		Future<YUVImage> image = processorManager.decode(token,imageData);

		for(String model : MODEL_LIST){
			if (params.containsKey(model)) {
				Map<String,Object> newModelConfig = (Map<String,Object>)params.get(model);
				StreamingContext context = processorManager.findStreamingContext( model,token);
				if(context == null){
					context = processorManager.newAlgorithmContext(model, token,newModelConfig);
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
						break;
					default :
						LOGGER.error("action:{} not supported",action);
						continue;
				}
				
				context.feed(image.get(), feedListener);
			}
		}
	}

}
