package com.sengled.mediaworker.dynamodb;

import java.util.Arrays;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;

public class DynamodbInit {
	private static final Logger LOGGER = LoggerFactory.getLogger(DynamodbInit.class);
	
	/**
	 * 算法结果dynamodb表名
	 */
	@Value("${AWS_SERVICE_NAME_PREFIX}_m_algorithm_results")
	private String algorithmResultDynamodbTableName;

	@Autowired
	DynamodbTemplate dynamodbTemplate;
	
	@PostConstruct
	public void init(){
		boolean bool = dynamodbTemplate.isExists(algorithmResultDynamodbTableName);
		if(!bool){
			LOGGER.warn("Dynamodb:{} isnot exists! create...",algorithmResultDynamodbTableName);
			createAlgorithmResultTable();
		}
	}
	private void createAlgorithmResultTable(){
		DynamoDB dynamoDB = dynamodbTemplate.getDynamoDB();
		try {
			LOGGER.info("Attempting to create table; please wait...");
			Table table = dynamoDB.createTable(algorithmResultDynamodbTableName,
					Arrays.asList(new KeySchemaElement("token", KeyType.HASH), // Partition																				// key
								  new KeySchemaElement("created", KeyType.RANGE)), // Sort
					Arrays.asList(new AttributeDefinition("token", ScalarAttributeType.S),
							new AttributeDefinition("created", ScalarAttributeType.S)),
					new ProvisionedThroughput(10L, 10L));
			table.waitForActive();
			LOGGER.info("Success.  Table status: " + table.getDescription().getTableStatus());
		} catch (Exception e) {
			LOGGER.error(e.getMessage(),e);
			LOGGER.error("Unable to create table: {}.system exit.",algorithmResultDynamodbTableName);
			System.exit(1);
		}
	}
}
