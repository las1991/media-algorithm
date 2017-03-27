/*
 * Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Amazon Software License (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://aws.amazon.com/asl/
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.sengled.mediaworker.test;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;

import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClient;
import com.amazonaws.services.kinesis.model.DescribeStreamResult;
import com.amazonaws.services.kinesis.model.PutRecordRequest;
import com.amazonaws.services.kinesis.model.ResourceNotFoundException;

/**
 * Continuously sends simulated stock trades to Kinesis
 *
 */
public class AlgorithmWriter {
    
//    final static String accessKey = "AKIAOJ3W3WYJF5TPQZAQ";
//    final static String secretKey = "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";
//    final static String STREAM_NAME = "capturer";
    
  final static String accessKey = "AKIAOBUUWDH7ATB6AEXA";
  final static String secretKey = "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";
  final static String STREAM_NAME = "test-bj1_algorithm";
    
    private static final Log LOG = LogFactory.getLog(AlgorithmWriter.class);

    private static void checkUsage(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: " + AlgorithmWriter.class.getSimpleName()
                    + " <stream name> <region>");
            System.exit(1);
        }
    }

    /**
     * Checks if the stream exists and is active
     *
     * @param kinesisClient Amazon Kinesis client instance
     * @param streamName Name of stream
     */
    private static void validateStream(AmazonKinesis kinesisClient, String streamName) {
        try {
            DescribeStreamResult result = kinesisClient.describeStream(streamName);
            if(!"ACTIVE".equals(result.getStreamDescription().getStreamStatus())) {
                System.err.println("Stream " + streamName + " is not active. Please wait a few moments and try again.");
                System.exit(1);
            }
        } catch (ResourceNotFoundException e) {
            System.err.println("Stream " + streamName + " does not exist. Please create it in the console.");
            System.err.println(e);
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Error found while describing the stream " + streamName);
            System.err.println(e);
            System.exit(1);
        }
    }

    /**
     * Uses the Kinesis client to send the stock trade to the given stream.
     *
     * @param trade instance representing the stock trade
     * @param kinesisClient Amazon Kinesis client
     * @param streamName Name of stream
     */
    
    
    private static void sendImageData(int index,byte[] imageBytes, AmazonKinesis kinesisClient,
            String streamName) {
        // The bytes could be null if there is an issue with the JSON serialization by the Jackson JSON library.
        if (imageBytes == null) {
            LOG.warn("Could not get JSON bytes for stock trade");
            return;
        }

        PutRecordRequest putRecord = new PutRecordRequest();
        putRecord.setStreamName(streamName);
        // We use the ticker symbol as the partition key, as explained in the tutorial.
        //putRecord.setPartitionKey("DD69960CE1DF6C27EBED2B7889CD8F5A");
        putRecord.setPartitionKey("IAMTOKEN_"+index);
        putRecord.setData(ByteBuffer.wrap(imageBytes));
        LOG.info("Putting token: " + "_IAMTOKEN_"+index);

        try {
            kinesisClient.putRecord(putRecord);
        } catch (AmazonClientException ex) {
            LOG.warn("Error sending record to Amazon Kinesis.", ex);
        }
    }

    public static void main(String[] args) throws Exception {
    	int millis = Integer.valueOf(args[0]);
        //checkUsage(args);

        String streamName = STREAM_NAME;
        String regionName = Regions.CN_NORTH_1.getName();
        Region region = RegionUtils.getRegion(regionName);
        if (region == null) {
            System.err.println(regionName + " is not a valid AWS region.");
            System.exit(1);
        }

        //AWSCredentials credentials = CredentialUtils.getCredentialsProvider().getCredentials();
        //add by leekli
        BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

        AmazonKinesis kinesisClient = new AmazonKinesisClient(credentials,
                ConfigurationUtils.getClientConfigWithUserAgent());
        kinesisClient.setRegion(region);

        // Validate that the stream exists and is active
        validateStream(kinesisClient, streamName);

        // Repeatedly send stock trades with a 100 milliseconds wait in between
        int i=1000;
        while(true) {
//            StockTrade trade = stockTradeGenerator.getRandomTrade();
//            sendStockTrade(trade, kinesisClient, streamName);
        	
            File file = new  File("D:\\test\\cutout1WithConfigExec.flv");
            if( !file.exists()){
                file = new File("/root/data");
            }
            FileInputStream fis = new FileInputStream(file);
            byte[] imageBytes = new byte[(int)file.length()];
            fis.read(imageBytes);
            fis.close();
            sendImageData(i++,imageBytes , kinesisClient, streamName);
            
            Thread.sleep(millis);
        }
    }

}
