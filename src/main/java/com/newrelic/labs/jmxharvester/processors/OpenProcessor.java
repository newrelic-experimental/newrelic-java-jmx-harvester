package com.newrelic.labs.jmxharvester.processors;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
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

public class OpenProcessor implements IProcessor{

	@Override
	public void execute(MBeanServer _mbeanServer, MBeanConfig[] _mbeans, JMXHarvesterConfig _jmxHarvesterConfig, MetricBatchSender _metricBatchSender) {

		ObjectName __tempMBean = null;
        String __stTelemetryModel = _jmxHarvesterConfig.getTelemetryModel(); //this will be the telemetry model for this run regardless of config change
        
        // events model objects 
        Map<String, Object> __tempEventAttributes = null;
        Vector<Map> __tempTabularEventVector = new Vector<Map>();
        
        // metrics model objects
        MetricBuffer __metricBuffer = new MetricBuffer(_jmxHarvesterConfig.getGlobalAttributes());
        Attributes __tempMetricAttributes = null;
        Attributes __tempMetricAtrributeAttributes = null;
        
        Iterator<ObjectInstance> __oiIterator = null;
        Set<ObjectInstance> __oiSet = null;
        ObjectInstance __oiInstance = null;
        Hashtable<?, ?> __htOIProperties = null;
        MBeanAttributeInfo[] __mbaiAttributes = null;
        MBeanInfo __mbiTempInfo = null;

        for (int i = 0; i < _mbeans.length; i++) {

            try {

                //determine if the candidate mbean satisfied the desired polling interval
                if (_mbeans[i].getPollingInterval() == _mbeans[i].getMBeanPollingCounter()) {

                    Agent.LOG.finer("[" + Constantz.EXTENSION_NAME + "] This MBean's polling interval has been satisfied: " + _mbeans[i].getMBeanName() + " : " + _mbeans[i].getPollingInterval());

                    /* determine if the mbean definition is using a leading wildcard - this has to be escaped by a special
                     * character because there is an issue with yml properties in the format "*:" - which could represent a valid
                     * MBean query string value.
                     */
                    if (_mbeans[i].getMBeanName().charAt(0) == '\\') {

                        _mbeans[i].setMBeanName(_mbeans[i].getMBeanName().substring(1));
                    } //if


                    __tempMBean = new ObjectName(_mbeans[i].getMBeanName());
                    __oiSet = _mbeanServer.queryMBeans(__tempMBean, null);
                    
                    if (__oiSet == null) {
                    	
                        Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Unable to find the bean defined by configuration: " + _mbeans[i].getMBeanName());
                        Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Bean representation as MBean Domain: " + __tempMBean.getDomain());
                        Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Bean representation as MBean Property List: " + __tempMBean.getKeyPropertyListString());
                        Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Bean representation as MBean Object String: " + __tempMBean.toString());
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
                            	__tempMetricAttributes.put("jmx_harvester_mode", "open"); 
                            	__tempMetricAttributes.put("jmx_harvester_type", "measurment"); 
                            	
                            } //else if
                            else {
                            	
                            	Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Problem processing MBeans, unknown telemetry model! Check your jmx_harvester config.");
                            } //else
                            
                            
                            try {

                                __mbiTempInfo = _mbeanServer.getMBeanInfo(__oiInstance.getObjectName());
                                __mbaiAttributes = __mbiTempInfo.getAttributes();

                                //report all the attributes for this mbean
                                for (int ii = 0; ii < __mbaiAttributes.length; ii++) {

                                    if (__mbaiAttributes[ii].isReadable()) {

                                    	if (__stTelemetryModel.equals("events")) {

                                        	Utilities.handleMBeanAttributeAsEvent(_mbeanServer.getAttribute(__oiInstance.getObjectName(), __mbaiAttributes[ii].getName()), new MBeanAttributeConfig(__mbaiAttributes[ii].getName()), __tempEventAttributes, __tempTabularEventVector);
                                    	} //if
                                    	else if (__stTelemetryModel.equals("metrics")) {
                                    		
                                    		__tempMetricAtrributeAttributes = new Attributes();
                                    		__tempMetricAtrributeAttributes.put("mbean_attribute_name", __mbaiAttributes[ii].getName());
                                    		__tempMetricAtrributeAttributes.putAll(__tempMetricAttributes);
                                    		Utilities.handleMBeanAttributeAsMetric(_jmxHarvesterConfig, __metricBuffer, _mbeanServer.getAttribute(__oiInstance.getObjectName(), __mbaiAttributes[ii].getName()), __oiInstance.getObjectName().getCanonicalName(), new MBeanAttributeConfig(__mbaiAttributes[ii].getName()), __tempMetricAtrributeAttributes);
                                    	} //else if
                                    	else {
                                    		Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Problem processing MBeans, unknown telemetry model! Check your jmx2insights config.");
                                    	} //else
                                    } //if
                                    else {

                                        Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Attribute " + __mbaiAttributes[ii].getName() + " is not readable. ");
                                    } //else
                                } //for

                            } //try
                            catch (java.lang.Exception _e) {

                                Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Problem interrogating mbean: " + __oiInstance.getObjectName().toString() + " during open harvest.");
                                Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Message MBean Access Fail: " + _e.getMessage());
                            } //catch


                            if (__stTelemetryModel.equals("events")) {
                            	
                            	Utilities.publishInsightsEvent(_jmxHarvesterConfig, __tempEventAttributes);

                                //add any tabular events - this is a poop work around for tabular support and breaks the whole simplicty of this derp derp ...
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
            				        	Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] sending from open ... " + __response.getStatusMessage());
            				        	__metricBuffer = new MetricBuffer(_jmxHarvesterConfig.getGlobalAttributes()); //reset the metric buffer
            				        	
            				        } //try
            				        catch(java.lang.Exception _e) {
            				        	
            				        	Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Problem sending this batch of metrics from open  - " + _e.toString());
            				        } //catch
                            	} //if
                            	
                            } //else if
                            else {
                            	Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Problem processing MBeans, unknown telemetry model! Check your jmx_harvester config. Nothing will be recorded.");
                            	
                            } //else

                        } //while
                                                
                    } //else

                } //if
                else {

                    Agent.LOG.finer("[" + Constantz.EXTENSION_NAME + "] This MBean's polling interval is not satisfied it will be increased by one from: " + _mbeans[i].getMBeanName() + " : " + _mbeans[i].getMBeanPollingCounter());
                    _mbeans[i].incrementMBeanPollingCounter();
                } //else

            } //try
            catch (java.lang.Exception _e) {

                Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Problem loading the mbean: " + _mbeans[i].getMBeanName());
                Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Message MBean Fail: " + _e.getMessage());
            } //catch
        } //for

        if (__metricBuffer.size() > 0) {
        	
        	//send the metrics buffer contents ...
	        try {
	        	
	        	Response __response = _metricBatchSender.sendBatch(__metricBuffer.createBatch());
	        	Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] FINAL sending from open ... " + __response.getStatusMessage());
	        	__metricBuffer = null; //reset the metric buffer
	        	
	        } //try
	        catch(java.lang.Exception _e) {
	        	
	        	Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] Problem sending this FINAL metrics batch of metrics from open  - " + _e.toString());
	        } //catch
        } //if
	} //execute

} //OpenProcessor
