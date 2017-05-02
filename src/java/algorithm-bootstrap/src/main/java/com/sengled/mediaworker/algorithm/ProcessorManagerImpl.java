package com.sengled.mediaworker.algorithm;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
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
	private static final Logger LOGGER = LoggerFactory.getLogger(ProcessorManagerImpl.class);
	
	private static final List<String> MODEL_LIST = Arrays.asList("motion");
	
	private JnaInterface jnaInterface;
	private ExecutorService  threadPool;
	private FeedListener feedListener;
	@Autowired
    private StreamingContextManager streamingContextManager;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		jnaInterface = new JnaInterface();
		threadPool = Executors.newWorkStealingPool();		
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
	public void setFeedListener(FeedListenerImpl feedListener) {
		this.feedListener = feedListener;
	}
	
	private  void handleDatas(final String token,final Collection<byte[]> datas){
		LOGGER.debug("Token:{},handleDatas begin. datas size:{}",token,datas.size());
		for (byte[] data : datas) {
			final Frame frame;
			try {
				frame = KinesisFrameDecoder.decode(data);
				LOGGER.debug("Token:{},Frame Config:{}",token,frame.getConfigs());
			} catch (FrameDecodeException e) {
				LOGGER.error("Token:{},KinesisFrameDecoder falied.",token);
				LOGGER.error(e.getMessage(),e);
				continue;
			}
			actionHandle(token, frame.getConfigs(), frame.getNalData());
		}
	}
	
	private void actionHandle(String token,Map<String, Object> config, final byte[] nalData) {
		if( !verifiyConfig(token,config)){
			LOGGER.error("Token:{} verifiyConfig failed. config:{}",token,config);
			return;
		}
		for (String model : MODEL_LIST) {
			if (config.containsKey(model)) {
				Map<String, Object> modelConfig = (Map<String, Object>) config.get(model);
				StreamingContext context;
				try {
					context = streamingContextManager.findOrCreateStreamingContext(this, token, model, modelConfig);
				} catch (Exception e) {
					LOGGER.error("findOrCreateStreamingContext failed."+e.getMessage(),e);
					LOGGER.error("Token:{} model:{} skip.",token,model);
					continue;
				}
				String utcDateTime = (String) config.get("utcDateTime");
				String action = (String) config.get("action");
				//保存上次UTC时间
				context.setLastTimeUpdateDate(context.getUpdateDate());
				//更新本次UTC时间
				context.setUtcDateTime(utcDateTime);
				//更新时间
				context.setUpdateDate(new Date());
				
				try {
					if(context.isDataExpire()){
						continue;
					}
					if(context.motionIntervalCheck()){
						continue;
					}
				} catch (Exception e2) {
					LOGGER.error("Token:{}  skip...",token);
					LOGGER.error(e2.getMessage(),e2);
					continue;
				}
				YUVImage yuvImage;
				try {
					yuvImage = decode(token, nalData);
				} catch (Exception e1) {
					LOGGER.error("Token:{} decode failed. skip...",token);
					LOGGER.error(e1.getMessage(),e1);
					continue;
				}
				
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
						LOGGER.error("Token:{},action:{} not supported", token,action);
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
	
	private boolean verifiyConfig(String token,Map<String, Object> config){
		LOGGER.debug("Token:{},verifiyConfig ...{}",token,config);
		boolean  verifiyResult = 
				null != config &&
				! config.isEmpty() &&
				config.containsKey("utcDateTime") &&
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
	@Override
	public void shutdown(){
		threadPool.shutdownNow();
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
