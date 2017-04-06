package com.sengled.mediaworker.spring;



import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.codahale.metrics.MetricRegistry;
import com.sengled.mediaworker.metrics.MetricsGraphicsController;
import com.sengled.mediaworker.metrics.osmonitor.OSMonitor;

@Configuration
@ConfigurationProperties
public class DefaultBootApplication {
	@Bean
	public JettyEmbeddedServletContainerFactory embeddedServletContainerFactory() {
		JettyEmbeddedServletContainerFactory factory;
		factory = new JettyEmbeddedServletContainerFactory();
		
		return factory;
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
	} }
