package com.sengled.mediaworker.algorithm;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sengled.media.interfaces.Algorithm;
import com.sengled.media.interfaces.exceptions.AlgorithmIntanceCloseException;
import com.sengled.media.interfaces.exceptions.AlgorithmIntanceCreateException;

public class StreamingContextManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(StreamingContextManager.class);
	private ConcurrentHashMap<String, StreamingContext> streamingContextMap = new ConcurrentHashMap<>();

	private Timer timer = new Timer();
	
	public StreamingContextManager(){
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				LOGGER.info("streamingContextMap size:{}",streamingContextMap.size());
			}
		}, 10000, 5000);
	}
	
	public StreamingContext findOrCreateStreamingContext(ProcessorManager processor,String token, String model,Map<String, Object> modelConfig) throws AlgorithmIntanceCreateException{
		StreamingContext context =  streamingContextMap.get(token + "_" + model);
		if (context == null) {
			context =  newAlgorithmContext(processor,token,model, modelConfig);
		}
		return context;
	}
	public StreamingContext reload(StreamingContext context) throws AlgorithmIntanceCloseException, AlgorithmIntanceCreateException{
		String token = context.getToken();
		String model = context.getModel();
		Algorithm algorithm = context.getAlgorithm();
		ProcessorManager processor = context.getProcessorManager();
		
		processor.close(context);
		streamingContextMap.remove(token +"_"+model);
		
		StreamingContext newContext = findOrCreateStreamingContext(processor,token,model, algorithm.getParameters());
		streamingContextMap.put(token +"_"+model, newContext);
		return newContext;
	}
	
	public void close(StreamingContext context) throws AlgorithmIntanceCloseException {
		ProcessorManager processor = context.getProcessorManager();
		processor.close(context);
		streamingContextMap.remove(context.getToken() + "_" + context.getModel());	
	}
	
	public StreamingContext newAlgorithmContext(ProcessorManager processor,String token, String model, Map<String, Object> newModelConfig) throws AlgorithmIntanceCreateException {
		String algorithmModelId = processor.newAlgorithmModel(token, model);
		Algorithm algorithm = new Algorithm(algorithmModelId, newModelConfig);
		StreamingContext context =  new StreamingContext(token, model, algorithm, processor,this);
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
}
