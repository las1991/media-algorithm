package com.sengled.mediaworker;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.sengled.mediaworker.algorithm.FeedListener;
import com.sengled.mediaworker.algorithm.ProcessorManager;
import com.sengled.mediaworker.algorithm.StreamingContext;
import com.sengled.mediaworker.algorithm.decode.KinesisFrameDecoder;
import com.sengled.mediaworker.algorithm.decode.KinesisFrameDecoder.Frame;
import com.sengled.mediaworker.algorithm.exception.FrameDecodeException;
import com.sengled.mediaworker.algorithm.pydto.YUVImage;

/**
 * kinesis stream record 处理器
 * 
 * @author liwei
 * @Date 2017年3月2日 下午3:28:28
 * @Desc
 */
public class ForkJoinRecordProcessor implements IRecordProcessor {
	private static final Logger LOGGER = LoggerFactory.getLogger(ForkJoinRecordProcessor.class);
	private static final List<String> MODEL_LIST = Arrays.asList("motion");
	private String kinesisShardId;
	private ProcessorManager processorManager;
	private FeedListener feedListener;
	private ExecutorService handleThread;

	private AtomicLong recordCount;

	public ForkJoinRecordProcessor(ExecutorService executor, ProcessorManager processorManager, AtomicLong recordCount,
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
		LOGGER.info("received records...{}", records.size());
		recordCount.addAndGet(records.size());

		final Multimap<String, Record> recordMap = ArrayListMultimap.create();

		for (Record record : records) {
			recordMap.put(record.getPartitionKey(), record);
		}
		List<Future<Void>> batchTasks = new ArrayList<>(recordMap.size());
		
		for (final String token : recordMap.keySet()) {
			batchTasks.add(handleThread.submit(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					handlingRecords(token, recordMap.get(token));// 顺序执行同token的一组消息
					return null;
				}
			}));
		}
		for (Future<Void> task : batchTasks) {
			try {
				task.get(30, TimeUnit.SECONDS);
			} catch (Exception e) {
				LOGGER.error(e.getMessage(), e);
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

	/**
	 *  顺序处理同token的一组消息
	 * @param token
	 * @param records
	 * @throws Exception
	 */
	private void handlingRecords(final String token, final Collection<Record> records) throws Exception {
		final List<Future<YUVFrame>> decodeTasks = new ArrayList<>(records.size());
		// 解码任务
		for (Record record : records) {
			ByteBuffer buffer = record.getData();
			final Frame frame = KinesisFrameDecoder.decode(buffer);
			Future<YUVFrame> task = handleThread.submit(new Callable<YUVFrame>() {
				@Override
				public YUVFrame call() throws Exception {
					Future<YUVImage> imageFuture = processorManager.decode(token, frame.getData());
					YUVImage yumImage = imageFuture.get();
					return new YUVFrame(frame.getConfigs(), yumImage);
				}
			});
			decodeTasks.add(task);
		}
		//feed
		for (Future<YUVFrame> task : decodeTasks) {
			YUVFrame frame;
			try {
				frame = task.get();
				Map<String, Object> params = frame.getConfigs();
				YUVImage yUVImage = frame.getyUVImage();
				handle(token, params, yUVImage);
			} catch (Exception e) {
				LOGGER.error("decode failed. "+e.getMessage(),e);
				LOGGER.error("handle exception skip ... token:{}",token);
				continue;
			}
		}
	}

	private void handle(final String token, final Map<String, Object> params, final YUVImage yUVImage) throws Exception {
		for (String model : MODEL_LIST) {
			if (params.containsKey(model)) {
				Map<String, Object> newModelConfig = (Map<String, Object>) params.get(model);
				StreamingContext context = processorManager.findStreamingContext(model, token);
				if (context == null) {
					context = processorManager.newAlgorithmContext(model, token, newModelConfig);
				}

				String utcDate = (String) params.get("utcDateTime");
				String action = (String) params.get("action");
				context.setUtcDate(utcDate);
				
				switch (action) {
				case "open":
					context.setAction(context.openAction);
					break;
				case "exec":
					context.setAction(context.execAction);
					break;
				case "close":
					context.setAction(context.closeAction);
					break;
				default:
					LOGGER.error("action:{} not supported", action);
					continue;
				}
				context.feed(yUVImage, feedListener);
			}
		}
	}

	public static class YUVFrame {

		private Map<String, Object> configs;
		private YUVImage yUVImage;

		public YUVFrame(Map<String, Object> configs, YUVImage yUVImage) {
			super();
			this.configs = configs;
			this.yUVImage = yUVImage;
		}

		public Map<String, Object> getConfigs() {
			return configs;
		}

		public void setConfigs(Map<String, Object> configs) {
			this.configs = configs;
		}

		public YUVImage getyUVImage() {
			return yUVImage;
		}

		public void setyUVImage(YUVImage yUVImage) {
			this.yUVImage = yUVImage;
		}
	}
}
