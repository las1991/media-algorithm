package com.sengled.mediaworker.mediabase;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import com.sengled.media.RESTfulInvoker;
import com.sengled.media.algorithm.MediaAlgorithmService;

@Configuration
public class MediaBaseConfig implements InitializingBean{
    private static final Logger LOGGER = LoggerFactory.getLogger(MediaBaseConfig.class);
    
    private RESTfulInvoker invoker;
    
    @Value("http://" + "${MEDIABASE_SERVER_SOCKET_ADDRESS}")
    private String  mediaBaseUrl;
    
    @Autowired
    RestTemplate restTemplate;

    @Override
    public void afterPropertiesSet() throws Exception {
        invoker = new RESTfulInvoker(restTemplate, mediaBaseUrl);
        LOGGER.info("{} used", mediaBaseUrl);
    }
    
    @Bean
    public MediaAlgorithmService mediaAlgorithmService(){
        return invoker.newProxyInstance(MediaAlgorithmService.class);
    }
}
