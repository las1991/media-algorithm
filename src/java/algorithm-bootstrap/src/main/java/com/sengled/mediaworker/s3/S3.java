package com.sengled.mediaworker.s3;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;


@Configuration
@ConfigurationProperties
public class S3 implements InitializingBean {
	private static final Logger LOGGER = LoggerFactory.getLogger(S3.class);
	
	@Value("${aws_access_key_id}")
	private String aws_access_key_id;
	
	@Value("${aws_secret_access_key}")
	private String aws_secret_access_key;

	@Value("${aws_s3_region}")
	private String regionName;
	
	private AmazonS3 s3;
	
	@Override
	public void afterPropertiesSet() {
		LOGGER.info("Initializing...");
		try {
			initialize();
		} catch (Exception e) {
			LOGGER.error("Fail connect with S3 for '{}'.", e.getMessage(), e);
			LOGGER.error(e.getMessage(),e);
			System.exit(1);
		}	
	}
	
	private void initialize() {
		ClientConfiguration conf = new ClientConfiguration();
		conf.setMaxErrorRetry(3);
		conf.setConnectionTimeout(15*1000);
		conf.setSocketTimeout(60*1000);
		conf.setProtocol(Protocol.HTTPS);
		conf.setMaxConnections(3 * ClientConfiguration.DEFAULT_MAX_CONNECTIONS);
		conf.setUseTcpKeepAlive(true);
		LOGGER.info("connect with S3, access_key = {}, secret_access_key={}, region = {}.", aws_access_key_id, aws_secret_access_key, regionName);
		BasicAWSCredentials credentials = new BasicAWSCredentials(aws_access_key_id, aws_secret_access_key);
		Region region = Region.getRegion(Regions.fromName(regionName));

		s3 = new AmazonS3Client(credentials, conf);
		s3.setRegion(region);
		
		// 查看都有哪些桶
		List<Bucket> buckets = s3.listBuckets();
		for (Bucket bucket : buckets) {
			LOGGER.info("{}'s has bucket '{}'", s3.getRegion(), bucket.getName());
		}
	}

	@Bean()
	public AmazonS3Template getS3Template(){
		return new AmazonS3Template(s3);
	}
	
	public void setAws_access_key_id(String aws_access_key_id) {
		this.aws_access_key_id = aws_access_key_id;
	}
	
	public void setAws_secret_access_key(String aws_secret_access_key) {
		this.aws_secret_access_key = aws_secret_access_key;
	}
	
	public void setRegion(String region) {
		this.regionName = region;
	}
}
