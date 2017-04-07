package com.sengled.mediaworker.sqs;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.*;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Base64Utils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.SerializationUtils;

import javax.annotation.PreDestroy;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.google.common.base.Preconditions.checkState;

/**
 * Created by admin on 2017/1/4.
 */
@Configuration
public class SQSTemplate implements InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(SQSTemplate.class);

    @Value("${AWS_SQS_KEY}")
    private String awsKey;

    @Value("${AWS_SQS_SECRET}")
    private String awsSecret;

    @Value("${AWS_SQS_REGION:cn-north-1}")
    private String regionName;


    @Value("${publisher_thread_count:3}")
    private Integer publisherThreadCount = 3; //发布消息线程数

    private ExecutorService executor = Executors.newFixedThreadPool(publisherThreadCount);

    private AmazonSQS sqsClient;

    @PreDestroy
    public void destroy(){
        sqsClient.shutdown();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        AmazonSQS sqsClient =new AmazonSQSClient(new BasicAWSCredentials(awsKey, awsSecret));
        Region region = null;
        try {
            region = Region.getRegion(Regions.fromName(regionName));
        }catch (Exception e){
            logger.warn("use default region,{}", region.getName());
            region = Region.getRegion(Regions.DEFAULT_REGION);
        }
        sqsClient.setRegion(region);
        this.sqsClient = sqsClient;
    }


    public <T>void subscribe(String queue, Map<String, String> config, SQSMessageHandler handler) throws Exception {
        String url = sqsClient.getQueueUrl(new GetQueueUrlRequest(queue)).getQueueUrl();
        if (!CollectionUtils.isEmpty(config)){
            sqsClient.setQueueAttributes(new SetQueueAttributesRequest(url,config));
        }

        while (true) {
            ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(url);
            receiveMessageRequest.withMaxNumberOfMessages(10);
            receiveMessageRequest.setMessageAttributeNames(new ArrayList<String>(Arrays.asList("All")));
            List<Message> msgs = sqsClient.receiveMessage(receiveMessageRequest).getMessages();

            if (!msgs.isEmpty()) {
                for (Message message: msgs){
                    try {
                        logger.info("The message is {}", message.getBody());

                        String serialized = "yes";
                        Map<String, MessageAttributeValue> attrs = message.getMessageAttributes();
                        if (null != attrs && null!=attrs.get("serialized")){
                            serialized = attrs.get("serialized").getStringValue();
                        }
                        logger.info("serialized = {}", serialized);

                        T result = null;
                        if (serialized.equals("no")){
                            result = (T)Base64Utils.decodeFromString(message.getBody());
                        }else {
                            result = (T)SerializationUtils.deserialize(Base64Utils.decodeFromString(message.getBody()));
                        }
                        handler.handler(result);
                    }catch (Exception e){
                        logger.error("Message press fail, {}", ExceptionUtils.getStackTrace(e));
                    }finally {
                        sqsClient.deleteMessage(new DeleteMessageRequest(url, message.getReceiptHandle()));
                    }
                }
            } else {
                logger.debug("nothing found, trying again in 5 seconds");
                Thread.sleep(5000);
            }
        }
    }

    public String publish(String queue,Serializable message){
        String encodedMessage;
        try {
            encodedMessage = Base64Utils.encodeToString(SerializationUtils.serialize(message));
        } catch (Exception e) {
            throw new RuntimeException("Could not serialize message.", e);
        }

        checkState(encodedMessage.length() <= 256 * 1024);

        logger.debug("Serialized Message: " + encodedMessage);

        String url = sqsClient.getQueueUrl(new GetQueueUrlRequest(queue)).getQueueUrl();
        SendMessageRequest request = new SendMessageRequest(url, encodedMessage);

        //request.withDelaySeconds(3);
        try {
            return sqsClient.sendMessage(request).getMessageId();
        } catch (AmazonServiceException e) {
            logger.warn("Could not sent message to SQS queue: {}. Retrying.", url);
        }
        throw new RuntimeException("Exceeded  message not sent!");
    }

    public void publishOnBackend(final String queue, final Serializable message){
        executor.execute(new Runnable() {
            @Override
            public void run() {
                logger.info("publish message to sqs, queue={},content={}", queue,message);
                publish(queue, message);
                logger.info("publish done");
            }
        });
    }
}
