package com.sengled.mediaworker.algorithm.context;

import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.sengled.media.interfaces.Algorithm;
import com.sengled.media.interfaces.exceptions.AlgorithmIntanceCreateException;
import com.sengled.mediaworker.algorithm.ProcessorManager;
import com.sengled.mediaworker.algorithm.decode.KinesisFrameDecoder.MotionConfig;
import com.sengled.mediaworker.algorithm.decode.KinesisFrameDecoder.ObjectConfig;
/**
 * 物体识别上下文管理
 * @author media-liwei
 *
 */
@Component
public class ObjectContextManager implements InitializingBean{
	private static final Logger LOGGER = LoggerFactory.getLogger(ObjectContextManager.class);
	
	private ConcurrentHashMap<String, ObjectContext> objectContextMap;
	
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
		objectContextMap = new ConcurrentHashMap<>();
	}
	
	public ObjectContext findOrCreateStreamingContext(StreamingContext streamingContext){
		String token = streamingContext.getToken();
		ObjectContext context =  objectContextMap.get(token);
		
		ObjectConfig objectConfig =  streamingContext.getConfig().getObjectConfig();
		ObjectConfig finalObjectConfig = new ObjectConfig();
		BeanUtils.copyProperties(objectConfig, finalObjectConfig);
		
		if (context == null) {
			context =  newObjectContext(streamingContext);
		}else{
			//设置算法参数
			//设置 上次接收到数据的时间
			//设置 本次接收到数据的时间
			context.setUtcDateTime(streamingContext.getUtcDateTime());
			context.setYuvImage(streamingContext.getYuvImage());
			context.setNalData(streamingContext.getNalData());
			context.setObjectConfig(finalObjectConfig);
		}
		return context;
	}
	private ObjectContext newObjectContext(StreamingContext context) {
		ObjectContext objectContext =  new ObjectContext(context);
		objectContextMap.put(context.getToken(), objectContext);
		return objectContext;
	}
}
