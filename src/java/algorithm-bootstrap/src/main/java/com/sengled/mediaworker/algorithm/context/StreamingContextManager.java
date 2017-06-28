package com.sengled.mediaworker.algorithm.context;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.sengled.media.interfaces.Algorithm;
import com.sengled.media.interfaces.exceptions.AlgorithmIntanceCloseException;
import com.sengled.media.interfaces.exceptions.AlgorithmIntanceCreateException;
import com.sengled.mediaworker.RecordCounter;
import com.sengled.mediaworker.algorithm.ProcessorManager;
import com.sengled.mediaworker.algorithm.decode.KinesisFrameDecoder.FrameConfig;
import com.sengled.mediaworker.algorithm.decode.KinesisFrameDecoder.MotionConfig;

@Component
public class StreamingContextManager implements InitializingBean{
	private static final Logger LOGGER = LoggerFactory.getLogger(StreamingContextManager.class);
	
	private static final long CONTEXT_EXPIRE_TIME_MILLIS = 10 * 60 * 1000;
	
	private ConcurrentHashMap<String, StreamingContext> streamingContextMap;
	private Timer timer;
	
	@Autowired
    private RecordCounter recordCounter;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		LOGGER.info("Initializing...");
		try {
			initialize();
		} catch (Exception e) {
			LOGGER.error(e.getMessage(),e);
			System.exit(1);
		}	
	}
	
	private void initialize(){
		streamingContextMap = new ConcurrentHashMap<>();
		timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				try {
					LOGGER.info("streamingContextMap size:{}",streamingContextMap.size());
					cleanExpiredContext();
				} catch (Exception e) {
					LOGGER.error(e.getMessage(),e);
				}
			}
		}, 10 * 60 * 1000, CONTEXT_EXPIRE_TIME_MILLIS);
	}
	
	public StreamingContext findOrCreateStreamingContext(ProcessorManager processor,String token,String utcDateTime,FrameConfig config) throws AlgorithmIntanceCreateException{
		StreamingContext context =  streamingContextMap.get(token);
		MotionConfig modelConfig = config.getBaseConfig();
		if (context == null) {
			context =  newAlgorithmContext(processor,token,utcDateTime, modelConfig);
		}else{
			//设置  数据中的UTC时间
			context.setUtcDateTime(utcDateTime);
			//设置算法参数
			context.getAlgorithm().setParameters(JSONObject.toJSONString(config.getBaseConfig()));
			//设置 上次接收到数据的时间
			context.setLastTimeContextUpdateTimestamp(context.getContextUpdateTimestamp());
			//设置 本次接收到数据的时间
			context.setContextUpdateTimestamp(System.currentTimeMillis());
		}
		context.setConfig(config);
		return context;
	}
	public void reload(StreamingContext context) throws AlgorithmIntanceCloseException, AlgorithmIntanceCreateException{
		if(LOGGER.isDebugEnabled()){
			LOGGER.debug("StreamingContext reload.{}",context.toString());
		}
		String token = context.getToken();
		Algorithm algorithm = context.getAlgorithm();
		ProcessorManager processor = context.getProcessorManager();
		
		processor.close(algorithm.getAlgorithmModelId());
		String algorithmModelId = processor.newAlgorithmModel(token);
		algorithm.setAlgorithmModelId(algorithmModelId);
	}
	
	public void close(StreamingContext context) throws AlgorithmIntanceCloseException {
		if(LOGGER.isDebugEnabled()){
			LOGGER.debug("StreamingContext close.{}",context.toString());
		}
		ProcessorManager processor = context.getProcessorManager();
		Algorithm algorithm = context.getAlgorithm();
		processor.close(algorithm.getAlgorithmModelId());
		streamingContextMap.remove(context.getToken());	
	}
	
	public StreamingContext newAlgorithmContext(ProcessorManager processor,String token,String utcDateTime, MotionConfig newModelConfig) throws AlgorithmIntanceCreateException {
		String algorithmModelId = processor.newAlgorithmModel(token);
		Algorithm algorithm = new Algorithm(algorithmModelId, JSONObject.toJSONString(newModelConfig));
		StreamingContext context =  new StreamingContext(token, utcDateTime,algorithm, processor,recordCounter,this);
		streamingContextMap.put(token, context);
		return context;
	}
	private void cleanExpiredContext() {
		LOGGER.info("CleanExpireContext. StreamingContextMap size:{}",streamingContextMap.size());
		for ( Entry<String, StreamingContext> entry : streamingContextMap.entrySet()) {
			StreamingContext context = entry.getValue();
			long currentTime = System.currentTimeMillis();
			long  updateTimestamp = context.getContextUpdateTimestamp();
			LOGGER.info("Token:{},currentTime{},updateDate:{}",entry.getKey(),new Date(currentTime),new Date(updateTimestamp));

			if((currentTime - updateTimestamp ) >= CONTEXT_EXPIRE_TIME_MILLIS){
				LOGGER.info("Token:{} Context expired clean...",context.getToken());
				if(LOGGER.isDebugEnabled()){
					LOGGER.debug("cleanExpiredContext StreamingContext:{}",context.toString());
				}
				try {
					close(context);
				} catch (AlgorithmIntanceCloseException e) {
					LOGGER.error(e.getMessage(),e);
				}
			}
		}
	}
	public List<String> getToken(){
		Enumeration<String> keys = streamingContextMap.keys();
		List<String> tokens = new ArrayList<>();
		while(keys.hasMoreElements()){
			tokens.add(keys.nextElement());
		}
		return tokens;
	}
}