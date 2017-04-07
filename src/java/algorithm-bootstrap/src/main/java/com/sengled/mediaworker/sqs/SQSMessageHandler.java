package com.sengled.mediaworker.sqs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by admin on 2017/1/5.
 */
public abstract class SQSMessageHandler<T> {

    protected static final Logger logger = LoggerFactory.getLogger(SQSMessageHandler.class);

    protected Map<String, String> extConfig = new HashMap<String, String>();

    protected String queue;

    @Value("${receive_thread_count:3}")
    protected Integer threadCount;

    @Autowired
    private SQSTemplate sqsTemplate;

    public abstract void handler(T message);

    protected abstract void init();

    @PostConstruct
    public void subMessage(){
        this.init();
        //声明一个线程池，订阅消息
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        logger.info("subscribe {}/{}",queue, threadCount);
        for (int i=0;i< threadCount;i++){
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        sqsTemplate.<T>subscribe(queue, extConfig, SQSMessageHandler.this);
                    } catch (Exception e) {
                        logger.error("aws sqs message subscribe fail,{}", e);
                    }
                }
            });
        }
    }
}
