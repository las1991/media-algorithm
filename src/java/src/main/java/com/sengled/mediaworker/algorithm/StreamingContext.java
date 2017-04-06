package com.sengled.mediaworker.algorithm;

import java.io.Closeable;
import java.text.ParseException;
import java.util.Date;
import java.util.concurrent.Future;

import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.sengled.mediaworker.algorithm.action.Action;
import com.sengled.mediaworker.algorithm.action.CloseAction;
import com.sengled.mediaworker.algorithm.action.ExecAction;
import com.sengled.mediaworker.algorithm.action.OpenAction;
import com.sengled.mediaworker.algorithm.pydto.Algorithm;
import com.sengled.mediaworker.algorithm.pydto.YUVImage;

/**
 * 
 * @author liwei
 *
 */
public class StreamingContext implements Closeable{
	private static final Logger LOGGER = LoggerFactory.getLogger(StreamingContext.class);
	
	private String token;
	/**
	 * @see @RecordProcessor.MODEL_LIST
	 */
	private String model;
	private String utcDate;
	private Date lastMotionDate;
	private PythonProcessor processor;
	private Algorithm algorithm;
	
	public final Action openAction = new OpenAction();
	public final Action execAction = new ExecAction();
	public final Action closeAction = new CloseAction();
	
	private Action action;
	
	public StreamingContext(String token,String model,PythonProcessor processor,Algorithm algorithm){
		
		this.token = token;
		this.model = model;
		this.algorithm = algorithm;
		this.processor = processor;
		LOGGER.debug("create StreamingContext token:{},algorithm:{}",token,algorithm);
		LOGGER.debug("create StreamingContext processor:{}",processor);
	}

	/**
	 * 提交image 计算相关事件
	 * @param image
	 * @param listener
	 * @return
	 */
	public void feed(final YUVImage yuvImage,final FeedListener listener) throws Exception{
		if(yuvImage ==null || listener==null){
			throw new IllegalArgumentException("params exception.");
		}
		LOGGER.debug("parameters:"+this.getAlgorithm().getParameters());
		action.feed(this,yuvImage,listener);
		
	}
	@Override
	public void close()  {
		processor.removeAlgorithm(this);
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
		this.action  = action;
	} 
	public String getModel(){
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

	public Date getLastUtcDateTime(){
		try {
			return DateUtils.parseDate(utcDate, new String[]{"yyyy-MM-dd HH:mm:ss.SSS"});
			//return  DateFormatUtils.format(utcDateTime, "yyyy-MM-dd HH:mm:ss.SSS");
		} catch (ParseException e) {
			LOGGER.error(e.getMessage(),e);
		}
		return null;
	}

	public void reloadAlgorithmModel(String cause) throws Exception {
		LOGGER.info("reloadAlgorithmModel cause:{}",cause);
		submit(new Operation<Void>() {
			@Override
			public Void apply(Function function) {
				function.close(algorithm);
				algorithm.setPythonObjectId(function.newAlgorithmModel(model, token));
				return null;
			}
		}).get();
	}

    public <T> Future<T> submit(Operation<T> operation) {
        return processor.submit(operation);
    }
}
