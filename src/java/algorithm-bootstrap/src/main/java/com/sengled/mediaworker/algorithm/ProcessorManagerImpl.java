package com.sengled.mediaworker.algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import com.sengled.media.interfaces.Algorithm;
import com.sengled.media.interfaces.JnaInterface;
import com.sengled.media.interfaces.YUVImage;
import com.sengled.media.interfaces.exceptions.AlgorithmIntanceCloseException;
import com.sengled.media.interfaces.exceptions.AlgorithmIntanceCreateException;
import com.sengled.media.interfaces.exceptions.DecodeException;
import com.sengled.media.interfaces.exceptions.EncodeException;
import com.sengled.media.interfaces.exceptions.FeedException;
import com.sengled.mediaworker.algorithm.decode.KinesisFrameDecoder;
import com.sengled.mediaworker.algorithm.decode.KinesisFrameDecoder.Frame;
import com.sengled.mediaworker.algorithm.exception.FrameDecodeException;

@Component
public class ProcessorManagerImpl implements InitializingBean,ProcessorManager{
	private static final List<String> MODEL_LIST = Arrays.asList("motion");
	private static final Logger LOGGER = LoggerFactory.getLogger(ProcessorManagerImpl.class);
	private JnaInterface jnaInterface;
	private ExecutorService  threadPool;
	private ExecutorService  decodethreadPool;
	private StreamingContextManager streamingContextManager;
	private FeedListener feedListener;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		jnaInterface = new JnaInterface();
		threadPool = Executors.newSingleThreadExecutor();
		decodethreadPool = Executors.newSingleThreadExecutor();
		streamingContextManager = new StreamingContextManager();
	}

	public Future<?> submit(String token, Collection<byte[]> datas) {
		return threadPool.submit(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				handleDatas(token, datas);
				return null;
			}
		});
	}
	@Override
	public void setFeedListener(FeedListener feedListener) {
		this.feedListener = feedListener;
	}
	
	private  void handleDatas(final String token,final Collection<byte[]> datas){
		LOGGER.debug("handleDatas start...datas size:{}",datas.size());
		final List<Future<YUVImageWrapper>> decodeTasks = new ArrayList<>(datas.size());
		for (byte[] data : datas) {
			final Frame frame;
			try {
				frame = KinesisFrameDecoder.decode(data);
			} catch (FrameDecodeException e) {
				LOGGER.error(e.getMessage(),e);
				continue;
			}
			 
			Future<YUVImageWrapper> task = decodethreadPool.submit(new Callable<YUVImageWrapper>() {
				@Override
				public YUVImageWrapper call() throws Exception {
					LOGGER.debug("decode start...");
					YUVImage yuvImage = decode(token, frame.getData());
					return new YUVImageWrapper(frame.getConfigs(), yuvImage);
				}
			});
			decodeTasks.add(task);
		}
		
		for (Future<YUVImageWrapper> decodeTask : decodeTasks) {
			try {
				LOGGER.debug("wait decode task...");
				YUVImageWrapper yuvImageWrapper = decodeTask.get();
				Map<String, Object> config = yuvImageWrapper.getConfigs();
				YUVImage image = yuvImageWrapper.getYuvImage();
				actionHandle(token,config,image);
			} catch (Exception e) {
				LOGGER.error(e.getMessage(),e);
				continue;
			} 
		}
	}
	
	private void actionHandle(String token,Map<String, Object> config, final YUVImage yuvImage) {
		verifiyConfig(config);
		for (String model : MODEL_LIST) {
			if (config.containsKey(model)) {
				Map<String, Object> modelConfig = (Map<String, Object>) config.get(model);
				StreamingContext context;
				try {
					context = streamingContextManager.findOrCreateStreamingContext(this, token, model, modelConfig);
				} catch (Exception e) {
					LOGGER.error("findOrCreateStreamingContext failed."+e.getMessage(),e);
					LOGGER.error("skip token:{} model:{}",token,model);
					continue;
				}
 
				String utcDate = (String) config.get("utcDateTime");
				String action = (String) config.get("action");
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
				try {
					context.feed(yuvImage, feedListener);
				} catch (Exception e) {
					LOGGER.error(e.getMessage(),e);
					continue;
				}
				
			}
		}
	}

	private YUVImage decode(final String token,final byte[] nalData) throws DecodeException{
		return jnaInterface.decode(token, nalData);
	}
	
	private boolean verifiyConfig(Map<String, Object> config){
		LOGGER.debug("verifiyConfig ...{}",config);
		boolean  verifiyResult = 
				null != config ||
				! config.isEmpty() ||
				config.containsKey("utcDateTime") ||
				config.containsKey("action");
		
		return verifiyResult;
	}
	
	public String newAlgorithmModel(String token,String model) throws AlgorithmIntanceCreateException{
		return jnaInterface.newAlgorithmModel(model, token);
	}
	@Override
	public String feed(Algorithm algorithm, YUVImage yuvImage) throws FeedException {
		return jnaInterface.feed(algorithm.getParametersJson(), algorithm.getAlgorithmModelId(), yuvImage);
	}

	@Override
	public byte[] encode(String token,byte[] yuvData,int width,int  height,int  dstWidth,int  dstHeight ) throws EncodeException{
		return jnaInterface.encode(token, width, height, dstWidth, dstHeight, yuvData);
	}
	@Override
	public void close(StreamingContext context) throws AlgorithmIntanceCloseException {
		Algorithm algorithm = context.getAlgorithm();
		jnaInterface.close(algorithm.getAlgorithmModelId());
		
	}
	
	public static class YUVImageWrapper {
		private Map<String, Object> configs;
		private YUVImage yuvImage;

		public YUVImageWrapper(Map<String, Object> configs, YUVImage yuvImage) {
			super();
			this.configs = configs;
			this.yuvImage = yuvImage;
		}

		public Map<String, Object> getConfigs() {
			return configs;
		}

		public void setConfigs(Map<String, Object> configs) {
			this.configs = configs;
		}

		public YUVImage getYuvImage() {
			return yuvImage;
		}

		public void setYuvImage(YUVImage yuvImage) {
			this.yuvImage = yuvImage;
		}
	}

}
