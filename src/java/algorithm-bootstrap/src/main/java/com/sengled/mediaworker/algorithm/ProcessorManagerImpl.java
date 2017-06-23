package com.sengled.mediaworker.algorithm;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sengled.media.interfaces.Algorithm;
import com.sengled.media.interfaces.JnaInterface;
import com.sengled.media.interfaces.YUVImage;
import com.sengled.media.interfaces.exceptions.AlgorithmIntanceCloseException;
import com.sengled.media.interfaces.exceptions.AlgorithmIntanceCreateException;
import com.sengled.media.interfaces.exceptions.DecodeException;
import com.sengled.media.interfaces.exceptions.EncodeException;
import com.sengled.media.interfaces.exceptions.FeedException;
import com.sengled.mediaworker.RecordCounter;
import com.sengled.mediaworker.algorithm.context.StreamingContext;
import com.sengled.mediaworker.algorithm.context.StreamingContextManager;
import com.sengled.mediaworker.algorithm.decode.KinesisFrameDecoder.Frame;
import com.sengled.mediaworker.algorithm.decode.KinesisFrameDecoder.FrameConfig;
import com.sengled.mediaworker.algorithm.feedlistener.FeedListener;

@Component
public class ProcessorManagerImpl implements InitializingBean,ProcessorManager{
	private static final Logger LOGGER = LoggerFactory.getLogger(ProcessorManagerImpl.class);
	
	private static final List<String> MODEL_LIST = Arrays.asList("motion");
	
	//Motion间隔时间
    @Value("${motion.interval.time.msce:15000}")
    private Long motionIntervalTimeMsce;
	//包最大延时
    @Value("${max.delayed.time.msce:10000}")
    private Long maxDelayedTimeMsce;
	
	private JnaInterface jnaInterface;
	private ExecutorService  threadPool;
	@Autowired
	private FeedListener[] feedListeners;
	@Autowired
    private StreamingContextManager streamingContextManager;
	@Autowired
	private RecordCounter recordCounter;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		try {
			jnaInterface = new JnaInterface();
			threadPool = Executors.newWorkStealingPool(Math.max(1, Runtime.getRuntime().availableProcessors() - 1));
		} catch (Exception e) {
			LOGGER.error(e.getMessage(),e);
			System.exit(1);
		}		
	}

	public synchronized Future<?> submit(long receiveTime,String token, Collection<Frame> datas) {
		return threadPool.submit(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				handleDatas(receiveTime,token, datas);
				return null;
			}
		});
	}
	
	private  void handleDatas(final long receiveTime,final String token,final Collection<Frame> datas){
		
		LOGGER.debug("Token:{},handleDatas begin. datas size:{} ",token,datas.size());
		
		for (Frame frame : datas) {
			long handleStartTime = System.currentTimeMillis();
			long waitProcessCost = handleStartTime - receiveTime;
			recordCounter.updateWaitProcessCost(waitProcessCost);
			LOGGER.debug("Token:{},waitProcessCost:{}",token,waitProcessCost);
			
			actionHandle(token, frame.getConfig(), frame.getNalData());
			
			long processCost = System.currentTimeMillis() -  handleStartTime;
			recordCounter.updateSingleDataProcessCost(processCost);
			LOGGER.debug("Token:{} process cost:{}",token,processCost);
		}
	}
	
	private void actionHandle(String token,FrameConfig config, final byte[] nalData) {
		
		for (String model : MODEL_LIST) {
			if (config.getMotionConfig() != null) {
				//Map<String, Object> modelConfig = (Map<String, Object>) config.get(model);
				String utcDateTime = config.getUtcDateTime();
				String action = config.getAction();
				LOGGER.debug("Token:{},Received config.[ action:{},utcDateTime:{},modelConfig:{} ]",token,action,utcDateTime,config);
				
				//获得上下文
				StreamingContext context;
				try {
					context = streamingContextManager.findOrCreateStreamingContext(this, token, model, utcDateTime,config);
					context.setNalData(nalData);
				} catch (Exception e) {
					LOGGER.error("findOrCreateStreamingContext failed."+e.getMessage(),e);
					LOGGER.error("Token:{} model:{} skip.",token,model);
					continue;
				}
				//过滤数据
				try {
//					if(context.isDataExpire(maxDelayedTimeMsce)){
//						continue;
//					}
					context.reportCheck(motionIntervalTimeMsce);
//					if(context.motionIntervalCheck(motionIntervalTimeMsce)){
//						continue;
//					}
				} catch (Exception e2) {
					LOGGER.error("Token:{}  skip...",token);
					LOGGER.error(e2.getMessage(),e2);
					continue;
				}				
				//处理
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
					context.feed(feedListeners);
				} catch (Exception e) {
					LOGGER.error(e.getMessage(),e);
					continue;
				}
			}
		}
	}
	@Override
	public YUVImage decode(final String token,final byte[] nalData) throws DecodeException{
		return jnaInterface.decode(token, nalData);
	}
	
	public String newAlgorithmModel(String token,String model) throws AlgorithmIntanceCreateException{
		return jnaInterface.newAlgorithmModel(token,model);
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
	public void close(String algorithmModelId) throws AlgorithmIntanceCloseException {
		jnaInterface.close(algorithmModelId);
		
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
