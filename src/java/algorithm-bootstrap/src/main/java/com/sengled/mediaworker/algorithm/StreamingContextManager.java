package com.sengled.mediaworker.algorithm;

import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

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
	
	private static final long CONTEXT_EXPIRE_TIME_MILLIS = 60 * 1000;
	
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
	
	public StreamingContext findOrCreateStreamingContext(ProcessorManager processor,String token, String model,Map<String, Object> modelConfig) throws AlgorithmIntanceCreateException{
		StreamingContext context =  streamingContextMap.get(token + "_" + model);
		if (context == null) {
			context =  newAlgorithmContext(processor,token,model, modelConfig);
		}else{
			context.getAlgorithm().setParameters(modelConfig);
		}
		return context;
	}
	public void reload(StreamingContext context) throws AlgorithmIntanceCloseException, AlgorithmIntanceCreateException{
		String token = context.getToken();
		String model = context.getModel();
		Algorithm algorithm = context.getAlgorithm();
		ProcessorManager processor = context.getProcessorManager();
		
		processor.close(context);
		String algorithmModelId = processor.newAlgorithmModel(token, model);
		algorithm.setAlgorithmModelId(algorithmModelId);
	}
	
	public void close(StreamingContext context) throws AlgorithmIntanceCloseException {
		ProcessorManager processor = context.getProcessorManager();
		processor.close(context);
		streamingContextMap.remove(context.getToken() + "_" + context.getModel());	
	}
	
	public StreamingContext newAlgorithmContext(ProcessorManager processor,String token, String model, Map<String, Object> newModelConfig) throws AlgorithmIntanceCreateException {
		LOGGER.info("Token:{},model:{},newAlgorithmContext...",token,model);
		String algorithmModelId = processor.newAlgorithmModel(token, model);
		LOGGER.info("Token:{},model:{},algorithmModelId:{}",token,model,algorithmModelId);
		Algorithm algorithm = new Algorithm(algorithmModelId, newModelConfig);
		StreamingContext context =  new StreamingContext(token, model, algorithm, processor,recordCounter,this);
		StreamingContext oldcontext = streamingContextMap.put(token +"_"+model, context);
		if( null != oldcontext){
			try {
				processor.close(oldcontext);
			} catch (AlgorithmIntanceCloseException e) {
				LOGGER.error("Token:{},close failed.",token);
				LOGGER.error(e.getMessage(),e);
			}
		}
		return context;
	}
	private void cleanExpiredContext() {
		LOGGER.info("CleanExpireContext. StreamingContextMap size:{}",streamingContextMap.size());
		for ( Entry<String, StreamingContext> entry : streamingContextMap.entrySet()) {
			StreamingContext context = entry.getValue();
			long currentTime = System.currentTimeMillis();
			Date  updateDate = context.getUpdateDate();
			LOGGER.info("Token:{},currentTime{},updateDate:{}",entry.getKey(),new Date(currentTime),updateDate);

			if((currentTime - updateDate.getTime() ) >= CONTEXT_EXPIRE_TIME_MILLIS){
				LOGGER.info("Token:{} Context expired clean...",context.getToken());
				try {
					close(context);
				} catch (AlgorithmIntanceCloseException e) {
					LOGGER.error(e.getMessage(),e);
				}
			}
		}
	}
}
