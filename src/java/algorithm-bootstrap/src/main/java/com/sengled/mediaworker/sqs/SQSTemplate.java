package com.sengled.mediaworker.sqs;

import static com.google.common.base.Preconditions.checkState;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import javax.annotation.PreDestroy;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Base64Utils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.SerializationUtils;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SetQueueAttributesRequest;

/**
 * Created by admin on 2017/1/4.
 */
@Configuration
public class SQSTemplate implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(SQSTemplate.class);

    @Value("${AWS_SQS_REGION:cn-north-1}")
    private String regionName;

    @Value("${publisher_thread_count:3}")
    private Integer publisherThreadCount = 3; //发布消息线程数
    
    @Value("${AWS_SERVICE_NAME_PREFIX}_${sqs.algorithm.result.queue}")
	private String queue;

    private ExecutorService executor = Executors.newFixedThreadPool(publisherThreadCount);

    private AmazonSQS sqsClient;

    @PreDestroy
    public void destroy(){
        sqsClient.shutdown();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
    	LOGGER.info("Initializing...");
		try {
			initialize();
		} catch (Exception e) {
			LOGGER.error("Initialize AmazonSQS failed.");
			LOGGER.error(e.getMessage(),e);
			System.exit(1);
		}

    }

    private void initialize() {
        AmazonSQS sqsClient =  AmazonSQSClientBuilder.standard()
                                .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                                .withRegion(regionName)
                                .build();
        String queueUrl = sqsClient.getQueueUrl(queue).getQueueUrl();
        LOGGER.info("QueueUrl:{}",queueUrl);
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
                        LOGGER.info("The message is {}", message.getBody());

                        String serialized = "yes";
                        Map<String, MessageAttributeValue> attrs = message.getMessageAttributes();
                        if (null != attrs && null!=attrs.get("serialized")){
                            serialized = attrs.get("serialized").getStringValue();
                        }
                        LOGGER.info("serialized = {}", serialized);

                        T result = null;
                        if (serialized.equals("no")){
                            result = (T)Base64Utils.decodeFromString(message.getBody());
                        }else {
                            result = (T)SerializationUtils.deserialize(Base64Utils.decodeFromString(message.getBody()));
                        }
                        handler.handler(result);
                    }catch (Exception e){
                        LOGGER.error("Message press fail, {}", ExceptionUtils.getStackTrace(e));
                    }finally {
                        sqsClient.deleteMessage(new DeleteMessageRequest(url, message.getReceiptHandle()));
                    }
                }
            } else {
                LOGGER.debug("nothing found, trying again in 5 seconds");
                Thread.sleep(5000);
            }
        }
    }

    public String publish(String queue,Serializable message) throws Exception{
        String encodedMessage;
        try {
            encodedMessage = Base64Utils.encodeToString(SerializationUtils.serialize(message));
        } catch (Exception e) {
            throw new RuntimeException("Could not serialize message.", e);
        }

        checkState(encodedMessage.length() <= 256 * 1024);

        LOGGER.debug("Serialized Message: " + encodedMessage);

        String url = sqsClient.getQueueUrl(new GetQueueUrlRequest(queue)).getQueueUrl();
        SendMessageRequest request = new SendMessageRequest(url, encodedMessage);

        //request.withDelaySeconds(3);
        try {
            return sqsClient.sendMessage(request).getMessageId();
        } catch (AmazonServiceException e) {
            LOGGER.warn("Could not sent message to SQS queue: {}. Retrying.", url);
        }
        throw new  IOException("Exceeded  message not sent!");
    }

    public void publish(String queue,List<Serializable> message){
        message.stream().forEach(new Consumer<Serializable>() {
            @Override
            public void accept(Serializable t) {
                try {
                    publish(queue, t);
                } catch (Exception e) {
                    LOGGER.error("publish msg error. msg:{}", t);
                    LOGGER.error(e.getMessage(),e);
                }
            }
        });
    }
}
