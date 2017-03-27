package com.sengled.media.worker;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sengled.media.worker.s3.AmazonS3Template;

/**
 * kinesis stream 消费后回调
 * 
 * @author liwei
 * @Date 2017年3月2日 下午3:26:39
 * @Desc
 */
@Component
public class S3FunctionListener implements FunctionListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(S3FunctionListener.class);

    @Autowired
    AmazonS3Template amazonS3Template;

    @Value("${aws_screenshot_bucket}")
    private String bucketName;

    @Override
    public void operationComplete(Exception e, String token, byte[] record, byte[] result) {
        if (e != null) {
            LOGGER.info("token:{}", token);
            LOGGER.error(e.getMessage(), e);
            return;
        }
        if(result == null || result.length <=0){
            LOGGER.warn("result is null.token:{}",token);
            return;
        }
        try {
            LOGGER.info("save s3 token:{}", token);
            amazonS3Template.putObject(bucketName, token + "_big.jpg", result);
            amazonS3Template.putObject(bucketName, token + "_small.jpg", result);
        } catch (IOException e1) {
            LOGGER.info("token:{}", token);
            LOGGER.error("put s3 " + e1.getMessage(), e1);
        }
    }
}
