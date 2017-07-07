package com.sengled.mediaworker.algorithm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.AsyncEventBus;
import com.sengled.media.interfaces.YUVImage;
import com.sengled.media.interfaces.exceptions.EncodeException;
import com.sengled.mediaworker.RecordCounter;
import com.sengled.mediaworker.algorithm.context.ObjectContext;
import com.sengled.mediaworker.algorithm.context.ObjectContextManager;
import com.sengled.mediaworker.algorithm.decode.KinesisFrameDecoder.Data;
import com.sengled.mediaworker.algorithm.decode.KinesisFrameDecoder.ObjectConfig;
import com.sengled.mediaworker.algorithm.event.ObjectEvent;
import com.sengled.mediaworker.algorithm.service.ObjectEventHandler;
import com.sengled.mediaworker.algorithm.service.dto.MotionFeedResult;
import com.sengled.mediaworker.algorithm.service.dto.MotionFeedResult.ZoneInfo;
import com.sengled.mediaworker.algorithm.service.dto.ObjectRecognitionResult;
import com.sengled.mediaworker.algorithm.service.dto.ObjectRecognitionResult.Object;
import com.sengled.mediaworker.httpclient.HttpResponseResult;
import com.sengled.mediaworker.httpclient.IHttpClient;

@Component
public class ObjectRecognitionImpl implements ObjectRecognition,InitializingBean{
	private static final Logger LOGGER = LoggerFactory.getLogger(ObjectRecognitionImpl.class);
	
	@Value("${object.zone.intersection.pct}")
	private  int objectAndZoneIntersectionPct;// zone 和物体识别次百分比
	
	@Value("${object.motion.intersection.pct}")
	private  int objectAndMotionIntersectionPct;// zone // 和物体识别百分比
	
	@Value("${object.thread.count}")
	private  int threadNum;
	
	private static final int EVENT_BUS_THREAD_COUNT = 100;
	
	@Value("http://${OBJECT.RECOGNITION.HOST}:${OBJECT.RECOGNITION.PORT}${OBJECT.RECOGNITION.PATH}")
	private String objectRecognitionUrl;
	
    @Value("${object.interval.time.msce}")
    private Long objectIntervalTimeMsce;
    
    @Value("${debug.image.save.path}")
    private String debugImageSavePath;
	@Autowired
	IHttpClient httpclient;
	@Autowired
	ProcessorManager  processorManager;
	@Autowired
	private ObjectEventHandler objectEventHandler;
	@Autowired
	RecordCounter recordCounter;
	@Autowired
	ObjectContextManager objectContextManager;

	
	private List<ExecutorService> executors;
	private AsyncEventBus eventBus;
	
	
	@Override
	public void afterPropertiesSet() throws Exception {
		LOGGER.info("Initializing...");
		try {
			initialize();
		} catch (Exception e) {
			LOGGER.error(e.getMessage(),e);
			System.exit(1);
		}		
	}
	
