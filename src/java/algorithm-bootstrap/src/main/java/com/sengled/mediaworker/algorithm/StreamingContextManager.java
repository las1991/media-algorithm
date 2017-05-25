package com.sengled.mediaworker.algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentHashMap.KeySetView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sengled.media.interfaces.Algorithm;
import com.sengled.media.interfaces.exceptions.AlgorithmIntanceCloseException;
import com.sengled.media.interfaces.exceptions.AlgorithmIntanceCreateException;
import com.sengled.mediaworker.RecordCounter;

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
				LOGGER.info("streamingContextMap size:{}",streamingContextMap.size());
			}
		}, 10000, 60 * 1000);
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				try {
					cleanExpiredContext();
				} catch (Exception e) {
					LOGGER.error(e.getMessage(),e);
				}
			}
		}, 60000, CONTEXT_EXPIRE_TIME_MILLIS);
	}
	
	public StreamingContext findOrCreateStreamingContext(ProcessorManager processor,String token, String model,String utcDateTime,Map<String, Object> modelConfig) throws AlgorithmIntanceCreateException{
		StreamingContext context =  streamingContextMap.get(token + "_" + model);
		if (context == null) {
			context =  newAlgorithmContext(processor,token,model,utcDateTime, modelConfig);
		}else{
			//设置  数据中的UTC时间
			context.setUtcDateTime(utcDateTime);
			//设置算法参数
			context.getAlgorithm().setParameters(modelConfig);
			//设置 上次接收到数据的时间
			context.setLastTimeContextUpdateTimestamp(context.getContextUpdateTimestamp());
			//设置 本次接收到数据的时间
			context.setContextUpdateTimestamp(System.currentTimeMillis());
		}
		return context;
	}
	public void reload(StreamingContext context) throws AlgorithmIntanceCloseException, AlgorithmIntanceCreateException{
		if(LOGGER.isDebugEnabled()){
			LOGGER.debug("StreamingContext reload.{}",context.toString());
		}
		String token = context.getToken();
		String model = context.getModel();
		Algorithm algorithm = context.getAlgorithm();
		ProcessorManager processor = context.getProcessorManager();
		
		processor.close(algorithm.getAlgorithmModelId());
		String algorithmModelId = processor.newAlgorithmModel(token, model);
		algorithm.setAlgorithmModelId(algorithmModelId);
	}
	
	public void close(StreamingContext context) throws AlgorithmIntanceCloseException {
		if(LOGGER.isDebugEnabled()){
			LOGGER.debug("StreamingContext close.{}",context.toString());
		}
		ProcessorManager processor = context.getProcessorManager();
		Algorithm algorithm = context.getAlgorithm();
		processor.close(algorithm.getAlgorithmModelId());
		streamingContextMap.remove(context.getToken() + "_" + context.getModel());	
	}
	
	public StreamingContext newAlgorithmContext(ProcessorManager processor,String token, String model,String utcDateTime, Map<String, Object> newModelConfig) throws AlgorithmIntanceCreateException {
		String algorithmModelId = processor.newAlgorithmModel(token, model);
		Algorithm algorithm = new Algorithm(algorithmModelId, newModelConfig);
		StreamingContext context =  new StreamingContext(token, model, utcDateTime,algorithm, processor,recordCounter,this);
		streamingContextMap.put(token +"_"+model, context);
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
