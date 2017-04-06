package com.sengled.mediaworker.dynamodb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsync;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
@Component
public class Dynamodb {
	private static final Logger LOGGER = LoggerFactory.getLogger(Dynamodb.class);
	@Value("${AWS_DYNAMO_KEY}")
	private String accessKey;
	
	@Value("${AWS_DYNAMO_SECRET}")
	private String secretKey;
		
	@Value("${AWS_DYNAMO_REGION}")
	private String region;
	
	private ClientConfiguration getConfig() {
		ClientConfiguration conf =  new ClientConfiguration();
		conf.setUseTcpKeepAlive(true);
		conf.setMaxErrorRetry(3);
		conf.setConnectionTimeout(15*1000);
		conf.setSocketTimeout(60*1000);
		conf.setProtocol(Protocol.HTTPS);
		conf.setMaxConnections(3 * ClientConfiguration.DEFAULT_MAX_CONNECTIONS);
		conf.setUseTcpKeepAlive(true);
		return conf;
	}
	
	@Bean
	public DynamodbTemplate getDynamodbTemplate(){
		AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
		AWSCredentialsProvider credentialsProvider = new StaticCredentialsProvider(credentials);
		AmazonDynamoDBAsync client = AmazonDynamoDBAsyncClientBuilder.standard()
				.withCredentials(credentialsProvider)
				.withRegion(region)
				.withClientConfiguration(getConfig())
				.build();
		DynamoDB dynamoDB = new DynamoDB(client);
		return new DynamodbTemplate(dynamoDB);
	}



}
