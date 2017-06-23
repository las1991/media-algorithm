package com.sengled.mediaworker.object;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;

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
public class DrawZoneObjectMotionFrame {
	
	@Autowired
	ProcessorManager processorManager;

	public void draw(String token,YUVImage yuvImage,ObjectConfig objectConfig ,ObjectRecognitionResult objectRecognitionResult,MotionFeedResult motionFeedResult){
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
	private static String  convert(int width,int height,String pos){
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
	private static String convert2spotLocation(String posStr){
		String[] pos  = posStr.split(",");
		int x = Integer.valueOf(pos[0]);
		int y = Integer.valueOf(pos[1]);
		int xx = Integer.valueOf(pos[2]) + x;
		int yy = Integer.valueOf(pos[3]) + y;
		return x+","+y+","+xx+","+yy;
	}
}
