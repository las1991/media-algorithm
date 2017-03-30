package com.sengled.mediaworker.algorithm.decode;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class KinesisFrameDecoder {	
	private static final Logger LOGGER = LoggerFactory.getLogger(KinesisFrameDecoder.class);

    public static Frame decode(ByteBuffer buffer) throws Exception {
    	int remaining =  buffer.remaining();
		if ( remaining <= 0) {
			LOGGER.error("record data size is null.");
			throw new Exception("record data size is null");
		}
        ByteBuf buf = Unpooled.wrappedBuffer(buffer);
        int firstByte = buf.readByte();
        if ('$' != firstByte) {
            throw new IllegalArgumentException("非法数据"+firstByte);
        }
        
        int jsonBytesLength = buf.readUnsignedShort();
        
        final byte[] jsonBytes = new byte[jsonBytesLength];
        final byte[] dataBytes = new byte[remaining - jsonBytesLength - 2 - 1];
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
