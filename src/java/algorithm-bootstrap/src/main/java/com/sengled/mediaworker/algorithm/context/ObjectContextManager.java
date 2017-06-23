package com.sengled.mediaworker.algorithm.context;

import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
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
	
	public ObjectContext findOrCreateStreamingContext(String token){
		ObjectContext context =  objectContextMap.get(token);
		if (context == null) {
			context =  new ObjectContext(token);
		}else{
			//设置  数据中的UTC时间
			//设置算法参数
			//设置 上次接收到数据的时间
			//设置 本次接收到数据的时间
		}
		return context;
	}

}
