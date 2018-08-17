package com.sengled.mediaworker.algorithm.context;

import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sengled.mediaworker.algorithm.context.AlgorithmConfigWarpper.ObjectConfig;

/**
 * 物体识别上下文
 * 
 * @author media-liwei
 *
 */
public class ObjectContext extends Context {
	private static final Logger LOGGER = LoggerFactory.getLogger(ObjectContext.class);

	private final String token;
	private Date utcDateTime;
	private ObjectConfig objectConfig;
	//接收到数据的时间
	private Long contextUpdateTimestamp = System.currentTimeMillis();


	public ObjectContext(String token){
		this.token = token;
	}

	public Date getUtcDateTime() {
		return utcDateTime;
	}


	public String getToken() {
		return token;
	}
	
	public void setUtcDateTime(Date utcDateTime) {
		this.utcDateTime = utcDateTime;
	}


	public ObjectConfig getObjectConfig() {
		return objectConfig;
	}

	public void setObjectConfig(ObjectConfig objectConfig) {
		this.objectConfig = objectConfig;
	}

	public Long getContextUpdateTimestamp() {
		return contextUpdateTimestamp;
	}

	public void setContextUpdateTimestamp(Long contextUpdateTimestamp) {
		this.contextUpdateTimestamp = contextUpdateTimestamp;
	}

}
