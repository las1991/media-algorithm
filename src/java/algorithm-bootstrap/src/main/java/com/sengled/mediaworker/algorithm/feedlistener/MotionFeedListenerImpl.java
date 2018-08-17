package com.sengled.mediaworker.algorithm.feedlistener;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.google.common.eventbus.AsyncEventBus;
import com.sengled.media.interfaces.YUVImage;
import com.sengled.media.interfaces.exceptions.EncodeException;
import com.sengled.mediaworker.algorithm.ImageUtils;
import com.sengled.mediaworker.algorithm.MotionAndObjectReportManager;
import com.sengled.mediaworker.algorithm.ProcessorManager;
import com.sengled.mediaworker.algorithm.context.AlgorithmConfigWarpper.MotionConfig;
import com.sengled.mediaworker.algorithm.context.StreamingContext;
import com.sengled.mediaworker.algorithm.event.MotionEvent;
import com.sengled.mediaworker.algorithm.service.MotionEventHandler;
import com.sengled.mediaworker.algorithm.service.dto.MotionFeedResult;
import com.sengled.mediaworker.algorithm.service.dto.MotionFeedResult.ZoneInfo;


/**
 * @author media-liwei
 *
 */
@Component
public class MotionFeedListenerImpl implements FeedListener,InitializingBean{
	private static final Logger LOGGER = LoggerFactory.getLogger(MotionFeedListenerImpl.class);
	private final static int EVENT_BUS_THREAD_COUNT = 100;
	private AsyncEventBus eventBus;
	@Autowired
	private MotionEventHandler motionEventHandler;
	@Autowired
	ProcessorManager processorManagerImpl;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		try {
			initialize();
		} catch (Exception e) {
			LOGGER.error(e.getMessage(),e);
			System.exit(1);
		}
	}
	
	private void initialize(){
		LOGGER.info("MotionFeedListener init.EVENT_BUS_THREAD_COUNT:{}",EVENT_BUS_THREAD_COUNT);
		eventBus = new AsyncEventBus(Executors.newFixedThreadPool(EVENT_BUS_THREAD_COUNT));
		eventBus.register(motionEventHandler);
	}

	private void feedResultHandle(StreamingContext context, 
	                              final YUVImage yuvImage, 
	                              final byte[] nalData,
	                              final MotionFeedResult motionFeedResult) throws Exception {
		LOGGER.debug("Begin feedResultHandle. StreamingContext:{},motionFeedResult:{}",context,motionFeedResult);
		
		MotionConfig motionConfig =  context.getConfig().getMotionConfig();
		String tokenMask =  context.getTokenMask();
		String token = context.getToken();
		
		if(null == motionConfig){
			LOGGER.info("[{}], skip. motionConfig is null. config:{}",context.getToken(),context.getConfig());
			return;
		}
		
        if( ! MotionAndObjectReportManager.isAllowMotionReport(token) ){
            LOGGER.info("[{}] skip.  motion lasttime report is :{}",token, MotionAndObjectReportManager.getMotionRportTime(token));
        }
		
		ZoneInfo  zone = motionFeedResult.getMotion().get(0);
		byte[] jpgData;
		try {
			jpgData = processorManagerImpl.encode(context.getTokenMask(), yuvImage.getYUVData(), yuvImage.getWidth(), yuvImage.getHeight(), yuvImage.getWidth(), yuvImage.getHeight());
			//FIXME 对图像进行motion画框操作{
			if(LOGGER.isDebugEnabled()){
				List<List<Integer>> boxs = new ArrayList<List<Integer>>();
				for (ZoneInfo zoneInfo : motionFeedResult.getMotion()) {
					for (List<Integer> list : zoneInfo.getBoxs()) {
						List<Integer> posStr = ImageUtils.convertPctToPixel(yuvImage.getWidth(), yuvImage.getHeight(), list);
						boxs.add(posStr);
					}
				}
				jpgData = ImageUtils.drawRect(boxs, Color.YELLOW, jpgData);
			}
			//}
		} catch (EncodeException e) {
			LOGGER.error(e.getMessage(),e);
			return;
		}catch(Exception e2){
			LOGGER.error(e2.getMessage(),e2);
			return;
		}

		LOGGER.info("tokenMask:{},Get motion. zoneId:{},",tokenMask,zone.getZone_id());
		Date copyUtcDate = new Date(context.getUtcDateTime().getTime());
		MotionEvent event = new MotionEvent(tokenMask,copyUtcDate,jpgData,context.getFileExpiresHours(),zone.getZone_id().intValue()+"");
		eventBus.post(event);
		MotionAndObjectReportManager.markMotionEvent(context.getToken(), DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
	}

    @Override
    public void feedResultHandle(StreamingContext context,
                            final byte[] nalData,
                            final Map<Integer, YUVImage> yuvImageResultMap,
                            final Map<Integer, MotionFeedResult> motionFeedResultMap) throws Exception {
         int  frameIndex = motionFeedResultMap.keySet().stream().findFirst().get();
         feedResultHandle(context, yuvImageResultMap.get(frameIndex), nalData, motionFeedResultMap.get(frameIndex));
    }
}
