package com.sengled.mediaworker.algorithm;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.sengled.media.interfaces.YUVImage;
import com.sengled.mediaworker.algorithm.ProcessorManager;
import com.sengled.mediaworker.algorithm.decode.KinesisFrameDecoder.ObjectConfig;
import com.sengled.mediaworker.algorithm.service.dto.MotionFeedResult;
import com.sengled.mediaworker.algorithm.service.dto.ObjectRecognitionResult;
import com.sengled.mediaworker.algorithm.service.dto.MotionFeedResult.ZoneInfo;
import com.sengled.mediaworker.algorithm.service.dto.ObjectRecognitionResult.Object;

@Component
public class RectangleUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger(RectangleUtils.class);
	
	@Autowired
	ProcessorManager processorManager;

	/**
	 * 百分比转换像素
	 * @param width  像素
	 * @param height 像素
	 * @param pos    逗号分隔字符串   "0,0,100,100"
	 * @return
	 */
	public static String  convert(int width,int height,String pos){
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
	 * @param   逗号分隔字符串   "0,0,100,100"
	 * @return 
	 */
	public static String convert2spotLocation(String posStr){
		String[] pos  = posStr.split(",");
		int x = Integer.valueOf(pos[0]);
		int y = Integer.valueOf(pos[1]);
		int xx = Integer.valueOf(pos[2]) + x;
		int yy = Integer.valueOf(pos[3]) + y;
		return x+","+y+","+xx+","+yy;
	}
	/**
	 * 求矩形相交面积
	 */
	public static int area(String[] r1,String[] r2){
		if(r1.length <4 || r2.length < 4){
			return 0;
		}
		int x = Integer.valueOf(r1[0]);
		int y = Integer.valueOf(r1[1]);
		int x1 = Integer.valueOf(r1[2]);
		int y1 = Integer.valueOf(r1[3]);
		int a = Integer.valueOf(r2[0]);
		int b = Integer.valueOf(r2[1]);
		int a1= Integer.valueOf(r2[2]);
		int b1= Integer.valueOf(r2[3]);
		boolean bool = (x1  > a &&     a1 > x &&    y1 > b &&   b1 > y);
		if(! bool){
			return 0;
		}
		int width=Math.min(x1,a1)-Math.max(x, a);
		int height=Math.min(y1, b1)-Math.max(y, b);
		return  (width * height);
	}
	
	/**
	 * 求面积area占矩形rectangle的百分比
	 */
	public static float areaPercent(String[] rectangle,float area){
		if(rectangle.length < 4){
			return 0;
		}
		float x = Float.valueOf(rectangle[0]);
		float y = Float.valueOf(rectangle[1]);
		float x1 = Float.valueOf(rectangle[2]);
		float y1 = Float.valueOf(rectangle[3]);
		float width = x1 -x;
		float height = y1 - y;
		return area/(width * height) * 100;
	}

	/**
	 * 画矩形
	 * @param token
	 * @param yuvImage
	 * @param objectConfig
	 * @param objectRecognitionResult
	 * @param motionFeedResult
	 */
	public void draw(String token,YUVImage yuvImage,ObjectConfig objectConfig ,ObjectRecognitionResult objectRecognitionResult,MotionFeedResult motionFeedResult){
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
}
}
