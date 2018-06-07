package com.sengled.mediaworker.algorithm.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.amazonaws.services.s3.model.Tag;
import com.google.common.eventbus.Subscribe;
import com.sengled.mediaworker.algorithm.ObjectType;
import com.sengled.mediaworker.algorithm.event.ObjectEvent;
import com.sengled.mediaworker.algorithm.service.PutManager.ImageS3Info;
import com.sengled.mediaworker.algorithm.service.dto.AlgorithmResult;
import com.sengled.mediaworker.algorithm.service.dto.ObjectRecognitionInnerDto;
import com.sengled.mediaworker.algorithm.service.dto.ObjectRecognitionResult.TargetObject;
import com.sengled.mediaworker.s3.StorageProperties;

@Component
public class ObjectEventHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(ObjectEventHandler.class);

    @Autowired
    StorageProperties storageProperties;
    
    @Autowired
    PutManager putManager;
    

	@Subscribe
	public void feedEvent(ObjectEvent event) {
	    
	    LOGGER.info("Get ObjectEvent:{}",event);
	    Tag tag = storageProperties.getTag(event.getFileExpiresDays()+"");
	       
	    AlgorithmResult result = buildAlgorithmResult(event);
	    switch(event.getFileExpiresDays() ) {
            case 1:
                putManager.put1(new ImageS3Info( event.getJpgData(), tag,result));    
                break;
            case 2:
                putManager.put2(new ImageS3Info( event.getJpgData(), tag,result));    
                break;
            case 7:
                putManager.put7(new ImageS3Info( event.getJpgData(), tag,result));    
                break;
            default :
                putManager.put30(new ImageS3Info( event.getJpgData(), tag,result));    
        }
	        LOGGER.info("Token:{},ObjectEvent finished",event.getToken());
	}

	private AlgorithmResult buildAlgorithmResult(ObjectEvent event) {
        String token = event.getToken();
        Date utcDateTime = event.getUtcDate();
	    AlgorithmResult result;
        result = new AlgorithmResult();
        List<ObjectRecognitionInnerDto> dataList = new ArrayList<>();
        Map<Integer, Collection<TargetObject>> zoneToObjectMap = event.getResult().asMap();
        for (Entry<Integer, Collection<TargetObject>> entry : zoneToObjectMap.entrySet()) {
            int zoneid = entry.getKey();
            for (TargetObject object : entry.getValue()) {
                String pos = object.getBbox_pct().get(0) + ","+object.getBbox_pct().get(1)+","+object.getBbox_pct().get(2)+","+object.getBbox_pct().get(3);
                int targetType = ObjectType.findByName(object.getType()).value;
                ObjectRecognitionInnerDto orid = new ObjectRecognitionInnerDto(Long.valueOf(zoneid),pos , targetType, object.getScore());
                dataList.add(orid);
            }
        }
        
        result.setEventType(AlgorithmResult.SLS_EVENT_TYPE_OBJECT);
        result.setDataList(dataList);
        result.setStreamId(token);
        String imageS3Path = event.getToken() + "_object_" + utcDateTime.getTime() +"_" +event.getFileExpiresHours()+".jpg";
        result.setBigImage(imageS3Path);
        result.setSmallImage(imageS3Path);
        result.setTimeStamp(DateFormatUtils.format(utcDateTime, "yyyy-MM-dd HH:mm:ss"));
        result.setFileExpiresHours(event.getFileExpiresHours());
        return result;
    }
}
