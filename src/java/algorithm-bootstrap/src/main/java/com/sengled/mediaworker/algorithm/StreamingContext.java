package com.sengled.mediaworker.algorithm;

import java.text.ParseException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sengled.media.interfaces.Algorithm;
import com.sengled.media.interfaces.YUVImage;
import com.sengled.media.interfaces.exceptions.AlgorithmIntanceCloseException;
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

	private static final long  MOTION_INTERVAL_SCE = 15;

	private String token;
	/**
	 * @see @RecordProcessor.MODEL_LIST
	 */
	private String model;
	private String utcDate;
	private Date lastMotionDate;
	private Algorithm algorithm;
	private Action action;
	private ProcessorManager processorManager;
	private StreamingContextManager streamingContextManager;
	
	public final Action openAction = new OpenAction();
	public final Action execAction = new ExecAction();
	public final Action closeAction = new CloseAction();
	
	public StreamingContext(String token, String model,Algorithm algorithm,ProcessorManager processorManager,StreamingContextManager streamingContextManager) {
		this.token = token;
		this.model = model;
		this.algorithm = algorithm;
		this.processorManager = processorManager;
		this.streamingContextManager = streamingContextManager;
		LOGGER.debug("Token:{},create StreamingContext algorithm:{}", token, algorithm);
	}

	public void feed(final YUVImage yuvImage, final FeedListener listener) throws Exception {
		if (yuvImage == null || listener == null) {
			throw new IllegalArgumentException("params exception.");
		}
		LOGGER.debug("Token:{},parameters:{}" ,token,this.getAlgorithm().getParameters());
		action.feed(this, yuvImage, listener);
	}
 

	public Date getLastUtcDateTime() {
		try {
			return DateUtils.parseDate(utcDate, new String[] { "yyyy-MM-dd HH:mm:ss.SSS" });
		} catch (ParseException e) {
			LOGGER.error("Token:{},parseDate failed.",token);
			LOGGER.error(e.getMessage(), e);
		}
		return null;
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

	public String getUtdDate() {
		return utcDate;
	}

	public void setUtcDate(String utcDate) {
		this.utcDate = utcDate;
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
	public boolean isSkipHandle(){
		boolean isSkip = false;
		Date lastMotionDate = getLastMotionDate();
		if(lastMotionDate !=null){
			long sinceLastMotion = getLastUtcDateTime().getTime()/1000 - lastMotionDate.getTime()/1000;
			if(sinceLastMotion <= MOTION_INTERVAL_SCE){
				LOGGER.info("Token:{},Since last time motion:{} sec <= {} sec",token,sinceLastMotion,MOTION_INTERVAL_SCE);
				isSkip = true;
			}else{
				setLastMotionDate(null);
				isSkip = false;
			}
		}
		return isSkip;
	}
}
