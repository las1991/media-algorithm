package com.sengled.mediaworker.algorithm;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.eventbus.AsyncEventBus;
import com.sengled.media.interfaces.YUVImage;
import com.sengled.media.interfaces.exceptions.EncodeException;
import com.sengled.mediaworker.RecordCounter;
import com.sengled.mediaworker.algorithm.context.AlgorithmConfigWarpper.Data;
import com.sengled.mediaworker.algorithm.context.AlgorithmConfigWarpper.ObjectConfig;
import com.sengled.mediaworker.algorithm.context.ObjectContext;
import com.sengled.mediaworker.algorithm.context.ObjectContextManager;
import com.sengled.mediaworker.algorithm.event.ObjectEvent;
import com.sengled.mediaworker.algorithm.service.ObjectEventHandler;
import com.sengled.mediaworker.algorithm.service.dto.MotionFeedResult;
import com.sengled.mediaworker.algorithm.service.dto.MotionFeedResult.ZoneInfo;
import com.sengled.mediaworker.algorithm.service.dto.ObjectRecognitionResult;
import com.sengled.mediaworker.algorithm.service.dto.ObjectRecognitionResult.TargetObject;
import com.sengled.mediaworker.algorithm.service.dto.ObjectRecognitionResultWrapper;

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
	
	@Value("${OBJECT.RECOGNITION.HOST}")
	private String objectRecognitionHost;
	
	@Value("${OBJECT.RECOGNITION.PORT}")
    private String objectRecognitionPort;
	
	@Value("${OBJECT.RECOGNITION.PATH}")
    private String objectRecognitionPath;
	
    @Value("${debug.image.save.path}")
    private String debugImageSavePath;
    
    @Value("${object.confirm.score}")
    private Double objectConfirmScore;
    
    @Autowired
    private ClientHttpRequestFactory clientHttpRequestFactory;
    
	@Autowired
	private ProcessorManager  processorManager;
	
	@Autowired
	private ObjectEventHandler objectEventHandler;
	
	@Autowired
	private RecordCounter recordCounter;
	
	@Autowired
	private ObjectContextManager objectContextManager;
	
	private RestTemplate restTemplate;

	private List<ExecutorService> executors;
	
	private AsyncEventBus eventBus;
	
	private String objectRecognitionUrl;
	
	
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
	    String scheme;
	    if( "443".equals(objectRecognitionPort.trim())){
	        scheme = "https://";
	    }else{
	        scheme = "http://";
	    }
	    objectRecognitionUrl = scheme + objectRecognitionHost +":"+ objectRecognitionPort + objectRecognitionPath;
	    
		LOGGER.info("ObjectRecognition init. EVENT_BUS_THREAD_COUNT:{},objectRecognitionUrl:{}",EVENT_BUS_THREAD_COUNT,objectRecognitionUrl);
		
		restTemplate = new RestTemplate(clientHttpRequestFactory);
		try {
			executors = new ArrayList<ExecutorService>(threadNum);
			for (int i=0;i<threadNum;i++) {
				ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(10);
	            ThreadPoolExecutor pool = new ThreadPoolExecutor(1, 1,0L, TimeUnit.MILLISECONDS,queue,new ThreadPoolExecutor.AbortPolicy());
				executors.add(pool); 
			}
			eventBus = new AsyncEventBus(Executors.newFixedThreadPool(EVENT_BUS_THREAD_COUNT));
			eventBus.register(objectEventHandler);
		} catch (Exception e) {
			LOGGER.error(e.getMessage(),e);
			System.exit(1);
		}
	}


    @Override
    public void submit(final String tokenMask, 
            final ObjectConfig finalObjectConfig, 
            final Date finalUtcDate, 
            final Map<Integer, YUVImage> finalYUVmageMap, 
            final byte[] nalData,
            final int finalFileExpiresHours, 
            final Map<Integer, MotionFeedResult> finalMotionFeedResultMap) {
        //hash token 提交到指定线程队列中
        int hashCode = Math.abs(tokenMask.hashCode());
        int threadIndex = hashCode % threadNum;
        LOGGER.debug("tokenMask:{} submit objectRecognition threadPool. threadIndex:{}",tokenMask,threadIndex);
        
        
        ExecutorService thread = executors.get(threadIndex);
        try {
            thread.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                        handle2(tokenMask,finalObjectConfig,finalUtcDate,finalYUVmageMap,nalData,finalFileExpiresHours,finalMotionFeedResultMap);
                    return null;
                }
            });
        } catch (Exception e) {
            LOGGER.error("ThreadPool AbortPolicy");
            LOGGER.error(e.getMessage(),e);
        }
    }
	

    private void handle2(final String tokenMask, final ObjectConfig objectConfig, final Date utcDate, final Map<Integer, YUVImage> copyYUVmageMap, final byte[] nalData,
            int fileExpiresHours, Map<Integer, MotionFeedResult> motionFeedResultMap) {
        ObjectContext objectContext = objectContextManager.findOrCreateStreamingContext(tokenMask,utcDate,objectConfig);
        
       //请求物体识别
        ObjectRecognitionResult objectResult = null;
        try {
            objectResult = requestObjectService(tokenMask,objectRecognitionUrl,nalData);
        } catch (Exception e) {
            LOGGER.error("RequestObjectService Exception "+e.getMessage(),e);
            LOGGER.info("[{}], ObjectConfig:{} utcDate:{},fileExpiresHours:{},motionFeedResultMap:{}",tokenMask, objectConfig,utcDate, motionFeedResultMap);
        }
        if( null == objectResult ){
            return;
        }
        ObjectRecognitionResultWrapper wrapper = new ObjectRecognitionResultWrapper(objectResult);
        
        Multiset<Integer> frameIndexSet = wrapper.getMultiMap().keys();
        
        for (Integer frameIndex : frameIndexSet) {
            //物体识别 与 移动检测 匹配
            YUVImage yuvImage =  copyYUVmageMap.get(frameIndex);
            MotionFeedResult mfr =  motionFeedResultMap.get(frameIndex);
            ObjectRecognitionResult objectRecognitionResult = wrapper.getObjectRecognitionResult(frameIndex);
            Multimap<Integer, TargetObject>  matchResult = match(tokenMask, yuvImage, objectRecognitionResult, objectConfig, mfr);
            if( null != matchResult && ! matchResult.isEmpty() ){
                postObjectEvent(tokenMask, objectConfig, utcDate, fileExpiresHours, objectContext, objectResult, yuvImage, mfr, matchResult);
                LOGGER.info("[{}], Object matchResult :{} frameIndex:{},",tokenMask, matchResult,frameIndex);
                LOGGER.info("[{}], ObjectConfig:{} utcDate:{},fileExpiresHours:{},motionFeedResultMap:{}",tokenMask, objectConfig,utcDate, motionFeedResultMap);
                break;
            }   
        }
    }
    
    private void postObjectEvent(final String tokenMask, final ObjectConfig objectConfig, final Date copyUtcDate, int fileExpiresHours,
        ObjectContext objectContext, final ObjectRecognitionResult objectResult, YUVImage yuvImage, MotionFeedResult mfr,
        Multimap<Integer, TargetObject> matchResult) {
        //decode to jpg
        byte[] jpgData = encodeJpg(tokenMask, yuvImage);
          
        //DEBUG 画矩形框
        if (LOGGER.isDebugEnabled()) {
            byte[] drawJpg = drawGraphical(tokenMask, objectConfig, copyUtcDate, yuvImage, mfr, objectResult, matchResult, jpgData);
            jpgData = drawJpg != null ? drawJpg:jpgData;
        }
          
        //post event
        ObjectEvent event = new ObjectEvent(tokenMask,matchResult,jpgData,fileExpiresHours,objectContext.getUtcDateTime());
        eventBus.post( event );
        MotionAndObjectReportManager.markObjectEvent(tokenMask.split(",")[0], DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
    }
    
    private byte[] encodeJpg(final String token, final YUVImage yuvImage) {
		try {
			return  processorManager.encode(token, yuvImage.getYUVData(), yuvImage.getWidth(), yuvImage.getHeight(), yuvImage.getWidth(), yuvImage.getHeight());
		} catch (EncodeException e) {
			LOGGER.error(e.getMessage(),e);
			return null;
		}
    }

    private ObjectRecognitionResult requestObjectService(String token,String objectRecognitionUrl2, byte[] nalData) throws URISyntaxException {
        long startTime = System.currentTimeMillis();
        RequestEntity<byte[]> requestEntity = new RequestEntity<>(nalData, HttpMethod.PUT, new URI(objectRecognitionUrl));
        ResponseEntity<ObjectRecognitionResult> response = restTemplate.exchange(requestEntity, ObjectRecognitionResult.class);
        long objectCost = System.currentTimeMillis() - startTime;
        recordCounter.updateObjectSingleDataProcessCost(objectCost);
        LOGGER.info("Process ObjectPut finished.Cost:{}",objectCost);
        
        //请求Object服务
        if( null == response || !response.getStatusCode().is2xxSuccessful() ) {
            LOGGER.error("Token:{},access object url:{}, error",token,objectRecognitionUrl);
            recordCounter.addAndGetObjectErrorCount(1);
            return null;
        }
        LOGGER.info("Token:{},access object result:{}",token,response.getBody());
        
        return response.getBody();
    }

    private byte[] drawGraphical(final String token, final ObjectConfig oc, final Date utcDate, final YUVImage yuvImage, final MotionFeedResult mfr,
            ObjectRecognitionResult objectResult, Multimap<Integer, TargetObject> matchResult, byte[] jpgData) {
        try {
        	if( null == jpgData){
        		jpgData = processorManager.encode(token, yuvImage.getYUVData(), yuvImage.getWidth(), yuvImage.getHeight(), yuvImage.getWidth(), yuvImage.getHeight());
        	}
        	jpgData = ImageUtils.draw(token, utcDate,jpgData, yuvImage, oc, mfr, objectResult,matchResult,debugImageSavePath);
        } catch (Exception e) {
        	LOGGER.error(e.getMessage(),e);
        	return null;
        }
        return jpgData;
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
	public Multimap<Integer, TargetObject>  match(String token,YUVImage yuvImage, ObjectRecognitionResult objectRecognitionResult, ObjectConfig objectConfig,MotionFeedResult motionFeedResult) {
	
		LOGGER.debug("Token:{},Match objectConfig:{},ObjectRecognitionResult:{},MotionFeedResult:{}", token,
				JSONObject.toJSON(objectConfig), JSONObject.toJSON(objectRecognitionResult),
				JSONObject.toJSON(motionFeedResult));
		
		Multimap<Integer, TargetObject> objectsResult = step1Filter(objectRecognitionResult, objectConfig);
		Multimap<Integer, TargetObject> objectMatchResult = step2Filter(motionFeedResult, objectsResult);
		LOGGER.info("Match result:{}",objectMatchResult);
		return objectMatchResult;	
	}

	/**
	 * Motion结果 与Object相交
	 * @param motionFeedResult
	 * @param objectsResult
	 * @return
	 */
	private Multimap<Integer, TargetObject> step2Filter(MotionFeedResult motionFeedResult,Multimap<Integer, TargetObject> objectsResult) {

		Multimap<Integer, TargetObject> result = ArrayListMultimap.create();
		
		Multimap<Integer, List<Integer>> motionZoneidToBox = ArrayListMultimap.create();
		for (ZoneInfo motion : motionFeedResult.getMotion()) {
			for (List<Integer> box : motion.getBoxs()) {
				motionZoneidToBox.put(motion.getZone_id(), box);
			}
		}

		Map<Integer, Collection<TargetObject>> objectZoneidToBox = objectsResult.asMap();
		Set<TargetObject> hasObjSet = new HashSet<>();
		for (Entry<Integer, Collection<TargetObject>> objectZoneidToBoxSet : objectZoneidToBox.entrySet()) {// zone
			int zoneid = objectZoneidToBoxSet.getKey();

			Collection<TargetObject> objectList = objectZoneidToBoxSet.getValue();
			Collection<List<Integer>> motionList = motionZoneidToBox.get(zoneid);
			for (TargetObject object : objectList) {
				List<Integer> objectBox = object.getBbox_pct();
				int areaSum = 0;
				for (List<Integer> motionBox : motionList) {
					int area = ImageUtils.area(objectBox, motionBox);
					areaSum += area;
					float  areaPercentObject= ImageUtils.areaPercent(objectBox, area);
					float areaPercentMotion = ImageUtils.areaPercent(motionBox, area);
					if (areaPercentObject >= objectAndMotionIntersectionPct && areaPercentMotion>= objectAndMotionIntersectionPct) {
					//if (areaPercentObject >= objectAndMotionIntersectionPct || areaPercentMotion>= objectAndMotionIntersectionPct) {
					//if (areaPercentObject >= objectAndMotionIntersectionPct ) {
						LOGGER.debug("step2Filter zoneid:{} object:{}", zoneid, object.getType());
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
	private Multimap<Integer, TargetObject> step1Filter(ObjectRecognitionResult objectRecognitionResult,ObjectConfig objectConfig) {
		Multimap<Integer, TargetObject> finalObjectsResult = ArrayListMultimap.create();//zoneid->Object
		for (TargetObject resultObject : objectRecognitionResult.getObjects()) {
			String resultObjectType = resultObject.getType();     //eg: person|cat|cat|dog
			List<Integer> objectBox = resultObject.getBbox_pct();
			if(resultObject.getScore() < objectConfirmScore){
				continue;
			}
			
			// zone
			List<Data> dataList = objectConfig.getDataList();// zone 配置
			for (Data zoneData : dataList) {
				String objectTypesConf = zoneData.getObjectList(); //eg:1,2,3 see:ObjectType
				// 判断与物体识别结果相交
				List<Integer> pos = zoneData.getPosList();
				List<Integer> zoneBox = ImageUtils.convert2spotLocation(pos);
				int area = ImageUtils.area(objectBox, zoneBox);
				float objectBoxPct = ImageUtils.areaPercent(objectBox, area);
				float zoneBoxpercent = ImageUtils.areaPercent(zoneBox, area);
				LOGGER.debug("object:{} data:{}  intersection area:{} objectBoxPct:{}, zoneBoxpercent:{}", resultObject, zoneData, area,objectBoxPct, zoneBoxpercent);
				if (objectBoxPct >= objectAndZoneIntersectionPct && objectTypesConf.contains(ObjectType.findByName(resultObjectType).value + "")) {
				//if (objectBoxPct >= objectAndZoneIntersectionPct &&  "person".equals(objectType)) {
					finalObjectsResult.put(zoneData.getId(), resultObject);
					LOGGER.debug("step1Filter zoneid:{} object:{}", zoneData.getId(), resultObject);
				}
			}
		}
		return finalObjectsResult;
	}
}
