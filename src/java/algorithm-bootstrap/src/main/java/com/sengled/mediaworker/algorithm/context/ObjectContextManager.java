package com.sengled.mediaworker.algorithm.context;

import java.util.Date;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import com.sengled.mediaworker.algorithm.decode.KinesisFrameDecoder.ObjectConfig;
/**
 * 物体识别上下文管理
 * @author media-liwei
 *
 */
@Component
public class ObjectContextManager implements InitializingBean{
	private static final Logger LOGGER = LoggerFactory.getLogger(ObjectContextManager.class);
	private static final long CONTEXT_EXPIRE_TIME_MILLIS = 10 * 60 * 1000;
	
	
	private ConcurrentHashMap<String, ObjectContext> objectContextMap;
	private Timer timer;
	
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
		timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				try {
					LOGGER.info("objectContextMap size:{}",objectContextMap.size());
					cleanExpiredContext();
				} catch (Exception e) {
					LOGGER.error(e.getMessage(),e);
				}
			}
		}, 10 * 60 * 1000, CONTEXT_EXPIRE_TIME_MILLIS);
	}
	
	public ObjectContext findOrCreateStreamingContext(String token,Date utcDate,ObjectConfig config){
		ObjectContext context =  objectContextMap.get(token);	
		if (context == null) {
			context =  newObjectContext(token);
		}
		context.setUtcDateTime(utcDate);
		context.setContextUpdateTimestamp(System.currentTimeMillis());
		context.setObjectConfig(config);
		return context;
	}
	private ObjectContext newObjectContext(String token) {
		ObjectContext objectContext =  new ObjectContext(token);
		objectContextMap.put(token, objectContext);
		return objectContext;
	}
	private void cleanExpiredContext() {
		LOGGER.info("CleanExpireContext. ObjectContextMap size:{}",objectContextMap.size());
		for ( Entry<String, ObjectContext> entry : objectContextMap.entrySet()) {
			ObjectContext context = entry.getValue();
			long  updateTimestamp = context.getContextUpdateTimestamp();
			long idleTime =  System.currentTimeMillis() - updateTimestamp;
			if( idleTime >= CONTEXT_EXPIRE_TIME_MILLIS ){
				LOGGER.info("Token:{} Context expired clean...",context.getToken());
				if(LOGGER.isDebugEnabled()){
					LOGGER.debug("cleanExpiredContext ObjectContext:{}",context.toString());
				}
				objectContextMap.remove(context.getToken());
			}
		}
	}
}
