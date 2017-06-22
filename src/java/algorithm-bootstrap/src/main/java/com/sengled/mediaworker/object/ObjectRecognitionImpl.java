package com.sengled.mediaworker.object;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.google.common.eventbus.AsyncEventBus;
import com.sengled.media.interfaces.YUVImage;
import com.sengled.media.interfaces.exceptions.EncodeException;
import com.sengled.mediaworker.algorithm.ProcessorManager;
import com.sengled.mediaworker.algorithm.decode.KinesisFrameDecoder.Data;
import com.sengled.mediaworker.algorithm.decode.KinesisFrameDecoder.ObjectConfig;
import com.sengled.mediaworker.algorithm.service.ObjectEventHandler;
import com.sengled.mediaworker.algorithm.service.dto.MotionFeedResult;
import com.sengled.mediaworker.algorithm.service.dto.MotionFeedResult.ZoneInfo;
import com.sengled.mediaworker.algorithm.service.dto.ObjectRecognitionResult;
import com.sengled.mediaworker.algorithm.service.dto.ObjectRecognitionResult.Object;
import com.sengled.mediaworker.httpclient.HttpResponseResult;
import com.sengled.mediaworker.httpclient.IHttpClient;

/**
 * 物体识别
 * @author media-liwei
 *
 */
@Component
public class ObjectRecognitionImpl implements InitializingBean,ObjectRecognition{
		
	private static final Logger LOGGER = LoggerFactory.getLogger(ObjectRecognitionImpl.class);
	private final static int EVENT_BUS_THREAD_COUNT = 100;
	
	@Autowired
	ObjectContextManager objectContextManager;
	@Autowired
	IHttpClient httpclient;
	@Autowired
	ObjectEventHandler objectEventHandler;
	@Autowired
	ProcessorManager  processorManager;
	
	@Value("${object.recognition.url}")
	private String objectRecognitionUrl;
	
	private AsyncEventBus eventBus;

	public void afterPropertiesSet() throws Exception {
		try {
			initialize();
		} catch (Exception e) {
			LOGGER.error(e.getMessage(),e);
			System.exit(1);
		}		
	}
	
	private void initialize(){
		LOGGER.info("ObjectRecognition init.EVENT_BUS_THREAD_COUNT:{}",EVENT_BUS_THREAD_COUNT);
		eventBus = new AsyncEventBus(Executors.newFixedThreadPool(EVENT_BUS_THREAD_COUNT));
		eventBus.register(objectEventHandler);
	}

