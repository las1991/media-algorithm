package com.sengled.mediaworker.algorithm;

import java.text.ParseException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sengled.media.interfaces.Algorithm;
import com.sengled.media.interfaces.YUVImage;
import com.sengled.mediaworker.RecordCounter;
import com.sengled.mediaworker.algorithm.action.Action;
import com.sengled.mediaworker.algorithm.action.CloseAction;
import com.sengled.mediaworker.algorithm.action.ExecAction;
import com.sengled.mediaworker.algorithm.action.OpenAction;

/**
 * 
 * @author liwei
 *
 */
public class StreamingContext {
	private static final Logger LOGGER = LoggerFactory.getLogger(StreamingContext.class);

	//Motion间隔时间
	private static final long  MOTION_INTERVAL_TIME_MSCE = 15 * 1000;
	//包最大延时
	private static final long  MAX_DELAYED_TIME_MSCE = 10 * 1000;

	private String token;
	/**
	 * @see @RecordProcessor.MODEL_LIST
	 */
	private String model;
	private String utcDateTime;
	private Date lastMotionDate;
	private Date lastTimeUpdateDate;
	private Date updateDate;
	private Algorithm algorithm;
	private Action action;
	private ProcessorManager processorManager;
	private StreamingContextManager streamingContextManager;
	private RecordCounter recordCounter;
	
	public final Action openAction = new OpenAction();
	public final Action execAction = new ExecAction();
	public final Action closeAction = new CloseAction();
	
	StreamingContext(String token, String model,
					Algorithm algorithm,
					ProcessorManager processorManager,
					RecordCounter recordCounter,
					StreamingContextManager streamingContextManager
					) {
		this.token = token;
		this.model = model;
		this.algorithm = algorithm;
		this.processorManager = processorManager;
		this.streamingContextManager = streamingContextManager;
		this.recordCounter = recordCounter;
		LOGGER.info("Token:{},Model:{},Create StreamingContext", token,model);
	}

	public void feed(final YUVImage yuvImage, final FeedListener listener) throws Exception {
		if (yuvImage == null || listener == null) {
			throw new IllegalArgumentException("params exception.");
		}
		action.feed(this, yuvImage, listener);
	}
 

	public Date getLastUtcDateTime() {
		if(StringUtils.isBlank(utcDateTime)){
			return null;
		}
		try {
			return DateUtils.parseDate(utcDateTime, new String[] { "yyyy-MM-dd HH:mm:ss.SSS" });
		} catch (ParseException e) {
			LOGGER.error("Token:{},parseDate failed.",token);
			LOGGER.error(e.getMessage(), e);
		}
		return null;
	}
	public boolean isSkipHandle(){
		boolean isSkip = false;
		//motion 检测间隔为15s
		Date lastMotionDate = getLastMotionDate();
		Date lastUtcDateTime = getLastUtcDateTime();
		if(lastMotionDate !=null && lastUtcDateTime !=null){
			long sinceLastMotion = (lastUtcDateTime.getTime() - lastMotionDate.getTime());
			if(sinceLastMotion <= MOTION_INTERVAL_TIME_MSCE){
				LOGGER.debug("Token:{},Since last time motion:{} msec <= {} msec skip.",token,sinceLastMotion,MOTION_INTERVAL_TIME_MSCE);
				isSkip = true;
			}else{
				setLastMotionDate(null);
			}
		}
		//过期数据
		Date lastUtcDate = getLastUtcDateTime();
		if(lastUtcDate !=null && updateDate !=null){
			long delayedTime = updateDate.getTime() - lastUtcDate.getTime();
			if( delayedTime >= MAX_DELAYED_TIME_MSCE){
				LOGGER.debug("Token:{},lastUtcDate:{},intervalTime:{} >= {} skip.",token,lastUtcDate,delayedTime,MAX_DELAYED_TIME_MSCE);
				isSkip = true;
				recordCounter.getDataDelayedCount().addAndGet(1);
			}
		}
		return isSkip;
	}
	public String getToken() {
		return token;
	}

	public Algorithm getAlgorithm() {
		return algorithm;
	}

	public Action getAction() {
		return action;
	}

	public void setAction(Action action) {
		this.action = action;
	}

	public String getModel() {
		return this.model;
	}

	public String getUtcDateTime() {
		return utcDateTime;
	}

	public void setUtcDateTime(String utcDateTime) {
		this.utcDateTime = utcDateTime;
	}

	public Date getLastMotionDate() {
		return lastMotionDate;
	}

	public void setLastMotionDate(Date lastMotionDate) {
		this.lastMotionDate = lastMotionDate;
	}

	public ProcessorManager getProcessorManager() {
		return processorManager;
	}

	public void setProcessorManager(ProcessorManager processorManager) {
		this.processorManager = processorManager;
	}

	public StreamingContextManager getStreamingContextManager() {
		return streamingContextManager;
	}

	public void setStreamingContextManager(StreamingContextManager streamingContextManager) {
		this.streamingContextManager = streamingContextManager;
	}

	public Date getUpdateDate() {
		return updateDate;
	}

	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}

	public Date getLastTimeUpdateDate() {
		return lastTimeUpdateDate;
	}

	public void setLastTimeUpdateDate(Date lastTimeUpdateDate) {
		this.lastTimeUpdateDate = lastTimeUpdateDate;
	}

	
}
