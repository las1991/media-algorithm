package com.sengled.mediaworker.algorithm.context;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sengled.media.interfaces.YUVImage;
import com.sengled.mediaworker.algorithm.decode.KinesisFrameDecoder.ObjectConfig;

/**
 * 物体识别上下文
 * 
 * @author media-liwei
 *
 */
public class ObjectContext extends Context {
	private static final Logger LOGGER = LoggerFactory.getLogger(ObjectContext.class);


	private String token;
	private Long lastObjectTimestamp;
	private Date utcDateTime;
	private byte[] nalData;
	private YUVImage yuvImage;
	private ObjectConfig objectConfig;


	public ObjectContext(String token){
		this.token = token;
	}
	
	public void setLastObjectTimestamp(Long lastObjectTimestamp) {
		this.lastObjectTimestamp = lastObjectTimestamp;
	}

	public boolean isSkip(long objectIntervalTimeMsce) {
		boolean skip = false;
		Date utcDateTime = getUtcDateTime();
		LOGGER.info("lastObjectTimestamp:{},utcDateTime:{}",lastObjectTimestamp,utcDateTime);
		if (lastObjectTimestamp != null && utcDateTime != null) {
			long sinceLastMotion = (utcDateTime.getTime() - lastObjectTimestamp.longValue());

			if (sinceLastMotion <= objectIntervalTimeMsce) {
				LOGGER.info("Token:{},Since last time object:{} msec <= {} msec isSkip=true.", token,
						sinceLastMotion, objectIntervalTimeMsce);
				skip = true;
			} else {
				lastObjectTimestamp = null;
				LOGGER.info("Token:{},Since last time object:{} msec > {} msec .isSkip=false.", token, sinceLastMotion,
						objectIntervalTimeMsce);
				skip = false;
			}
		}
		return skip;
	}

	public Date getUtcDateTime() {
		return utcDateTime;
	}


	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public Long getLastObjectTimestamp() {
		return lastObjectTimestamp;
	}

	public void setUtcDateTime(Date utcDateTime) {
		this.utcDateTime = utcDateTime;
	}

	public byte[] getNalData() {
		return nalData;
	}

	public void setNalData(byte[] nalData) {
		this.nalData = nalData;
	}

	public YUVImage getYuvImage() {
		return yuvImage;
	}

	public void setYuvImage(YUVImage yuvImage) {
		this.yuvImage = yuvImage;
	}

	public ObjectConfig getObjectConfig() {
		return objectConfig;
	}

	public void setObjectConfig(ObjectConfig objectConfig) {
		this.objectConfig = objectConfig;
	}

}
