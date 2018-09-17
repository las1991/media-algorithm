package com.sengled.mediaworker.algorithm.decode;

import java.io.UnsupportedEncodingException;
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

        //data format :[$ (1byte)][jsonlen(2byte)][jsondata (jsonlen byte)][h264 nal (data.length - jsonlen - 2 - 1)]
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
        	final FrameConfig frameConfig = JSONObject.parseObject(configString, FrameConfig.class);
			return new Frame(frameConfig, dataBytes);
		} catch (UnsupportedEncodingException e) {
			LOGGER.error("Frame decode error.");
			throw new FrameDecodeException(e.getMessage(),e);
		}
    }
    
    public static class Frame {
    	
    	private final FrameConfig config;
        private final byte[] nalData;
        
        public Frame( FrameConfig config, byte[] dataBytes) {
        	this.config = config;
            this.nalData = dataBytes;
        }

		public byte[] getNalData() {
			return nalData;
		}

		public FrameConfig getConfig() {
			return config;
		}
		
    }
    public static class FrameConfig{
    	@JSONField(name="action")
    	private String action;//open,exec,close

        @JSONField(name="utcDateTime")
    	private String utcDateTime;
    	
    	@JSONField(name="fileExpires")
    	private int fileExpiresHours;
        
        public void setFileExpiresHours(int fileExpiresHours) {
            this.fileExpiresHours = fileExpiresHours;
        }

        public int getFileExpiresHours() {
            return fileExpiresHours;
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

        @Override
        public String toString() {
            return "FrameConfig [action=" + action + ", utcDateTime=" + utcDateTime + ", fileExpiresHours=" + fileExpiresHours + "]";
        }
    }
}
