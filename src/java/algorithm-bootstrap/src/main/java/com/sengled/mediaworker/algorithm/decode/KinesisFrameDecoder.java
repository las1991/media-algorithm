package com.sengled.mediaworker.algorithm.decode;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
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
			return new Frame(new String(jsonBytes, "UTF-8"), dataBytes);
		} catch (UnsupportedEncodingException e) {
			throw new FrameDecodeException(e.getMessage(),e);
		}
    }
    
    public static class Frame {
    	
    	private Map<String, Object>  configs;
        private byte[] nalData;
        ObjectMapper objectMapper = new ObjectMapper();
        
        public Frame(String string, byte[] dataBytes) {
            try {
				this.configs = objectMapper.readValue(string, HashMap.class);
			} catch (Exception e) {
				LOGGER.error(e.getMessage(),e);
			}
            this.nalData = dataBytes;
        }

		public Map<String, Object> getConfigs() {
			return configs;
		}

		public void setConfigs(Map<String, Object> configs) {
			this.configs = configs;
		}

		public byte[] getNalData() {
			return nalData;
		}

		public void setNalData(byte[] yuvData) {
			this.nalData = yuvData;
		}

   
    }
}
