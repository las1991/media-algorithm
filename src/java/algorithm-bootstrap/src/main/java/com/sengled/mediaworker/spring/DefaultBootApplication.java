package com.sengled.mediaworker.spring;



import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Slf4jRequestLog;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.jetty.JettyServerCustomizer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sengled.mediaworker.metrics.MetricsGraphicsController;
import com.sengled.mediaworker.metrics.osmonitor.OSMonitor;

@Configuration
@ConfigurationProperties
public class DefaultBootApplication {

    @Bean
    public EmbeddedServletContainerFactory getEmbeddedServletContainerFactory(
            @Value("${server.jetty.maxThreads:256}") int maxThreads, 
            @Value("${server.jetty.minThreads:16}") int minThreads, 
            @Value("${server.jetty.idleTimeoutSeconds:30}") int idleTimeout) {
        
        // 记录请求日志
        Slf4jRequestLog requestLogger = new Slf4jRequestLog();
        requestLogger.setLogLatency(true);
        requestLogger.setLogServer(true);
        requestLogger.setLogDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        requestLogger.setIgnorePaths(new String[] {"/metrics", "/standard-metrics"});

        JettyEmbeddedServletContainerFactory factory = new JettyEmbeddedServletContainerFactory();
        factory.setThreadPool(new QueuedThreadPool(maxThreads, minThreads, idleTimeout * 1000));
        factory.addServerCustomizers(new JettyServerCustomizer() {
            @Override
            public void customize(Server server) {
                server.setRequestLog(requestLogger);
            }
        });
        
        return factory;
    }

    @Bean
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        ObjectMapper objectMapper = converter.getObjectMapper();
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return converter;
    }
	/**
	 * OS Monitor
	 * 
	 * 用于系统监控
	 * @return
	 */
	@Bean
	public OSMonitor getOsMonitor() {
		return OSMonitor.getInstance();
	} 
	
	@Bean
	public MetricsGraphicsController metricsGraphicsController() {
		return new MetricsGraphicsController();
	}

	@Bean()
	public MetricRegistry metricRegistry() {
		return new MetricRegistry();
	}
	
}
