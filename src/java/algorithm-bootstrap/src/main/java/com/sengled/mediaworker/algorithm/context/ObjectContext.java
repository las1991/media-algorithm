package com.sengled.mediaworker.algorithm.context;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.sengled.media.interfaces.YUVImage;
import com.sengled.mediaworker.algorithm.ImageUtils;
import com.sengled.mediaworker.algorithm.ObjectType;
import com.sengled.mediaworker.algorithm.decode.KinesisFrameDecoder.Data;
import com.sengled.mediaworker.algorithm.decode.KinesisFrameDecoder.ObjectConfig;
import com.sengled.mediaworker.algorithm.service.dto.MotionFeedResult;
import com.sengled.mediaworker.algorithm.service.dto.MotionFeedResult.ZoneInfo;
import com.sengled.mediaworker.algorithm.service.dto.ObjectRecognitionResult;
import com.sengled.mediaworker.algorithm.service.dto.ObjectRecognitionResult.Object;

/**
 * 物体识别上下文
 * 
 * @author media-liwei
 *
 */
public class ObjectContext extends Context {
	private static final Logger LOGGER = LoggerFactory.getLogger(ObjectContext.class);
	private static final Long objectIntervalTimeMsce = 30000L;// 物体识别间隔
	private static final int percent = 20;// zone 和物体识别次百分比
	private static final int objectAndMotionIntersectionPct = 70;// zone
																		// 和物体识别次百分比

	private String token;
	private Long lastObjectTimestamp;
	private Date utcDateTime;
	private byte[] nalData;
	private YUVImage yuvImage;
	private ObjectConfig objectConfig;

	public ObjectContext(StreamingContext context) {
		this.token = context.getToken();
		this.utcDateTime = context.getUtcDateTime();
		this.nalData = context.getNalData();
		this.yuvImage = context.getYuvImage();
		this.objectConfig = context.getConfig().getObjectConfig();
	}

	public void setLastObjectTimestamp(Long lastObjectTimestamp) {
		this.lastObjectTimestamp = lastObjectTimestamp;
	}

	public boolean isSkip() {
		boolean skip = false;
		Date utcDateTime = getUtcDateTime();
		LOGGER.info("lastObjectTimestamp:{},utcDateTime:{}",lastObjectTimestamp,utcDateTime);
		if (lastObjectTimestamp != null && utcDateTime != null) {
			long sinceLastMotion = (utcDateTime.getTime() - lastObjectTimestamp.longValue());

			if (sinceLastMotion <= objectIntervalTimeMsce) {
				LOGGER.info("Token:{},Since last time object:{} msec <= {} msec isSkip=true.", token,
						sinceLastMotion, objectIntervalTimeMsce);
				skip = true;
			} else {
				lastObjectTimestamp = null;
				LOGGER.info("Token:{},Since last time object:{} msec > {} msec .isSkip=false.", token, sinceLastMotion,
						objectIntervalTimeMsce);
				skip = false;
			}
		}
		return skip;
	}

	public Date getUtcDateTime() {
		return utcDateTime;
	}

	/**
	 * 物体识别处理
	 * 
	 * @param processorManager
	 *            用于encode
	 * @param httpclient
	 * @param objectRecognitionUrl
	 *            请求物体识别服务URL
	 * @param token
	 * @param nal
	 * @param yuvImage
	 * @param objectConfig
	 *            物体识别配置
	 * @param motionFeedResult
	 *            Motion结果
	 * @return
	 */
	public Multimap<Integer, Object>  match( String responseText, MotionFeedResult motionFeedResult) {

		JSONObject jsonObj = JSONObject.parseObject(responseText);
		if (jsonObj.getJSONArray("objects").isEmpty()) {
			LOGGER.info("object recognition NORESULT.");
			return null;
		}

		ObjectRecognitionResult objectResult = JSONObject.toJavaObject(jsonObj, ObjectRecognitionResult.class);
		LOGGER.info("recognition object JSON result:{},javaBean Result{}", jsonObj.toJSONString(),
				objectResult.toString());
		if (objectResult == null || objectResult.objects == null || objectResult.objects.isEmpty()) {
			LOGGER.info("object recognition objectResult empty.");
			return null;
		}
	
		return  filter(token, yuvImage, objectConfig, objectResult, motionFeedResult);		
	}

