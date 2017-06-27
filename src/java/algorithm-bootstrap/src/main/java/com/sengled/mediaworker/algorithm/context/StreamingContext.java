package com.sengled.mediaworker.algorithm.context;

import java.text.ParseException;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sengled.media.interfaces.Algorithm;
import com.sengled.media.interfaces.YUVImage;
import com.sengled.mediaworker.RecordCounter;
import com.sengled.mediaworker.algorithm.ProcessorManager;
import com.sengled.mediaworker.algorithm.action.Action;
import com.sengled.mediaworker.algorithm.action.CloseAction;
import com.sengled.mediaworker.algorithm.action.ExecAction;
import com.sengled.mediaworker.algorithm.action.OpenAction;
import com.sengled.mediaworker.algorithm.decode.KinesisFrameDecoder.FrameConfig;
import com.sengled.mediaworker.algorithm.feedlistener.FeedListener;

/**
 * 
 * @author liwei
 *
 */
public class StreamingContext extends Context{
	private static final Logger LOGGER = LoggerFactory.getLogger(StreamingContext.class);

	private String token;
	//接收kinesis数据中的utc时间
	private String utcDateTime;
	//保存最后一次Motion的时间
	private Long lastMotionTimestamp;
	//上次 接收到数据的时间
	private Long lastTimeContextUpdateTimestamp;
	//接收到数据的时间
	private Long contextUpdateTimestamp;
	//创建上下文时间
	private Long contextCreateTimestamp;
	//配置
	private FrameConfig config;
	private byte[] nalData;
	private YUVImage yuvImage;
	private boolean isReport = true;
	private Algorithm algorithm;
	private Action action;
	private ProcessorManager processorManager;
	private StreamingContextManager streamingContextManager;
	private RecordCounter recordCounter;
	
	public final Action openAction = new OpenAction();
	public final Action execAction = new ExecAction();
	public final Action closeAction = new CloseAction();
	
	StreamingContext(String token, 
					String utcDateTime,
					Algorithm algorithm,
					ProcessorManager processorManager,
					RecordCounter recordCounter,
					StreamingContextManager streamingContextManager
					) {
		this.token = token;
		this.algorithm = algorithm;
		this.utcDateTime = utcDateTime;
		this.processorManager = processorManager;
		this.streamingContextManager = streamingContextManager;
		this.recordCounter = recordCounter;
		this.contextCreateTimestamp = System.currentTimeMillis();
		this.contextUpdateTimestamp = contextCreateTimestamp;
		this.lastMotionTimestamp = null;
		LOGGER.info("Token:{},Model:{},Create StreamingContext", token);
	}

	public void feed(final FeedListener[] listeners) throws Exception {
		if (nalData == null || listeners == null) {
			throw new IllegalArgumentException("params exception.");
		}
		action.feed(this, listeners);
	}

	public Date getUtcDateTime() {
		if(StringUtils.isBlank(utcDateTime)){
			return null;
		}
		try {
			return DateUtils.parseDate(utcDateTime, UTC_DATE_FORMAT);
		} catch (ParseException e) {
			LOGGER.error("Token:{},parseDate failed.",token);
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
				LOGGER.info("Token:{},utcDate:{},intervalTime:{} >= {} skip.",token,utcDate,delayedTime,maxDelayedTimeMsce);
				expire = true;
				recordCounter.addAndGetDataDelayedCount(1);
			}
		}
		return expire;
	}
	public void reportCheck(Long motionIntervalTimeMsce){
		Date utcDateTime = getUtcDateTime();
		if(lastMotionTimestamp !=null && utcDateTime !=null){
			long sinceLastMotion = (utcDateTime.getTime() - lastMotionTimestamp.longValue());
			
			if(sinceLastMotion <= motionIntervalTimeMsce){
				LOGGER.info("Token:{},Since last time motion:{} msec <= {} msec isReport=false.",token,sinceLastMotion,motionIntervalTimeMsce);
				isReport = false;
			}else{
				lastMotionTimestamp = null;
				LOGGER.info("Token:{},Since last time motion:{} msec > {} msec .isReport=true.",token,sinceLastMotion,motionIntervalTimeMsce);
				isReport = true;
			}
		}
	}
	public String getToken() {
		return token;
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

	public Long getLastMotionTimestamp() {
		return lastMotionTimestamp;
	}
	public void setLastMotionTimestamp(Long lastMotionTimestamp) {
		this.lastMotionTimestamp = lastMotionTimestamp;
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
	
	public boolean isReport(){
		return isReport;
	}
	
	public FrameConfig getConfig() {
		return config;
	}

	public void setConfig(FrameConfig config) {
		this.config = config;
	}

	public byte[] getNalData() {
		return nalData;
	}

	public void setNalData(byte[] nalData) {
		this.nalData = nalData;
	}

	public YUVImage getYuvImage() {
		return yuvImage;
	}

	public void setYuvImage(YUVImage yuvImage) {
		this.yuvImage = yuvImage;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Token:" + token);
		sb.append(" utcDateTime" + utcDateTime);
		sb.append(" lastMotionTimestamp:" + getTimestampFormat(lastMotionTimestamp));
		sb.append(" lastTimeContextUpdateTimestamp:" + getTimestampFormat(lastTimeContextUpdateTimestamp));
		sb.append(" contextUpdateTimestamp:" + getTimestampFormat(contextUpdateTimestamp));
		sb.append(" contextCreateTimestamp:" + getTimestampFormat(contextCreateTimestamp));
		sb.append(" algorithm:" + algorithm.toString());
		return sb.toString();
	}	
}
