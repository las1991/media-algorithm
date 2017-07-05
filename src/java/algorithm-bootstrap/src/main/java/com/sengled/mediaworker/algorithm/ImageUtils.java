package com.sengled.mediaworker.algorithm;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;

import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Multimap;
import com.sengled.media.interfaces.YUVImage;
import com.sengled.mediaworker.algorithm.decode.KinesisFrameDecoder.ObjectConfig;
import com.sengled.mediaworker.algorithm.service.dto.MotionFeedResult;
import com.sengled.mediaworker.algorithm.service.dto.MotionFeedResult.ZoneInfo;
import com.sengled.mediaworker.algorithm.service.dto.ObjectRecognitionResult;
import com.sengled.mediaworker.algorithm.service.dto.ObjectRecognitionResult.Object;

public class ImageUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger(ImageUtils.class);

	/**
	 * 百分比转换像素
	 * 
	 * @param width
	 *            像素
	 * @param height
	 *            像素
	 * @param pos
	 *            逗号分隔字符串 "0,0,100,100"
	 * @return
	 */
	public static List<Integer> convertPctToPixel(int width, int height, List<Integer> posList) {
		if(null == posList || posList.isEmpty()){
			return Collections.emptyList();
		}
		int x = posList.get(0),y = posList.get(1);
		int xx = posList.get(2),yy = posList.get(3);
		int px = (x * width) / 100,py=(y * height) / 100;
		int pxx = (xx * width) / 100 ,pyy = (yy * height) / 100;
		return  Arrays.asList(px,py,pxx,pyy);
	}

	/**
	 * 长宽标记 转换为2点定位
	 * 
	 * @param 逗号分隔字符串
	 *            "0,0,100,100"
	 * @return
	 */
	public static List<Integer> convert2spotLocation(List<Integer>  posList) {
		if(null == posList || posList.isEmpty()){
			return Collections.emptyList();
		}
		int x = posList.get(0);
		int y = posList.get(1);
		int xx = posList.get(2) + x;
		int yy = posList.get(3) + y;
		posList.clear();
		posList.add(x);  posList.add(y);
		posList.add(xx); posList.add(yy);
		return posList;
	}

	/**
	 * 求矩形相交面积
	 */
	public static int area(List<Integer> r1, List<Integer> r2) {
		if (r1.size() < 4 || r2.size() < 4) {
			return 0;
		}
		int x = r1.get(0), y = r1.get(1), x1 = r1.get(2), y1 = r1.get(3);
		int a = r2.get(0), b = r2.get(1), a1 = r2.get(2), b1 = r2.get(3);

		boolean bool = (x1 > a && a1 > x && y1 > b && b1 > y);
		if (!bool) {
			return 0;
		}
		int width = Math.min(x1, a1) - Math.max(x, a);
		int height = Math.min(y1, b1) - Math.max(y, b);
		return (width * height);
	}
	/**
	 * 求面积area占矩形rectangle的百分比
	 */
	public static float areaPercent(List<Integer> rectangle, float area) {
		if (null == rectangle || rectangle.size() != 4) {
			return 0;
		}
		float x = Float.valueOf(rectangle.get(0));
		float y = Float.valueOf(rectangle.get(1));
		float x1 = Float.valueOf(rectangle.get(2));
		float y1 = Float.valueOf(rectangle.get(3));
		float width = x1 - x;
		float height = y1 - y;
		try {
			return area / (width * height) * 100;
		} catch (Exception e) {
			LOGGER.error(e.getMessage(),e);
			return 0;
		}
	}

	
	public static byte[] drawRect(List<List<Integer>> boxs,Color color,byte[] jpgData){
		BufferedImage img;
		try {
			img = ImageIO.read(new ByteArrayInputStream(jpgData));
			BufferedImage r = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
			r.getGraphics().drawImage(img, 0, 0, img.getWidth(), img.getHeight(), null);
			Graphics g = r.getGraphics();
			drawRect(g, boxs, color, "", 0, 0);
			ByteArrayOutputStream out = new ByteArrayOutputStream();  
            ImageIO.write(r, "jpg", out);  
            byte[] b = out.toByteArray();  			
			return b;
		} catch (Exception e) {
			LOGGER.error(e.getMessage(),e);
		}
		return null;
	}
	
	/**
	 * 画矩形
	 * 
	 * @param token
	 * @param yuvImage
	 * @param objectConfig
	 * @param objectRecognitionResult
	 * @param motionFeedResult
	 */
	public static byte[] draw(String token,Date utcDate, byte[] jpgData, YUVImage yuvImage, ObjectConfig objectConfig,
			MotionFeedResult motionFeedResult,ObjectRecognitionResult objectRecognitionResult, Multimap<Integer, Object> matchResult) {
		try {
			List<List<Integer>> objectConfigPos = new ArrayList<>();
			for (int i = 0; i < objectConfig.getDataList().size(); i++) {
				List<Integer> pos = objectConfig.getDataList().get(i).getPosList();
				List<Integer> posList = convert2spotLocation(pos);
				List<Integer> resultPos = convertPctToPixel(yuvImage.getWidth(), yuvImage.getHeight(), posList);
				objectConfigPos.add(resultPos);
			}
			List<List<Integer>> motionFeedResultPos = new ArrayList<>();
			for (ZoneInfo zi : motionFeedResult.motion) {
				for (List<Integer> z : zi.boxs) {
					List<Integer> posStr = convertPctToPixel(yuvImage.getWidth(), yuvImage.getHeight(), z);
					motionFeedResultPos.add(posStr);
				}
			}
			List<List<Integer>> objectResultPos = new ArrayList<>();
			for ( Object object : objectRecognitionResult.objects) {
				List<Integer> posStr = convertPctToPixel(yuvImage.getWidth(), yuvImage.getHeight(), object.bbox_pct);
				objectResultPos.add(posStr	);
			}
			
			List<List<Integer>> matchResultPos = new ArrayList<>();
			Map<Integer, Collection<Object>> map = matchResult.asMap();
			for (Entry<Integer, Collection<Object>> entry : map.entrySet()) {
				Collection<Object> objects = entry.getValue();
				for (Object object : objects) {
					List<Integer> posStr = convertPctToPixel(yuvImage.getWidth(), yuvImage.getHeight(), object.bbox_pct);
					matchResultPos.add(posStr);
				}
			}

			BufferedImage img = ImageIO.read(new ByteArrayInputStream(jpgData));
			BufferedImage r = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
			r.getGraphics().drawImage(img, 0, 0, img.getWidth(), img.getHeight(), null);
			Graphics g = r.getGraphics();
			drawRect(g, objectConfigPos, Color.GREEN, "ZoneInfo", 30, 30);
			drawRect(g, motionFeedResultPos, Color.yellow, "MOTION", 30, 50);
			drawRect(g, objectResultPos, Color.red, "OBJECT", 30, 70);
			drawRect(g, matchResultPos, Color.blue, "Result", 30, 90);
			ByteArrayOutputStream out = new ByteArrayOutputStream();  
            ImageIO.write(r, "jpg", out);  
            byte[] b = out.toByteArray();
            out.close();
//			String imageFileName = token + "_" + DateFormatUtils.format(utcDate, "yyyy-MM-dd-HH_mm_ss_SSS") + ".jpg";
//			File file = new File("/root/save/" +imageFileName );
//			ImageIO.write(r, "jpg", file);
//			FileInputStream input = new FileInputStream(file);
//			byte[] buffer = new byte[(int) file.length()];
//			input.read(buffer);
//			input.close();
			LOGGER.debug("Token:{} draw  MotionFeedResult:{} matchResult:{}",token,motionFeedResult,matchResult);
			return b;
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			return null;
		}
	}

	private static void drawRect(Graphics g, List<List<Integer>> boxs, Color color, String word, int wx, int wy) {
		for (List<Integer> pos : boxs) {
			int x = pos.get(0);
			int y = pos.get(1);
			int xx = pos.get(2);
			int yy = pos.get(3);
			Graphics2D g2d = (Graphics2D)g;
			g2d.setStroke(new BasicStroke(3));
			
			g2d.setColor(color);
			g2d.drawRect(x, y, xx - x, yy - y);
			g2d.drawString(word, wx, wy);
		}
	}

	public static void main(String[] args) throws IOException {
		BufferedImage img = ImageIO.read(new File("D://test/1.jpg"));

		BufferedImage r = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
		r.getGraphics().drawImage(img, 0, 0, img.getWidth(), img.getHeight(), null);

		r.getGraphics().drawString("hello world", 20, 20);

		Graphics g = r.getGraphics();
		g.setColor(Color.RED);
		Graphics2D g2d = (Graphics2D)g;
		g2d.setStroke(new BasicStroke(3));
		
		g2d.drawRect(0, 0, 100, 100);
		

		ImageIO.write(r, "jpg", new File("D://test/1.chenxh.jpg"));

	}
}
