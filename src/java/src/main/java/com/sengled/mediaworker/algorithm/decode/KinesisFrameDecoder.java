package com.sengled.mediaworker.algorithm.decode;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sengled.mediaworker.RecordProcessor;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class KinesisFrameDecoder {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(KinesisFrameDecoder.class);

    
    public static Frame decode(byte[] iframe) throws IOException {
        final int fullLength = iframe.length;
        
        ByteBuf buf = Unpooled.wrappedBuffer(iframe);
        
        int firstByte = buf.readByte();
        if ('$' != firstByte) {
            throw new IllegalArgumentException("非法数据"+firstByte);
        }
        
        int jsonBytesLength = buf.readUnsignedShort();
        
        final byte[] jsonBytes = new byte[jsonBytesLength];
        final byte[] dataBytes = new byte[fullLength - jsonBytesLength - 2 - 1];
        buf.readBytes(jsonBytes);
        buf.readBytes(dataBytes);
        
        return new Frame(new String(jsonBytes, "UTF-8"), dataBytes);
    }
    
    public static class Frame {
    	
    	private Map<String, Object>  configs;
        private byte[] data;
        ObjectMapper objectMapper = new ObjectMapper();
        
        public Frame(String string, byte[] dataBytes) {
            try {
				this.configs = objectMapper.readValue(string, HashMap.class);
			} catch (Exception e) {
				LOGGER.error(e.getMessage(),e);
			}
            this.data = dataBytes;
        }

		public Map<String, Object> getConfigs() {
			return configs;
		}

		public void setConfigs(Map<String, Object> configs) {
			this.configs = configs;
		}

		public byte[] getData() {
			return data;
		}

		public void setData(byte[] data) {
			this.data = data;
		}   
    }
}