	private void initialize() {
		LOGGER.info("ObjectRecognition init. EVENT_BUS_THREAD_COUNT:{}",EVENT_BUS_THREAD_COUNT);
		try {
			executors = new ArrayList<ExecutorService>(threadNum);
			for (int i=0;i<threadNum;i++) {
				executors.add(Executors.newSingleThreadExecutor()); 
			}
			eventBus = new AsyncEventBus(Executors.newFixedThreadPool(EVENT_BUS_THREAD_COUNT));
			eventBus.register(objectEventHandler);
		} catch (Exception e) {
			LOGGER.error(e.getMessage(),e);
			System.exit(1);
		}
	}

	
	@Override
	public Future<?> submit(String token,ObjectConfig oc, Date utcDate, YUVImage yuvImage, byte[] nalData, MotionFeedResult mfr) {
		//hash token 提交到指定线程队列中
		int hashCode = Math.abs(token.hashCode());
		int threadIndex = hashCode % threadNum;
		LOGGER.debug("Token:{} submit objectRecognition threadPool. threadIndex:{}",token,threadIndex);
		
		return executors.get(threadIndex).submit(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				try {
					handle(token,oc,utcDate,yuvImage,nalData,mfr);
				} catch (Exception e) {
					LOGGER.error(e.getMessage(),e);
				}
				return null;
			}
		});
	}
	
	private void handle(String token, ObjectConfig oc,Date utcDate, YUVImage yuvImage, byte[] nalData, MotionFeedResult mfr)throws Exception{
		LOGGER.debug("Run objectRecognition. token:{},ObjectConfig:{},MotionFeedResult:{}",token,oc,mfr);
		if(oc ==null || utcDate==null || yuvImage==null || nalData==null || mfr == null){
			LOGGER.error("parameter error.");
			return;
		}
		ObjectContext objectContext = objectContextManager.findOrCreateStreamingContext(token,utcDate,oc);
		if (objectContext.isSkip(objectIntervalTimeMsce)) {
			return;
		}

		long startTime = System.currentTimeMillis();
		HttpEntity putEntity = new ByteArrayEntity(nalData);
		HttpResponseResult result = httpclient.put(objectRecognitionUrl, putEntity);
		
		long objectCost = System.currentTimeMillis() - startTime;
		recordCounter.updateObjectSingleDataProcessCost(objectCost);
		LOGGER.info("Process ObjectPut finished.Cost:{}",objectCost);
		
		//请求Object服务
		if ( ! result.responseOk() && StringUtils.isNotBlank(result.getBody())) {
			LOGGER.error("object recognition HttpResponseResult{}", result);
			return ;
		}

		//Object 响应结果
		JSONObject objectResponseBodyJson = JSONObject.parseObject(result.getBody());
		if (null == objectResponseBodyJson || objectResponseBodyJson.getJSONArray("objects").isEmpty()) {
			LOGGER.info("object recognition NORESULT.");
			return ;
		}
		
		//Object 响应结果 匹配zoneinfo 匹配motion 
		ObjectRecognitionResult objectResult = JSONObject.toJavaObject(objectResponseBodyJson, ObjectRecognitionResult.class);
		Multimap<Integer, Object>  matchResult = match(token, yuvImage, objectResult, oc, mfr);
		LOGGER.info("Token:{},ObjectRecognition  Cost:{}", token,(System.currentTimeMillis() - startTime));
		
		byte[] jpgData = null;
		if( null != matchResult && ! matchResult.isEmpty() ){
			try {
				jpgData = processorManager.encode(token, yuvImage.getYUVData(), yuvImage.getWidth(), yuvImage.getHeight(), yuvImage.getWidth(), yuvImage.getHeight());
			} catch (EncodeException e) {
				LOGGER.error(e.getMessage(),e);
				return;
			}
		}else{
			LOGGER.info("Token:{},Object Match Result is NULL",token);
		}
		
		//FIXME 画矩形框
		if (LOGGER.isDebugEnabled()) {
			try {
				if( null == jpgData){
					jpgData = processorManager.encode(token, yuvImage.getYUVData(), yuvImage.getWidth(), yuvImage.getHeight(), yuvImage.getWidth(), yuvImage.getHeight());
				}
				jpgData = ImageUtils.draw(token, utcDate,jpgData, yuvImage, oc, mfr, objectResult,matchResult,debugImageSavePath);
			} catch (Exception e) {
				LOGGER.error(e.getMessage(),e);
				return;
			}
		}
		if( null != matchResult && ! matchResult.isEmpty() ){
			ObjectEvent event = new ObjectEvent(token,matchResult,jpgData,objectContext.getUtcDateTime());
			eventBus.post(event );
			objectContext.setLastObjectTimestamp(objectContext.getUtcDateTime().getTime());	
		}
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
	public Multimap<Integer, Object>  match(String token,YUVImage yuvImage, ObjectRecognitionResult objectRecognitionResult, ObjectConfig objectConfig,MotionFeedResult motionFeedResult) {
	
		LOGGER.debug("Token:{},Match objectConfig:{},ObjectRecognitionResult:{},MotionFeedResult:{}", token,
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
		Set<Object> hasObjSet = new HashSet<>();
		for (Entry<Integer, Collection<Object>> objectZoneidToBoxSet : objectZoneidToBox.entrySet()) {// zone
			int zoneid = objectZoneidToBoxSet.getKey();

			Collection<Object> objectList = objectZoneidToBoxSet.getValue();
			Collection<List<Integer>> motionList = motionZoneidToBox.get(zoneid);
			for (Object object : objectList) {
				List<Integer> objectBox = object.bbox_pct;
				int areaSum = 0;
				for (List<Integer> motionBox : motionList) {
					int area = ImageUtils.area(objectBox, motionBox);
					areaSum += area;
					float  areaPercentObject= ImageUtils.areaPercent(objectBox, area);
					float areaPercentMotion = ImageUtils.areaPercent(motionBox, area);
					if (areaPercentObject >= objectAndMotionIntersectionPct && areaPercentMotion>= objectAndMotionIntersectionPct) {
					//if (areaPercentObject >= objectAndMotionIntersectionPct || areaPercentMotion>= objectAndMotionIntersectionPct) {
					//if (areaPercentObject >= objectAndMotionIntersectionPct ) {
						LOGGER.debug("step2Filter zoneid:{} object:{}", zoneid, object.type);
						if( ! hasObjSet.contains(object)){
							result.put(zoneid,  object);
							hasObjSet.add(object);
						}
					}
				}
				float  areaSumPercentObject= ImageUtils.areaPercent(objectBox, areaSum);
				if( areaSumPercentObject >= objectAndMotionIntersectionPct){
					LOGGER.debug("step2Filter object and motion intersection area sum:{} ,zoneid:{}",areaSumPercentObject,zoneid);
					if( ! hasObjSet.contains(object)){
						result.put(zoneid,  object);
						hasObjSet.add(object);
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
				String objectTypes = data.getObjectList();
				// 判断与物体识别结果相交
				List<Integer> pos = data.getPosList();
				List<Integer> zoneBox = ImageUtils.convert2spotLocation(pos);
				int area = ImageUtils.area(objectBox, zoneBox);
				float objectBoxPct = ImageUtils.areaPercent(objectBox, area);
				float zoneBoxpercent = ImageUtils.areaPercent(zoneBox, area);
				LOGGER.debug("object:{} data:{}  intersection area:{} objectBoxPct:{}, zoneBoxpercent:{}", object, data, area,objectBoxPct, zoneBoxpercent);
				if (objectBoxPct >= objectAndZoneIntersectionPct && objectTypes.contains(ObjectType.findByName(objectType).value + "")) {
					finalObjectsResult.put(data.getId(), object);
					LOGGER.debug("step1Filter zoneid:{} object:{}", data.getId(), object);
				}
			}
		}
		return finalObjectsResult;
	}
}
