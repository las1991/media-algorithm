package com.sengled.mediaworker.algorithm.action;

import java.util.Date;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sengled.mediaworker.algorithm.FeedListener;
import com.sengled.mediaworker.algorithm.Function;
import com.sengled.mediaworker.algorithm.Operation;
import com.sengled.mediaworker.algorithm.StreamingContext;
import com.sengled.mediaworker.algorithm.event.MotionEvent;
import com.sengled.mediaworker.algorithm.event.ObjectEvent;
import com.sengled.mediaworker.algorithm.pydto.YUVImage;

public class ExecAction extends Action {
	private static final int  MOTION_INTERVAL = 15;
	private static final Logger LOGGER = LoggerFactory.getLogger(ExecAction.class);
	
	@Override
	public void feed(final StreamingContext context, final YUVImage yuvImage,final FeedListener listener) {
		final String token = context.getToken();
		final String model = context.getModel();
		Date lastMotionDate = context.getLastMotionDate();
		if(lastMotionDate !=null){
			if((context.getLastUtcDateTime().getTime()/1000 - lastMotionDate.getTime()/1000 ) <= MOTION_INTERVAL){
				LOGGER.info("Motion MOTION_INTERVAL:{}s",MOTION_INTERVAL);
				return;
			}else{
				context.setLastMotionDate(null);
				context.reloadAlgorithmModel("Motion after pass 15s");
			}
		}
		
		LOGGER.debug("token:{},model:{},pythonObjectId:{},parameters:{}", token, model,context.getAlgorithm().getPythonObjectId(), context.getAlgorithm().getParameters());
		Future<String> result = context.getProcessor().submit(new Operation<String>() {
			@Override
			public String apply(Function function) {
				return function.feed(context.getAlgorithm(), yuvImage);
			}
		});

		// 如果python进程算法模型上下文丢失，则重新初始化算法模型
		String text = "";
		try {
			text = result.get();
			LOGGER.debug("token:{},model:{},feed return:{}", token, model, text);
			if (Action.NULL_ALGORITHM_MODEL.equals(text)) {// feed 返回ERROR
											// 时，重新初始化算法模型，丢弃本次接收的数据不再调用feed
				LOGGER.error("Feed result "+Action.NULL_ALGORITHM_MODEL+". run reloadAlgorithmModel");
				context.reloadAlgorithmModel("Feed return "+Action.NULL_ALGORITHM_MODEL);
				return;
			}
			if (Action.NORESULT.equals(text)) {
				LOGGER.debug("Feed result NORESULT. token:{}",token);
				return;
			}
			handleListenerEvent(text,context, yuvImage, listener);
		}catch(InterruptedException e){
			LOGGER.error("InterruptedException token:{}",token);
			LOGGER.error(e.getMessage(),e);
			return;
		}catch (Exception e) {
			LOGGER.info("feed result:{}",text);
			LOGGER.error("token:" + token + " Feed Exception." + e.getMessage(), e);
			return;
		}

		LOGGER.debug("token:{},model:{},OpenAction feed finisthed...", token, model);

	}
	private void handleListenerEvent(String text,final StreamingContext context, final YUVImage yuvImage,final FeedListener listener) throws Exception{
		Future<byte[]> jpgDate = context.getProcessor().submit(new Operation<byte[]>() {
			@Override
			public byte[] apply(Function function) {
				return function.encode(context.getToken(), yuvImage.getWidth(), yuvImage.getHeight(), yuvImage.getWidth(),
						yuvImage.getHeight(), yuvImage.getYUVData());
			}
		});

		byte[] jpgData = jpgDate.get();
		JSONObject jsonObj = JSON.parseObject(text.trim());
		String zoneId = jsonObj.getString("zone_id");
		String model = context.getModel();
		String token =  context.getToken();
		
		switch(model){
			case "motion":
				MotionEvent event = new MotionEvent();
				event.setToken(token);
				event.setModel(model);
				event.setZoneId(zoneId);
				event.setUtcDate(context.getLastUtcDateTime());
				event.setJpgData(jpgData);
				listener.post(event);
				//update motion time
				context.setLastMotionDate(context.getLastUtcDateTime());
				break;
			case "object":
				ObjectEvent objectEvent = new ObjectEvent();
				objectEvent.setToken(token);
				listener.post(objectEvent);
				break;
			default:
				LOGGER.error("model:{} nonsupport",model);
				return;
		}
	}

}
