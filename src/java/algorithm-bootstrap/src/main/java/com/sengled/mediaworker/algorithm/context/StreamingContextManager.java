package com.sengled.mediaworker.algorithm.context;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import com.alibaba.fastjson.JSONObject;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sengled.media.algorithm.MediaAlgorithmService;
import com.sengled.media.algorithm.QueryAlgorithmConfigRequest;
import com.sengled.media.algorithm.config.AlgorithmConfig;
import com.sengled.media.interfaces.Algorithm;
import com.sengled.media.interfaces.exceptions.AlgorithmIntanceCloseException;
import com.sengled.media.interfaces.exceptions.AlgorithmIntanceCreateException;
import com.sengled.mediaworker.RecordCounter;
import com.sengled.mediaworker.algorithm.ProcessorManager;
import com.sengled.mediaworker.algorithm.decode.KinesisFrameDecoder.FrameConfig;

@Component
public class StreamingContextManager{
	private static final Logger LOGGER = LoggerFactory.getLogger(StreamingContextManager.class);
	
	private static final long CONTEXT_EXPIRE_TIME_MILLIS = 10 * 60 * 1000;
	
    @Value("${motion.sensitivity.high}")
    private int motionHigh;
    
    @Value("${motion.sensitivity.normal}")
    private int motionNormal;
    
    @Value("${motion.sensitivity.low}")
    private int motionLow;
    
	private ConcurrentHashMap<String, StreamingContext> streamingContextMap = new ConcurrentHashMap<>();
	
	private Object lockObject  = new Object();;
	
	@Autowired
    private RecordCounter recordCounter;
	
	@Autowired
	MediaAlgorithmService mediaAlgorithmService;
	
    @Scheduled(initialDelay=10 * 60 * 1000,fixedRate = CONTEXT_EXPIRE_TIME_MILLIS)
    private void cleanExpiredContext() {
        LOGGER.info("CleanExpireContext. StreamingContextMap size:{}",streamingContextMap.size());
        for ( Entry<String, StreamingContext> entry : streamingContextMap.entrySet()) {
            StreamingContext context = entry.getValue();
            long currentTime = System.currentTimeMillis();
            long  updateTimestamp = context.getContextUpdateTimestamp();
            LOGGER.info("TokenMask:{},currentTime{},updateDate:{}",entry.getKey(),DateFormatUtils.format(currentTime, Context.UTC_DATE_FORMAT[0]),DateFormatUtils.format(updateTimestamp,Context.UTC_DATE_FORMAT[0]));

            if((currentTime - updateTimestamp ) >= CONTEXT_EXPIRE_TIME_MILLIS){
                LOGGER.info("TokenMask:{} Context expired clean...",context.getTokenMask());
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
	
	public StreamingContext findOrCreateStreamingContext(ProcessorManager processor,String tokenMask,String utcDateTime,FrameConfig frameConfig) throws Exception{
	    StreamingContext  context = null;
		String action = frameConfig.getAction();
		synchronized (lockObject) {
		    context = streamingContextMap.get(tokenMask);
		    
		    if( "open".equalsIgnoreCase(action) ){
	            if( null != context ){
	                close(context);
	            }
	            context =  newAlgorithmContext(processor,tokenMask,utcDateTime);
	            context.setAction(context.openAction);
	        }else if( "exec".equalsIgnoreCase(action) ){
	            if( null == context ){
	                context =  newAlgorithmContext(processor,tokenMask,utcDateTime);
	            }
                context.setAction(context.execAction);
	        }else if( "close".equalsIgnoreCase(action) ){
	            if( null != context ){
	                close(context);
	                context = null;
	            }
	        }
		}
		
		if( null != context ){
	        //设置  数据中的UTC时间
	        context.setUtcDateTime(utcDateTime);
	        //设置 上次接收到数据的时间
	        context.setLastTimeContextUpdateTimestamp(context.getContextUpdateTimestamp());
	        //设置 本次接收到数据的时间
	        context.setContextUpdateTimestamp(System.currentTimeMillis());
	        
	        context.setFrameConfig(frameConfig);
		}
		return context;
	}
	private AlgorithmConfigWarpper getAlgorithmConfig(String tokenMask) throws Exception{
        QueryAlgorithmConfigRequest request = new QueryAlgorithmConfigRequest();
        request.setToken(StringUtils.split(tokenMask, ",")[0]);
        AlgorithmConfig algor = mediaAlgorithmService.getAlgorithmConfig(request);
        LOGGER.info("Mediabase return AlgorithmConfig:{}, tokenMask:{}",algor, tokenMask);
        Assert.notNull(algor,"getAlgorithmConfig return null");
        AlgorithmConfigWarpper configWapper = new AlgorithmConfigWarpper(algor,motionHigh,motionNormal,motionLow);
        LOGGER.info("AlgorithmConfigWarpper:{} tokenMask:{}",configWapper , tokenMask);
        return configWapper;
    }

    public void reload(StreamingContext context) throws AlgorithmIntanceCloseException, AlgorithmIntanceCreateException{
		if(LOGGER.isDebugEnabled()){
			LOGGER.debug("StreamingContext reload.{}",context.toString());
		}
		String token = context.getTokenMask();
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
		streamingContextMap.remove(context.getTokenMask());	
	}
	
	public StreamingContext newAlgorithmContext(ProcessorManager processor,String tokenMask,String utcDateTime) throws Exception {
	    AlgorithmConfigWarpper algorithmConfig = getAlgorithmConfig(tokenMask);
		String algorithmModelId = processor.newAlgorithmModel(tokenMask);
		Algorithm algorithm = new Algorithm(algorithmModelId, JSONObject.toJSONString(algorithmConfig.getBaseConfig()));
		StreamingContext context =  new StreamingContext(tokenMask, utcDateTime,algorithm, processor,recordCounter,algorithmConfig,this);
		streamingContextMap.put(tokenMask, context);
		return context;
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
