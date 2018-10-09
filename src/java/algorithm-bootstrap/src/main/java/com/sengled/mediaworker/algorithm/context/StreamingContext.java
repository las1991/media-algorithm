package com.sengled.mediaworker.algorithm.context;

import java.text.ParseException;
import java.util.Date;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sengled.media.interfaces.Algorithm;
import com.sengled.mediaworker.RecordCounter;
import com.sengled.mediaworker.algorithm.ProcessorManager;
import com.sengled.mediaworker.algorithm.action.Action;
import com.sengled.mediaworker.algorithm.action.ExecAction;
import com.sengled.mediaworker.algorithm.decode.KinesisFrameDecoder.Frame;
import com.sengled.mediaworker.algorithm.decode.KinesisFrameDecoder.FrameConfig;
import com.sengled.mediaworker.algorithm.feedlistener.FeedListener;

/**
 * 
 * @author liwei
 *
 */
public class StreamingContext extends Context{
	private static final Logger LOGGER = LoggerFactory.getLogger(StreamingContext.class);

	private String tokenMask;
	//接收kinesis数据中的utc时间
	private String utcDateTime;
	//上次 接收到数据的时间
	private Long lastTimeContextUpdateTimestamp;
	//接收到数据的时间
	private Long contextUpdateTimestamp;
	//创建上下文时间
	private Long contextCreateTimestamp;
	//配置
	private FrameConfig frameConfig;
	private AlgorithmConfigWarpper config;

	private Algorithm algorithm;
	private Action action = new ExecAction();
	private ProcessorManager processorManager;
	private StreamingContextManager streamingContextManager;
	private RecordCounter recordCounter;
	
	StreamingContext(String tokenMask, 
					String utcDateTime,
					Algorithm algorithm,
					ProcessorManager processorManager,
					RecordCounter recordCounter,
					AlgorithmConfigWarpper config,
					StreamingContextManager streamingContextManager
					) {
		this.tokenMask = tokenMask;
		this.algorithm = algorithm;
		this.utcDateTime = utcDateTime;
		this.processorManager = processorManager;
	    this.config = config;
		this.streamingContextManager = streamingContextManager;
		this.recordCounter = recordCounter;
		this.contextCreateTimestamp = System.currentTimeMillis();
		this.contextUpdateTimestamp = contextCreateTimestamp;
		LOGGER.info("Token:{},Model:{},Create StreamingContext", tokenMask);
	}

	public void feed(final Frame frame,final FeedListener[] listeners) throws Exception {
		if ( listeners == null) {
			throw new IllegalArgumentException("params exception.");
		}
		action.feed(this, frame, listeners);
	}

	public Date getUtcDateTime() {
		if(StringUtils.isBlank(utcDateTime)){
			return null;
		}
		try {
			return DateUtils.parseDate(utcDateTime, UTC_DATE_FORMAT);
		} catch (ParseException e) {
			LOGGER.error("Token:{},parseDate failed.",tokenMask);
			LOGGER.error(e.getMessage(), e);
		}
		return null;
	}
	
	public boolean isDataExpire(Long maxDelayedTimeMsce){
		boolean expire = false;
		//过期数据
		Date utcDate = getUtcDateTime();
		if(utcDate !=null){
			long delayedTime = contextUpdateTimestamp - utcDate.getTime();
			if( delayedTime >= maxDelayedTimeMsce){
				LOGGER.warn("Token:{},utcDate:{},intervalTime:{} >= {} skip.",tokenMask,utcDate,delayedTime,maxDelayedTimeMsce);
				expire = true;
				recordCounter.addAndGetDataDelayedCount(1);
			}
		}
		return expire;
	}

	public String getTokenMask() {
		return tokenMask;
	}
	
    public String getToken() {
        return tokenMask.split(",")[0];
    }

	public Algorithm getAlgorithm() {
		return algorithm;
	}

	public void setAction(Action action) {
		this.action = action;
	}

	public void setUtcDateTime(String utcDateTime) {
		this.utcDateTime = utcDateTime;
	}
 
	public ProcessorManager getProcessorManager() {
		return processorManager;
	}

	public StreamingContextManager getStreamingContextManager() {
		return streamingContextManager;
	}
	
	public long getContextCreateTimestamp() {
		return contextCreateTimestamp;
	}

	public void setContextCreateTimestamp(long contextCreateTimestamp) {
		this.contextCreateTimestamp = contextCreateTimestamp;
	}

	public Long getContextUpdateTimestamp() {
		return contextUpdateTimestamp;
	}

	public void setContextUpdateTimestamp(Long contextUpdateTimestamp) {
		this.contextUpdateTimestamp = contextUpdateTimestamp;
	}

	public Long getLastTimeContextUpdateTimestamp() {
		return lastTimeContextUpdateTimestamp;
	}

	public void setLastTimeContextUpdateTimestamp(Long lastTimeContextUpdateTimestamp) {
		this.lastTimeContextUpdateTimestamp = lastTimeContextUpdateTimestamp;
	}
	
	public String getTimestampFormat(Long timestamp) {
		if(null == timestamp){
			return "";
		}
		try {
			return DateFormatUtils.format(timestamp, "yyyy-MM-dd HH:mm:ss.SSS");
		} catch (Exception e) {
			LOGGER.error(e.getMessage(),e);
		}
		return "";
	}
	
	public int getFileExpiresHours(){
	    if( null == frameConfig ){
	        LOGGER.error("[{}] frameConfig is null.",tokenMask);
	        return 30 * 24;
	    }
	    return frameConfig.getFileExpiresHours();
	}

    public AlgorithmConfigWarpper getConfig() {
        return config;
    }

    public void setConfig(AlgorithmConfigWarpper config) {
        this.config = config;
    }

    public FrameConfig getFrameConfig() {
        return frameConfig;
    }

    public void setFrameConfig(FrameConfig frameConfig) {
        this.frameConfig = frameConfig;
    }	
}
