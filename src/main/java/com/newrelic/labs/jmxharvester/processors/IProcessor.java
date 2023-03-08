package com.newrelic.labs.jmxharvester.processors;

import javax.management.MBeanServer;

import com.newrelic.labs.jmxharvester.JMXHarvesterConfig;
import com.newrelic.labs.jmxharvester.MBeanConfig;
import com.newrelic.telemetry.metrics.MetricBatchSender;

public interface IProcessor {

	public void execute(MBeanServer _mbeanServer, MBeanConfig[] _mbeans, JMXHarvesterConfig _jmxHarvesterConfig, MetricBatchSender _metricBatchSender);
		
} //IProcessor
