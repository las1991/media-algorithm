package com.sengled.mediaworker.algorithm.decode;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.sengled.mediaworker.algorithm.exception.FrameDecodeException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class KinesisFrameDecoder {	
	private static final Logger LOGGER = LoggerFactory.getLogger(KinesisFrameDecoder.class);

    public static Frame decode(byte[] data) throws FrameDecodeException {

        ByteBuf buf = Unpooled.wrappedBuffer(data);
        int firstByte = buf.readByte();
        if ('$' != firstByte) {
            throw new IllegalArgumentException("非法数据"+firstByte);
        }
        
        int jsonBytesLength = buf.readUnsignedShort();
        
        final byte[] jsonBytes = new byte[jsonBytesLength];
        final byte[] dataBytes = new byte[data.length - jsonBytesLength - 2 - 1];
        buf.readBytes(jsonBytes);
        buf.readBytes(dataBytes);
        
        try {
        	String configString = new String(jsonBytes, "UTF-8");
        	LOGGER.debug("configString:{}",configString);
        	FrameConfig frameConfig = JSONObject.parseObject(configString, FrameConfig.class);
			return new Frame(frameConfig, dataBytes);
		} catch (UnsupportedEncodingException e) {
			LOGGER.error("Frame decode error.");
			throw new FrameDecodeException(e.getMessage(),e);
		}
    }
    
    public static class Frame {
    	
    	private FrameConfig config;
        private byte[] nalData;
        
        public Frame( FrameConfig config, byte[] dataBytes) {
        	this.config = config;
            this.nalData = dataBytes;
        }

		public byte[] getNalData() {
			return nalData;
		}

		public void setNalData(byte[] yuvData) {
			this.nalData = yuvData;
		}

		public FrameConfig getConfig() {
			return config;
		}

		public void setConfig(FrameConfig config) {
			this.config = config;
		}
		
    }
    public static class FrameConfig{
    	@JSONField(name="action")
    	private String action;
    	
    	@JSONField(name="utcDateTime")
    	private String utcDateTime;
    	
    	@JSONField(name="motion")
    	private MotionConfig motionConfig;
    	
    	@JSONField(name="object")
    	private ObjectConfig objectConfig;
    	
    	
    	public MotionConfig getBaseConfig(){
    		if( null != motionConfig){
    			return motionConfig;
    		}
    		if(null != objectConfig ){
    			MotionConfig config = new MotionConfig();
    			List<Data> dataList = new ArrayList<>(objectConfig.dataList.size());
    			for ( Data data : objectConfig.dataList) {
    				Data obj = new Data();
    				obj.setId(data.getId());
    				obj.setPos(data.getPos());
    				dataList.add(obj);
				}
    			config.setDataList(dataList);
    			config.setSensitivity(objectConfig.sensitivity);
    			return config;
    		}
    		return null;
    	}
    	
		public String getAction() {
			return action;
		}
		public void setAction(String action) {
			this.action = action;
		}
		public String getUtcDateTime() {
			return utcDateTime;
		}
		public void setUtcDateTime(String utcDateTime) {
			this.utcDateTime = utcDateTime;
		}
		public MotionConfig getMotionConfig() {
			return motionConfig;
		}
		public void setMotionConfig(MotionConfig motionConfig) {
			this.motionConfig = motionConfig;
		}
		public ObjectConfig getObjectConfig() {
			return objectConfig;
		}
		public void setObjectConfig(ObjectConfig objectConfig) {
			this.objectConfig = objectConfig;
		}
		@Override
		public String toString() {
			return "FrameConfig [action=" + action + ", utcDateTime=" + utcDateTime + ", motionConfig=" + motionConfig
					+ ", objectConfig=" + objectConfig + "]";
		}
    	
    }
    public static class MotionConfig{
    	@JSONField(name="sensitivity")
    	private int sensitivity;
    	
    	@JSONField(name="dataList")
    	private List<Data> dataList;
    	
		public int getSensitivity() {
			return sensitivity;
		}
		public void setSensitivity(int sensitivity) {
			this.sensitivity = sensitivity;
		}
		public List<Data> getDataList() {
			return dataList;
		}
		public void setDataList(List<Data> dataList) {
			this.dataList = dataList;
		}
		@Override
		public String toString() {
			return "MotionConfig [sensitivity=" + sensitivity + ", dataList=" + dataList + "]";
		}
    	
    }
    public static class ObjectConfig{
    	@JSONField(name="sensitivity")
    	private int sensitivity;
    	
    	@JSONField(name="dataList")
    	private List<Data> dataList;
    	
		public List<Data> getDataList() {
			return dataList;
		}
		public void setDataList(List<Data> dataList) {
			this.dataList = dataList;
		}
		public int getSensitivity() {
			return sensitivity;
		}
		public void setSensitivity(int sensitivity) {
			this.sensitivity = sensitivity;
		}
		@Override
		public String toString() {
			return "ObjectConfig [sensitivity=" + sensitivity + ", dataList=" + dataList + "]";
		}
		
    }
    public static class Data{
    	@JSONField(name="id")
    	private int id;
    	
    	@JSONField(name="pos")
    	private String pos;
    	
    	@JSONField(name="objectList")
    	private String objectList;
    	
		public int getId() {
			return id;
		}
		public void setId(int id) {
			this.id = id;
		}
		public List<Integer> getPosList() {
			String[] poss = pos.split(",");
			if(4 != poss.length){
				Collections.emptyList();
			}
			List<Integer> posList = new ArrayList<>();
			posList.add(Integer.valueOf(poss[0]));
			posList.add(Integer.valueOf(poss[1]));
			posList.add(Integer.valueOf(poss[2]));
			posList.add(Integer.valueOf(poss[3]));
			return posList;
		}
		public String getPos() {
			return pos;
		}
		public void setPosFromList(List<Integer> pos) {
			if(null == pos || pos.isEmpty()){
				return;
			}
			this.pos = pos.get(0)+"," + pos.get(1) + "," + pos.get(2) + "," + pos.get(3);
		}
		public void setPos(String pos) {
			this.pos  = pos;
		}
		public String getObjectList() {
			return objectList;
		}
		public void setObjectList(String objectList) {
			this.objectList = objectList;
		}
		@Override
		public String toString() {
			return "Data [id=" + id + ", pos=" + pos + ", objectList=" + objectList + "]";
		}
    	
    }
    
}
