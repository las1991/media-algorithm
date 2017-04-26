package com.sengled.mediaworker;
import java.sql.Timestamp;

import org.springframework.boot.SpringApplication;

import com.sengled.mediaworker.dynamodb.Dynamodb;
import com.sengled.mediaworker.dynamodb.DynamodbInit;
import com.sengled.mediaworker.s3.S3;
import com.sengled.mediaworker.spring.DefaultBootApplication;
import com.sengled.mediaworker.sqs.SQSTemplate;

public class AlgorithmServer {
	public static final String SPRING_CONFIG_LOCATION = "spring.config.location";
	
	public static void main(String[] args) {
		final long startAt = System.currentTimeMillis();
		if (null == System.getProperty(SPRING_CONFIG_LOCATION)) {
			throw new IllegalArgumentException(
					"Not Found Env property '-Dspring.config.location', please use -Dspring.config.location=classpath:/config/application.properties,file:/etc/sengled/sengled.properties");
		}
		SpringApplication.run(new Object[] {
		                                    DefaultBootApplication.class,
		                                    AlgorithmKinesisStreamProcessor.class,
											S3.class,
											SQSTemplate.class,
											Dynamodb.class,
											DynamodbInit.class

								}, args);
		System.out.println("media-algorithm-v3  started at " + new Timestamp(System.currentTimeMillis()) + ", cost " + (System.currentTimeMillis() - startAt) + "ms");
		
	}
}