	private Multimap<Integer, Object> filter(String token, YUVImage yuvImage, ObjectConfig objectConfig,
			ObjectRecognitionResult objectRecognitionResult, MotionFeedResult motionFeedResult) {
		LOGGER.info("Token:{},objectConfig:{},ObjectRecognitionResult:{},MotionFeedResult:{}", token,
				JSONObject.toJSON(objectConfig), JSONObject.toJSON(objectRecognitionResult),
				JSONObject.toJSON(motionFeedResult));
		
		Multimap<Integer, Object> objectsResult = step1Filter(objectRecognitionResult, objectConfig);
		Multimap<Integer, Object> objectMatchResult = step2Filter(motionFeedResult, objectsResult);
		LOGGER.info("Match result:{}",objectMatchResult);
		return objectMatchResult;
	}

	/**
	 * Motion结果 与Object相交
	 * @param motionFeedResult
	 * @param objectsResult
	 * @return
	 */
	private Multimap<Integer, Object> step2Filter(MotionFeedResult motionFeedResult,Multimap<Integer, Object> objectsResult) {

		Multimap<Integer, Object> result = ArrayListMultimap.create();
		
		Multimap<Integer, List<Integer>> motionZoneidToBox = ArrayListMultimap.create();
		for (ZoneInfo motion : motionFeedResult.motion) {
			for (List<Integer> box : motion.boxs) {
				motionZoneidToBox.put(motion.zone_id, box);
			}
		}

		Map<Integer, Collection<Object>> objectZoneidToBox = objectsResult.asMap();
		for (Entry<Integer, Collection<Object>> objectZoneidToBoxSet : objectZoneidToBox.entrySet()) {// zone
			int zoneid = objectZoneidToBoxSet.getKey();

			Collection<Object> objectList = objectZoneidToBoxSet.getValue();
			Collection<List<Integer>> motionList = motionZoneidToBox.get(zoneid);

			for (List<Integer> motionBox : motionList) {
				for (Object object : objectList) {
					List<Integer> objectBox = object.bbox_pct;
					int area = ImageUtils.area(objectBox, motionBox);
					float  areaPercentObject= ImageUtils.areaPercent(objectBox, area);
					float areaPercentMotion = ImageUtils.areaPercent(motionBox, area);
					if (areaPercentObject >= objectAndMotionIntersectionPct || areaPercentMotion>= objectAndMotionIntersectionPct) {
						LOGGER.info("step2Filter zoneid:{} object:{}", zoneid, object.type);
						result.put(zoneid,  object);
					}
				}
			}
		}
		return result;
	}

	/**
	 * step 1  Zone与Object相交
	 * @param objectRecognitionResult
	 * @param objectConfig
	 * @return  Multimap<Integer, Object> zoneid->Object
	 */
	private Multimap<Integer, Object> step1Filter(ObjectRecognitionResult objectRecognitionResult,ObjectConfig objectConfig) {
		Multimap<Integer, Object> finalObjectsResult = ArrayListMultimap.create();//zoneid->Object
		for (Object object : objectRecognitionResult.objects) {
			String objectType = object.type;
			List<Integer> objectBox = object.bbox_pct;
			// zone
			List<Data> dataList = objectConfig.getDataList();// zone 配置
			for (Data data : dataList) {
				String posTmp = data.getPos();
				String objectTypes = data.getObjectList();
				// 判断与物体识别结果相交
				List<Integer> zoneBox = ImageUtils.convert2spotLocation(posTmp);
				int area = ImageUtils.area(objectBox, zoneBox);
				float objectBoxPct = ImageUtils.areaPercent(objectBox, area);
				float zoneBoxpercent = ImageUtils.areaPercent(zoneBox, area);
				LOGGER.info("object:{} data:{}  intersection area:{} objectBoxPct:{}, zoneBoxpercent:{}", object, data, area,objectBoxPct, zoneBoxpercent);
				if (objectBoxPct >= percent && objectTypes.contains(ObjectType.findByName(objectType).value + "")) {
					finalObjectsResult.put(data.getId(), object);
					LOGGER.info("step1Filter zoneid:{} object:{}", data.getId(), object);
				}
			}
		}
		return finalObjectsResult;
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

	public ObjectConfig getObjectConfig() {
		return objectConfig;
	}

	public void setObjectConfig(ObjectConfig objectConfig) {
		this.objectConfig = objectConfig;
	}

}
