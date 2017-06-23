package com.sengled.mediaworker.algorithm.context;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.sengled.media.interfaces.YUVImage;
import com.sengled.mediaworker.algorithm.ProcessorManager;
import com.sengled.mediaworker.algorithm.ImageUtils;
import com.sengled.mediaworker.algorithm.decode.KinesisFrameDecoder.Data;
import com.sengled.mediaworker.algorithm.decode.KinesisFrameDecoder.ObjectConfig;
import com.sengled.mediaworker.algorithm.service.dto.MotionFeedResult;
import com.sengled.mediaworker.algorithm.service.dto.MotionFeedResult.ZoneInfo;
import com.sengled.mediaworker.algorithm.service.dto.ObjectRecognitionResult;
import com.sengled.mediaworker.algorithm.service.dto.ObjectRecognitionResult.Object;
import com.sengled.mediaworker.context.Context;
import com.sengled.mediaworker.httpclient.HttpResponseResult;
import com.sengled.mediaworker.httpclient.IHttpClient;

/**
 * 物体识别上下文
 * @author media-liwei
 *
 */
public class ObjectContext extends Context {
	private static final Logger LOGGER = LoggerFactory.getLogger(ObjectContext.class);
	private static final Long objectIntervalTimeMsce = 30000L;//物体识别间隔
	private static final int    percent = 20;//zone 和物体识别次百分比
	
	private String token;
	private Long lastObjectTimestamp;
	private Date utcDateTime;
	
	public ObjectContext(StreamingContext context) {
		this.token = context.getToken();
		this.utcDateTime = context.getUtcDateTime();
	}
	public void setLastObjectTimestamp(Long lastObjectTimestamp) {
		this.lastObjectTimestamp = lastObjectTimestamp;
	}

	private boolean objectIntervalCheck(){
		boolean skip = false;
		Date utcDateTime = getUtcDateTime();
		if(lastObjectTimestamp !=null && utcDateTime !=null){
			long sinceLastMotion = (utcDateTime.getTime() - lastObjectTimestamp.longValue());
			
			if(sinceLastMotion <= objectIntervalTimeMsce){
				LOGGER.info("Token:{},Since last time object:{} msec <= {} msec isReport=false.",token,sinceLastMotion,objectIntervalTimeMsce);
				skip = false;
			}else{
				lastObjectTimestamp = null;
				LOGGER.info("Token:{},Since last time object:{} msec > {} msec .isReport=true.",token,sinceLastMotion,objectIntervalTimeMsce);
				skip = true;
			}
		}
		return skip;
	}
	public Date getUtcDateTime() {
		return utcDateTime;
	}
	
	/**
	 * 物体识别处理
	 * @param processorManager  用于encode
	 * @param httpclient   
	 * @param objectRecognitionUrl  请求物体识别服务URL
	 * @param token 
	 * @param nal  
	 * @param yuvImage
	 * @param objectConfig  物体识别配置
	 * @param motionFeedResult  Motion结果
	 * @return
	 */
	public String match(ProcessorManager  processorManager,IHttpClient httpclient,String objectRecognitionUrl,
						String token,byte[] nal,YUVImage yuvImage,ObjectConfig objectConfig,MotionFeedResult motionFeedResult) {
		if(objectIntervalCheck()){
			return null;
		}
		HttpEntity putEntity = new ByteArrayEntity(nal);
		HttpResponseResult result = httpclient.put(objectRecognitionUrl, putEntity);
		
		if(result.getCode().intValue() != 200){
			LOGGER.error("object recognition http code {},body:{}",result.getCode(),result.getBody());
			return null;
		}
		String responseText = result.getBody();
		JSONObject jsonObj = JSONObject.parseObject(responseText);
		if(jsonObj.getJSONArray("objects").isEmpty()){
			LOGGER.info("object recognition NORESULT.");
			return null;
		}
		
		ObjectRecognitionResult objectResult = JSONObject.toJavaObject(jsonObj, ObjectRecognitionResult.class);
		LOGGER.info("recognition object JSON result:{},javaBean Result{}",jsonObj.toJSONString(),objectResult.toString());
		if(objectResult ==null || objectResult.objects == null || objectResult.objects.isEmpty()){
			LOGGER.info("object recognition objectResult empty.");
			return null;
		}
		return filter(processorManager,token,yuvImage,objectConfig,objectResult,motionFeedResult);
		
	}
	private String filter(ProcessorManager  processorManager,String token,YUVImage yuvImage,ObjectConfig objectConfig ,ObjectRecognitionResult objectRecognitionResult,MotionFeedResult motionFeedResult){
		LOGGER.info("Token:{},objectConfig:{},ObjectRecognitionResult:{},MotionFeedResult:{}",token,JSONObject.toJSON(objectConfig),JSONObject.toJSON(objectRecognitionResult),JSONObject.toJSON(motionFeedResult));
		
		
		//TODO 结果匹配
		/*
		 * 1.物体识别结果
		 * 2.红黄相交
		 * 
		 * 
		 * 
		 */
		//step 1  过滤zone外的物体识别
		List<Object> objectsResult = new ArrayList<>();
		for(Object object:objectRecognitionResult.objects){
			List<Integer> bbox = object.bbox_pct;
			String  objectType = object.type;
			String[] r1 = {bbox.get(0)+"",bbox.get(1)+"",bbox.get(2)+"",bbox.get(3)+""}; 
			//zone
			List<Data> dataList = objectConfig.getDataList();//zone 配置
			for (Data data : dataList) {
				String posTmp = data.getPos();
				//判断与物体识别结果相交
				String pos = ImageUtils.convert2spotLocation(posTmp);
				String[] r2 = pos.split(",");
				int area = ImageUtils.area(r1, r2);
				float r1percent = ImageUtils.areaPercent(r1, area);
				float r2percent = ImageUtils.areaPercent(r2, area);
				LOGGER.info("object:{} data:{}  intersection area:{} r1percent:{}, r2percent:{}", object,data,area,r1percent,r2percent);
				if(r1percent >= percent && objectType.equals("person")){//TODO
					objectsResult.add(object);
				}
			}
			
		}
		
		//step 2. 过滤未移动的区域
		List<Object> finalObjectsResult = new ArrayList<>();
		for(  Object object : objectsResult){
			List<Integer> bbox = object.bbox_pct;
			String[] r1 = {bbox.get(0)+"",bbox.get(1)+"",bbox.get(2)+"",bbox.get(3)+""}; 
			for(ZoneInfo motion : motionFeedResult.motion){
				List<List<Integer>> boxList = motion.boxs;
				for (List<Integer> list : boxList) {
					String[] r2 = {list.get(0)+"",list.get(1)+"",list.get(2)+"",list.get(3)+""};
					int area = ImageUtils.area(r1, r2);
					if(area > 0){
						finalObjectsResult.add(object);
					}
				}
			}
		}
		
		
		if(LOGGER.isDebugEnabled()){
			ImageUtils.draw(processorManager,token, yuvImage, objectConfig, objectRecognitionResult, motionFeedResult,finalObjectsResult);	
		}
		String matchResult = "{'zone_id':54,'type':['persion','car'],'zone_id':12,'type':['persion','dog']}";
		return matchResult;
	}
	public String getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token = token;
	}
	public static Long getObjectintervaltimemsce() {
		return objectIntervalTimeMsce;
	}
	public Long getLastObjectTimestamp() {
		return lastObjectTimestamp;
	}
	public void setUtcDateTime(Date utcDateTime) {
		this.utcDateTime = utcDateTime;
	}
	
	
}
