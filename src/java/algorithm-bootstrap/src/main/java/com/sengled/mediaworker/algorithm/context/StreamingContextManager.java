package com.sengled.mediaworker.algorithm.context;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson.JSONObject;
import com.sengled.media.algorithm.MediaAlgorithmService;
import com.sengled.media.algorithm.QueryAlgorithmConfigRequest;
import com.sengled.media.algorithm.config.AlgorithmConfig;
import com.sengled.media.interfaces.Algorithm;
import com.sengled.media.interfaces.exceptions.AlgorithmIntanceCloseException;
import com.sengled.media.interfaces.exceptions.AlgorithmIntanceCreateException;
import com.sengled.mediaworker.RecordCounter;
import com.sengled.mediaworker.algorithm.ProcessorManager;
import com.sengled.mediaworker.algorithm.context.AlgorithmConfigWarpper.MotionConfig;
import com.sengled.mediaworker.algorithm.decode.KinesisFrameDecoder.FrameConfig;

@Component
public class StreamingContextManager implements InitializingBean{
	private static final Logger LOGGER = LoggerFactory.getLogger(StreamingContextManager.class);
	
	private static final long CONTEXT_EXPIRE_TIME_MILLIS = 10 * 60 * 1000;
	
    @Value("${motion.sensitivity.high}")
    private int motionHigh;
    @Value("${motion.sensitivity.normal}")
    private int motionNormal;
    @Value("${motion.sensitivity.low}")
    private int motionLow;
	    
	private ConcurrentHashMap<String, StreamingContext> streamingContextMap;
	private ConcurrentHashMap<String, AlgorithmConfigWarpper> AlgorithmConfigMap;
	private Timer timer;
	private Object lockObject  = new Object();;
	
	@Autowired
    private RecordCounter recordCounter;
	
	@Autowired
	MediaAlgorithmService mediaAlgorithmService;
	
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
		AlgorithmConfigMap   =   new ConcurrentHashMap<>();
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
	
	public StreamingContext findOrCreateStreamingContext(ProcessorManager processor,String tokenMask,String utcDateTime,FrameConfig frameConfig) throws AlgorithmIntanceCreateException{
	    
		StreamingContext context =  null;
		boolean isRefresh = frameConfig.getAction().equals("open");
		
		AlgorithmConfigWarpper algorithmConfig;
		try {
            algorithmConfig = getAlgorithmConfig(isRefresh,tokenMask);
        } catch (Exception e) {
            throw new AlgorithmIntanceCreateException(e);
        }
		MotionConfig baseConfig = algorithmConfig.getBaseConfig();
		
		synchronized (lockObject) {
		    context =  streamingContextMap.get(tokenMask);
            if( null == context ){ 
                context =  newAlgorithmContext(processor,tokenMask,utcDateTime, baseConfig);
            }else{
                //设置  数据中的UTC时间
                context.setUtcDateTime(utcDateTime);
                //设置算法参数
                context.getAlgorithm().setParameters(JSONObject.toJSONString(baseConfig));
                //设置 上次接收到数据的时间
                context.setLastTimeContextUpdateTimestamp(context.getContextUpdateTimestamp());
                //设置 本次接收到数据的时间
                context.setContextUpdateTimestamp(System.currentTimeMillis());
            }
            context.setConfig(algorithmConfig);
            context.setFrameConfig(frameConfig);
        }

		return context;
	}
	private AlgorithmConfigWarpper getAlgorithmConfig(boolean isRefresh,String tokenMask) throws Exception{
	    AlgorithmConfigWarpper configWapper =  AlgorithmConfigMap.get(tokenMask);
        if( null != configWapper && ! isRefresh ){
            return configWapper;
        }
        QueryAlgorithmConfigRequest request = new QueryAlgorithmConfigRequest();
        request.setToken(StringUtils.split(tokenMask, ",")[0]);
        AlgorithmConfig algor = mediaAlgorithmService.getAlgorithmConfig(request);
        if( null == algor ) {
            LOGGER.error("getAlgorithmConfig error. tokenMask:{} ", tokenMask);
            throw new Exception("getAlgorithmConfig return null");
        }
        LOGGER.info("Mediabase return AlgorithmConfig:{}, tokenMask:{}",algor, tokenMask);
        configWapper = new AlgorithmConfigWarpper(algor,motionHigh,motionNormal,motionLow);
        AlgorithmConfigMap.put(tokenMask, configWapper);
        LOGGER.info("AlgorithmConfigWarpper:{} tokenMask:{}",configWapper , tokenMask);
        return configWapper;
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
			LOGGER.info("Token:{},currentTime{},updateDate:{}",entry.getKey(),DateFormatUtils.format(currentTime, Context.UTC_DATE_FORMAT[0]),DateFormatUtils.format(updateTimestamp,Context.UTC_DATE_FORMAT[0]));

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
