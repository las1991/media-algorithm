package com.sengled.mediaworker.mediabase;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

@Deprecated
public class MediaBaseConfig implements InitializingBean{
    private static final Logger LOGGER = LoggerFactory.getLogger(MediaBaseConfig.class);

    @Override
    public void afterPropertiesSet() throws Exception {
        
    }
    
}