	public String match(String token,byte[] nal,YUVImage yuvImage,ObjectConfig objectConfig,MotionFeedResult motionFeedResult) {
		ObjectContext objectContext = objectContextManager.findOrCreateStreamingContext(token);
		if(objectContext.isSkip()){
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
		
		//处理物体识别事件
		//TODO 分析结果，找出在zone中的结果，提交sqs s3

		matchZone(token,yuvImage,objectConfig,objectResult,motionFeedResult);
		
		String matchResult = "{'zone_id':54,'type':['persion','car'],'zone_id':12,'type':['persion','dog']}";
		
		return matchResult;
	}
	private void matchZone(String token,YUVImage yuvImage,ObjectConfig objectConfig ,ObjectRecognitionResult objectRecognitionResult,MotionFeedResult motionFeedResult){
		LOGGER.info("Token:{},objectConfig:{},ObjectRecognitionResult:{},MotionFeedResult:{}",token,JSONObject.toJSON(objectConfig),JSONObject.toJSON(objectRecognitionResult),JSONObject.toJSON(motionFeedResult));
		
		//TODO test  
		try {
			//posList objectConfig
			List<String> posList = new ArrayList<>();
			for(int i=0;i<objectConfig.getDataList().size();i++){
				String pos = objectConfig.getDataList().get(i).getPos();
				String posStr = convert2spotLocation(pos);
				String resultPos = convert(yuvImage.getWidth(), yuvImage.getHeight(), posStr);
				posList.add(resultPos);
			}
			//posList2  objectRecognitionResult
			List<String> posList2 = new ArrayList<>();
			for(Object obj : objectRecognitionResult.objects){
				List<String> strList = new ArrayList<>(obj.bbox_pct.size());
				for(Integer i : obj.bbox_pct){
					strList.add(String.valueOf(i));
				}
				String[] strings = new String[strList.size()];
				strList.toArray(strings);
				posList2.add(convert(yuvImage.getWidth(), yuvImage.getHeight(),String.join(",", strings)));
			}
			
			//posList3 MotionFeedResult
			List<String> posList3 = new ArrayList<>();
			for ( ZoneInfo zi : motionFeedResult.motion) {
				for( List<Integer> z : zi.boxs){
					String[] pos = {z.get(0)+"",z.get(1)+"",z.get(2)+"",z.get(3)+""};
					String posStr = convert(yuvImage.getWidth(), yuvImage.getHeight(), String.join(",", pos));
					posList3.add(posStr);
				}
			}
			
			byte[] jpgData = processorManager.encode(token, yuvImage.getYUVData(), yuvImage.getWidth(), yuvImage.getHeight(), yuvImage.getWidth(), yuvImage.getHeight());
			String filename = "/root/save/"+token+".jpg";
			File imageName = new File(filename);
			ImageOutputStream ios = ImageIO.createImageOutputStream(imageName);
			ios.write(jpgData);
			String newfile = "/root/save/"+token+System.currentTimeMillis() + ".jpg";
			String[] cmdarray = {"/usr/bin/python","/root/pang/draw.py",JSONObject.toJSONString(posList),JSONObject.toJSONString(posList2),JSONObject.toJSONString(posList3),filename,newfile};
			System.out.println((Arrays.asList(cmdarray)));
			Runtime.getRuntime().exec(cmdarray);
		} catch (Exception e) {
			e.printStackTrace();
		}
		//TODO test END
	}
	
	/**
	 * 百分比转换像素
	 * @param width
	 * @param height
	 * @param pos
	 * @return
	 */
	private String  convert(int width,int height,String pos){
		String boxs[] = pos.split(",");
		int x = Integer.valueOf(boxs[0]);
		int y = Integer.valueOf(boxs[1]);
		int xx = Integer.valueOf(boxs[2]);
		int yy = Integer.valueOf(boxs[3]);
		String[] result = {""+(x * width)/100,""+(y * height)/100,""+(xx * width)/100+","+(yy * height)/100};
		return String.join(",", result);	
	}
	/**
	 * 长宽标记 转换为2点定位
	 * @param posStr
	 * @return
	 */
	private String convert2spotLocation(String posStr){
		String[] pos  = posStr.split(",");
		int x = Integer.valueOf(pos[0]);
		int y = Integer.valueOf(pos[1]);
		int xx = Integer.valueOf(pos[2]) + x;
		int yy = Integer.valueOf(pos[3]) + y;
		return x+","+y+","+xx+","+yy;
	}
	public static void main(String[] args) {
		String  objectConfigStr = "{\"dataList\":[{\"pos\":\"29,16,50,63\",\"objectList\":\"1\",\"id\":418},{\"pos\":\"0,0,100,100\",\"objectList\":\"1\",\"id\":479},{\"pos\":\"28,50,22,15\",\"objectList\":\"1\",\"id\":597}]}";
		String objectRecognitionResultStr = "{\"objects\":[{\"bbox_pct\":[49,61,53,70],\"type\":\"person\"},{\"bbox_pct\":[9,48,30,96],\"type\":\"person\"},{\"bbox_pct\":[86,49,99,98],\"type\":\"person\"}]}";
		String motionFeedResultStr = "{\"motion\":[{\"zone_id\":418,\"boxs\":[[134,165,12,17],[6,155,17,17]]},{\"zone_id\":479,\"boxs\":[[191,212,17,17],[576,194,25,32],[113,182,44,33]]},{\"zone_id\":597,\"boxs\":[[80,44,6,7],[12,32,17,17]]}]}";

		ObjectConfig objectConfig = JSONObject.parseObject(objectConfigStr , ObjectConfig.class);
		ObjectRecognitionResult  objectRecognitionResult = JSONObject.parseObject(objectRecognitionResultStr,ObjectRecognitionResult.class);
		MotionFeedResult  motionFeedResult = JSONObject.parseObject(motionFeedResultStr,MotionFeedResult.class);
		
		
		System.out.println(objectRecognitionResult);
		System.out.println(motionFeedResult);
		
		//统一百分比
		System.out.println("before:"+objectConfig);
		List<Data> list = objectConfig.getDataList();
		for(Data  data : list){
			String posStr = data.getPos();
			String[] pos  = posStr.split(",");
			int x = Integer.valueOf(pos[0]);
			int y = Integer.valueOf(pos[1]);
			int xx = Integer.valueOf(pos[2]) + x;
			int yy = Integer.valueOf(pos[3]) + y;
			data.setPos(x+","+y+","+xx+","+yy);
		}
		System.out.println("convert percent:"+objectConfig);
		
		
		//创建map<zoneid,posList>
		Map<Integer,List<String>> map = new HashMap<>();
		for( Data data: objectConfig.getDataList()){
			map.put(data.getId(), Arrays.asList(data.getPos().split(",")));
		}

		
	}
 
}



