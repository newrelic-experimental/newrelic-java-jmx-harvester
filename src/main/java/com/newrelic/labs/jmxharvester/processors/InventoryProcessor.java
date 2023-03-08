package com.newrelic.labs.jmxharvester.processors;

import java.util.Iterator;
import java.util.Set;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import com.newrelic.agent.Agent;
import com.newrelic.labs.jmxharvester.Constantz;
import com.newrelic.labs.jmxharvester.JMXHarvesterConfig;
import com.newrelic.labs.jmxharvester.MBeanConfig;
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.Response;
import com.newrelic.telemetry.metrics.Gauge;
import com.newrelic.telemetry.metrics.MetricBatchSender;
import com.newrelic.telemetry.metrics.MetricBuffer;

public class InventoryProcessor implements IProcessor {

	@Override
	public void execute(MBeanServer _mbeanServer, MBeanConfig[] _mbeans, JMXHarvesterConfig _jmxHarvesterConfig, MetricBatchSender _metricBatchSender) {

		//metrics only
        MetricBuffer __metricBuffer = null;
        Set<ObjectInstance> __mbeanInstances = _mbeanServer.queryMBeans(null, null);
        Iterator<ObjectInstance> __iterator = __mbeanInstances.iterator();
        
        Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Running Inventory .... ");
                    
        try {

        	__metricBuffer = new MetricBuffer(_jmxHarvesterConfig.getGlobalAttributes());
        	
            while (__iterator.hasNext()) {
            	                
                ObjectInstance instance = __iterator.next();
                ObjectName objectName = instance.getObjectName();
                MBeanInfo __info = _mbeanServer.getMBeanInfo(objectName);
      
                // going to reuse the metric buffer and reallocate when it is cleared __metricBuffer = new MetricBuffer(globalAttributes);
                Attributes __standardAttributes = new Attributes();
                __standardAttributes.put("jmx_harvester_mode", "inventory");
                __standardAttributes.put("jmx_harvester_type", "inventory"); //keeping both mode and type to support more exotic types in the future - the harvester ui depends on _type attribute
                __standardAttributes.put("object_type", "mbean");
                __standardAttributes.put("mbean_name", objectName.toString());
                __standardAttributes.put("mbean_domain", objectName.getDomain());                
            	
                MBeanAttributeInfo[] __mbai = __info.getAttributes();
                
                for (int i = 0; i < __mbai.length; i++) {

                	 Attributes __attributeAttributes = new Attributes();
                	 __attributeAttributes.put("mbean_element", "attribute");
                     __attributeAttributes.put("mbean_attribute_type", __mbai[i].getType());
                     __attributeAttributes.put("mbean_attribute_description", __mbai[i].getDescription());
                     __attributeAttributes.put("mbean_attribute_readable", __mbai[i].isReadable());
                     __attributeAttributes.put("mbean_attribute_name", __mbai[i].getName());
                     __attributeAttributes.put("jmx_harvester_frequency", _jmxHarvesterConfig.getFrequency());
                     __attributeAttributes.putAll(__standardAttributes);
                     Gauge __gauge = new Gauge(Constantz.INVENTORY_METRIC_PREFIX + "." + instance.getObjectName() + "." + __mbai[i].getName(), 0d, System.currentTimeMillis(), __attributeAttributes);
                    __metricBuffer.addMetric(__gauge);
                } //for


                MBeanOperationInfo[] __mboi = __info.getOperations();

                for (int i = 0; i < __mboi.length; i++) {

                	Attributes __operationAttributes = new Attributes();
                	__operationAttributes.put("mbean_element", "operation");
                	__operationAttributes.put("mbean_operation_return_type", __mboi[i].getReturnType());
                	__operationAttributes.put("mbean_operation_description", __mboi[i].getDescription());
                	__operationAttributes.put("mbean_operation_name", __mboi[i].getName());
                	__operationAttributes.put("jmx_harvester_frequency", _jmxHarvesterConfig.getFrequency());
                	__operationAttributes.putAll(__standardAttributes);
                    Gauge __operation_gauge = new Gauge(Constantz.INVENTORY_METRIC_PREFIX + "." + instance.getObjectName() + "." + __mboi[i].getName(), 0d, System.currentTimeMillis(), __operationAttributes);
               		__metricBuffer.addMetric(__operation_gauge);
                } //for

                if (__metricBuffer.size() > _jmxHarvesterConfig.getMetricBatchSize()) {
                    try {
                    	
                    	Response __response = _metricBatchSender.sendBatch(__metricBuffer.createBatch());
                    	Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] metricBatchSend Status Message >>>> " + __response.getStatusMessage());
                    	__metricBuffer = new MetricBuffer(_jmxHarvesterConfig.getGlobalAttributes());
                    } //try
                    catch(java.lang.Exception _e) {
                    	
                    	Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] metricBatchSender failed. " + _e.toString());
                    } //catch                	
                } //if
                
            } //while
            
            // send remaining batch of metrics ... 
            if (__metricBuffer.size() > 0) {
            	
	    	   try {
	           	
	    		   Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Sending " + __metricBuffer.size() + " metrics to New Relic.");
		           Response __response = _metricBatchSender.sendBatch(__metricBuffer.createBatch());
		           Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] FINAL metricBatchSend Status Message >>>> " + __response.getStatusMessage());
		           	__metricBuffer = null;
	           } //try
	           catch(java.lang.Exception _e) {
	           	
	        	   Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] FINAL metricBatchSender failed on final metrics push. " + _e.toString());
	           } //catch
            } //if

        } //try
        catch (java.lang.Exception _e) {

            Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] Problem discovering MBeans and Attributes.");
            Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] Message: " + _e.getMessage());
        } //catch
	
	} //execute
	
} //InventoryProcessor
