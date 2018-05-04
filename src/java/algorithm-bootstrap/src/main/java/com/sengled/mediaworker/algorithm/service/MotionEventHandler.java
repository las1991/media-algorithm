package com.sengled.mediaworker.algorithm.service;

import java.util.Collections;
import java.util.Date;
import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.amazonaws.services.s3.model.Tag;
import com.google.common.eventbus.Subscribe;
import com.sengled.mediaworker.algorithm.event.MotionEvent;
import com.sengled.mediaworker.algorithm.service.PutManager.ImageS3Info;
import com.sengled.mediaworker.algorithm.service.dto.AlgorithmResult;
import com.sengled.mediaworker.algorithm.service.dto.ObjectRecognitionInnerDto;
import com.sengled.mediaworker.s3.StorageProperties;

@Component
public class MotionEventHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(MotionEventHandler.class);

    @Autowired
    StorageProperties storageProperties;
	
	@Autowired
	PutManager testMerge;

	/**
	 * motion事件
	 * 
	 * @param event
	 */
	@Subscribe
	public void feedEvent(MotionEvent event) {
		LOGGER.info("Get MotionEvent:{}",event);
		Tag tag = storageProperties.getTag(event.getFileExpiresDays()+"");
		
        AlgorithmResult result = buildAlgorithmResult(event);
        switch(event.getFileExpiresDays() ) {
            case 1:
                testMerge.put1(new ImageS3Info( event.getJpgData(), tag,result));    
                break;
            case 2:
                testMerge.put2(new ImageS3Info( event.getJpgData(), tag,result));    
                break;
            case 7:
                testMerge.put7(new ImageS3Info( event.getJpgData(), tag,result));    
                break;
            default :
                testMerge.put30(new ImageS3Info( event.getJpgData(), tag,result));    
        }
		LOGGER.info("Token:{},MotionEvent finished",event.getToken());
	}

    private AlgorithmResult buildAlgorithmResult(MotionEvent event) {
        Date utcDateTime = event.getUtcDate();
        String zoneId = event.getZoneId();
        String imageS3Path = event.getToken() + "_motion_" + utcDateTime.getTime() +"_" +event.getFileExpiresHours()+".jpg";

        AlgorithmResult result = new AlgorithmResult();
        result.setEventType(AlgorithmResult.SLS_EVENT_TYPE_MOTION);
        result.setDataList(Collections.<ObjectRecognitionInnerDto>emptyList());
        result.setStreamId(event.getToken());
        result.setBigImage(imageS3Path);
        result.setSmallImage(imageS3Path);
        result.setTimeStamp(DateFormatUtils.format(utcDateTime, "yyyy-MM-dd HH:mm:ss"));
        result.setZoneId(Long.valueOf(zoneId));
        result.setFileExpiresHours(event.getFileExpiresHours());
        return result;
    }
}
