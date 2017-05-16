package com.sengled.mediaworker.metrics.custom;

import java.util.Collection;
import java.util.LinkedHashSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.PublicMetrics;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

@Component
public class CustomPublicMetrics implements PublicMetrics, Ordered{

	@Autowired
	ServicesMetrics servicesMetrics; 
	@Override
	public int getOrder() {
		return 0;
	}

	@Override
	public Collection<Metric<?>> metrics() {
		Collection<Metric<?>> result = new LinkedHashSet<Metric<?>>();
		addMetrics(result);
		return result;
	}
	protected void addMetrics(Collection<Metric<?>> result) {
		
		result.add(newMemoryMetric(ServicesMetrics.RECEIVE,servicesMetrics.getValue(ServicesMetrics.RECEIVE)));
		result.add(newMemoryMetric(ServicesMetrics.DATA_DELAYED,servicesMetrics.getValue(ServicesMetrics.DATA_DELAYED)));
		result.add(newMemoryMetric(ServicesMetrics.RECEIVE_DELAYED,servicesMetrics.getValue(ServicesMetrics.RECEIVE_DELAYED)));
		result.add(newMemoryMetric(ServicesMetrics.SQS_FAILURE,servicesMetrics.getValue(ServicesMetrics.SQS_FAILURE)));
		result.add(newMemoryMetric(ServicesMetrics.S3_FAILURE,servicesMetrics.getValue(ServicesMetrics.S3_FAILURE)));
	}
	private Metric<Double> newMemoryMetric(String name, double num) {
		return new Metric<Double>(name, num);
	}
}
