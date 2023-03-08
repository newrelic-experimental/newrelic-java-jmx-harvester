package com.newrelic.labs.jmxharvester.processors;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import com.newrelic.agent.Agent;
import com.newrelic.labs.jmxharvester.Constantz;
import com.newrelic.labs.jmxharvester.JMXHarvesterConfig;
import com.newrelic.labs.jmxharvester.MBeanAttributeConfig;
import com.newrelic.labs.jmxharvester.MBeanConfig;
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.Response;
import com.newrelic.telemetry.metrics.MetricBatchSender;
import com.newrelic.telemetry.metrics.MetricBuffer;

public class StrictProcessor implements IProcessor {

	public StrictProcessor() {} //StrictProcessor
	
	@Override
	public void execute(MBeanServer _mbeanServer, MBeanConfig[] _mbeans, JMXHarvesterConfig _jmxHarvesterConfig, MetricBatchSender _metricBatchSender) {
		
		ObjectName __tempMBean = null;
        String __stTelemetryModel = _jmxHarvesterConfig.getTelemetryModel(); //this will be the telemetry model for this run regardless of config change
        
        // events model elements
        Map<String, Object> __tempEventAttributes = null;
        Vector<Map> __tempTabularEventVector = new Vector<Map>();

        //metric model elements 
        MetricBuffer __metricBuffer = new MetricBuffer(_jmxHarvesterConfig.getGlobalAttributes());
        Attributes __tempMetricAttributes = null;
        Attributes __tempMetricAtrributeAttributes = null;
        
        MBeanAttributeConfig[] __macAttributes = null;
        Iterator<ObjectInstance> __oiIterator = null;
        Set<ObjectInstance> __oiSet = null;
        ObjectInstance __oiInstance = null;
        Hashtable<?, ?> __htOIProperties = null;

        for (int i = 0; i < _mbeans.length; i++) {

            try {

                if (!Utilities.testAndIncrementMBeanPollingInterval(_mbeans[i])) {

                    Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] This MBean's polling interval is not satisfied it will be increased by one from: " + _mbeans[i].getMBeanName() + " : " + _mbeans[i].getMBeanPollingCounter());
                    _mbeans[i].incrementMBeanPollingCounter();
                } //if
                else {
                	
                	Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] This MBean's polling interval has been satisfied: " + _mbeans[i].getMBeanName() + " : " + _mbeans[i].getPollingInterval());
                	Utilities.normalizeMBeanName(_mbeans[i]);
                	
                    __tempMBean = new ObjectName(_mbeans[i].getMBeanName());
                    __oiSet = _mbeanServer.queryMBeans(__tempMBean, null);
                    
                    if (__oiSet == null) {
                    	
                        Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Unable to find the bean defined by configuration: " + _mbeans[i].getMBeanName());
                        Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Bean representation as MBean Domain: " + __tempMBean.getDomain());
                        Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Bean representation as MBean Property List: " + __tempMBean.getKeyPropertyListString());
                        Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Bean representation as MBean Object String: " + __tempMBean.toString());
                    } //if
                    else {
                    
                        __oiIterator = __oiSet.iterator();

                        while (__oiIterator.hasNext()) {
                        	
                            __oiInstance = __oiIterator.next();
                            __htOIProperties = __oiInstance.getObjectName().getKeyPropertyList();
                            
                            if (__stTelemetryModel.equals("events")) {
                            	Agent.LOG.finer("[" + Constantz.EXTENSION_NAME + "] EVENTS have been selected as the telemetry model.");
                            	__tempEventAttributes = Utilities.getEventAttributes(__oiInstance);
                                
                            } //if
                            else if (__stTelemetryModel.equals("metrics")) {
                            	Agent.LOG.finer("[" + Constantz.EXTENSION_NAME + "] METRICS have been selected as the telemetry model");
                            	__tempMetricAttributes = Utilities.getMetricAttributes(__oiInstance);
                            	__tempMetricAttributes.put("jmx_harvester_mode", "strict"); 
                            	__tempMetricAttributes.put("jmx_harvester_type", "measurment"); 
                            	
                            } //else if
                            else {
                            	
                            	Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Problem processing MBeans, unknown telemetry model! Check your jmx_harvester config.");
                            } //else
                            
                            
                            //might want to add an MBean Instance
                            //removed to support complex type sub members __stAttributes = __mbeans[i].getMBeanAttributes();
                            __macAttributes = _mbeans[i].getMbeanAttributeObjects();

                            for (int ii = 0; ii < __macAttributes.length; ii++) {
                            	
                            	if (__stTelemetryModel.equals("events")) {

                            		Utilities.handleMBeanAttributeAsEvent(_mbeanServer.getAttribute(__oiInstance.getObjectName(), __macAttributes[ii].getAttributeName()), __macAttributes[ii], __tempEventAttributes, __tempTabularEventVector);
                            	} //if
                            	else if (__stTelemetryModel.equals("metrics")) {
                            		
                            		__tempMetricAtrributeAttributes = new Attributes();
                            		__tempMetricAtrributeAttributes.put("mbean_attribute_name", __macAttributes[ii].getAttributeName());
                            		__tempMetricAtrributeAttributes.putAll(__tempMetricAttributes);
                              		Utilities.handleMBeanAttributeAsMetric(_jmxHarvesterConfig, __metricBuffer, _mbeanServer.getAttribute(__oiInstance.getObjectName(), __macAttributes[ii].getAttributeName()), __oiInstance.getObjectName().getCanonicalName(), __macAttributes[ii], __tempMetricAtrributeAttributes);
                            	} //else if
                            	else {
                            		
                            		Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Problem processing MBeans, unknown telemetry model! Check your jmx_harvester config.");
                            	} //else

                            } //for


                            if (__stTelemetryModel.equals("events")) {
                            	
                                Utilities.publishInsightsEvent(_jmxHarvesterConfig, __tempEventAttributes);

                                //TODO Rework handling for tabular events
                                if (__tempTabularEventVector.size() > 0) {

                                    Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Adding TabularData entries for this mbean execution.");

                                    for (int __itabs = 0; __itabs < __tempTabularEventVector.size(); __itabs++) {

                                        Utilities.publishInsightsEvent(_jmxHarvesterConfig, __tempTabularEventVector.get(__itabs));

                                    } //for

                                } //if
                                
                            } //if
                            else if (__stTelemetryModel.equals("metrics")) {
                            	
                            	if (__metricBuffer.size() > _jmxHarvesterConfig.getMetricBatchSize()) {

                            		//send the metrics buffer contents ...
            				        try {
            				        	
            				        	Response __response = _metricBatchSender.sendBatch(__metricBuffer.createBatch());
            				        	Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] sending from strict ... " + __response.getStatusMessage());
            				        	__metricBuffer = new MetricBuffer(_jmxHarvesterConfig.getGlobalAttributes()); //reset the metric buffer
            				        	
            				        } //try
            				        catch(java.lang.Exception _e) {
            				        	
            				        	Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] Problem sending this batch of metrics from strict  - " + _e.toString());
            				        } //catch
                            	} //if
                            	
                            } //else if
                            else {
                            	
                            	Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Problem processing MBeans, unknown telemetry model! Check your jmx_harvester config. Nothing will be recorded.");
                            } //else

                        } //while
                        
                    } //else 

                    _mbeans[i].resetMBeanPollingCounter();
                } //else

            } //try
            catch (java.lang.Exception _e) {

                Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] Problem loading the mbean: " + _mbeans[i].getMBeanName());
                Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] Message MBean Fail: " + _e.getMessage());
                //adding additional Logging for the NULL MBEAN ISSUE:
                Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] Message MBean Fail Cause: " + _e.getCause());
            } //catch
        } //for
        
        // send the remaining values in the buffer
        if (__metricBuffer.size() > 0) {
        	
        	//send the metrics buffer contents ...
	        try {
	        	
	        	Response __response = _metricBatchSender.sendBatch(__metricBuffer.createBatch());
	        	Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] FINAL sending from strict ... " + __response.getStatusMessage());
	        	__metricBuffer = null; //reset the metric buffer
	        	
	        } //try
	        catch(java.lang.Exception _e) {
	        	
	        	Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] Problem sending this FINAL metrics batch of metrics from strict  - " + _e.toString());
	        } //catch
        } //if
		
	} //execute

} //StrictProcessor
