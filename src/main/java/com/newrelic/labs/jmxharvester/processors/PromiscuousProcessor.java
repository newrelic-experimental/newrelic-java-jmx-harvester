package com.newrelic.labs.jmxharvester.processors;

import java.lang.management.ManagementFactory;
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

import com.newrelic.agent.Agent;
import com.newrelic.labs.jmxharvester.Constantz;
import com.newrelic.labs.jmxharvester.JMXHarvesterConfig;
import com.newrelic.labs.jmxharvester.MBeanAttributeConfig;
import com.newrelic.labs.jmxharvester.MBeanConfig;
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.Response;
import com.newrelic.telemetry.metrics.MetricBatchSender;
import com.newrelic.telemetry.metrics.MetricBuffer;

public class PromiscuousProcessor implements IProcessor {

	@Override
	public void execute(MBeanServer _mbeanServer, MBeanConfig[] _mbeans, JMXHarvesterConfig _jmxHarvesterConfig, MetricBatchSender _metricBatchSender) {
		
		//metrics only
        MetricBuffer __metricBuffer = null;
        Set<ObjectInstance> __mbeanInstances = _mbeanServer.queryMBeans(null, null);
        Iterator<ObjectInstance> __iterator = __mbeanInstances.iterator();

        Map<String, Object> __tempEventAttributes = null;
        Vector<Map> __tempTabularEventVector = new Vector<Map>();

        ObjectInstance __oiInstance = null;
        Hashtable<?, ?> __htOIProperties = null;
        MBeanAttributeInfo[] __mbaiAttributes = null;
        MBeanInfo __mbiTempInfo = null;
        
        Attributes __standardAttributes = new Attributes();
        Attributes __tempMetricAtrributeAttributes = null;
        
        try {
        	
        	__metricBuffer = new MetricBuffer(_jmxHarvesterConfig.getGlobalAttributes());
        	
            //loop each of the available MBeans
            while (__iterator.hasNext()) {

                try {

                    __oiInstance = __iterator.next();
                    __tempEventAttributes = new HashMap<String, Object>();
                    __tempEventAttributes.put("MBean", __oiInstance.getObjectName().toString());
                    __htOIProperties = __oiInstance.getObjectName().getKeyPropertyList();               
                    __standardAttributes = new Attributes();
                    __standardAttributes.put("jmx_harvester_mode", "promiscuous");
                    __standardAttributes.put("jmx_harvester_type", "measurement");
                    __standardAttributes.put("object_type", "mbean");
                    __standardAttributes.put("mbean_name", __oiInstance.getObjectName().toString());
                    __standardAttributes.put("mbean_domain", __oiInstance.getObjectName().getDomain());   
                    __mbiTempInfo = _mbeanServer.getMBeanInfo(__oiInstance.getObjectName());
                    __mbaiAttributes = __mbiTempInfo.getAttributes();

                    for (int i = 0; i < __mbaiAttributes.length; i++) {

                        if (__mbaiAttributes[i].isReadable() && __mbaiAttributes[i].getName() != null) {

                        	__tempMetricAtrributeAttributes = new Attributes();
                    		__tempMetricAtrributeAttributes.put("mbean_attribute_name", __mbaiAttributes[i].getName());
                    		__tempMetricAtrributeAttributes.putAll(__standardAttributes);
                        	Utilities.handleMBeanAttributeAsMetric(_jmxHarvesterConfig, __metricBuffer, _mbeanServer.getAttribute(__oiInstance.getObjectName(), __mbaiAttributes[i].getName()), __oiInstance.getObjectName().getCanonicalName(), new MBeanAttributeConfig(__mbaiAttributes[i].getName()), __tempMetricAtrributeAttributes);
                        } //if
                        else {

                            Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "]" + __oiInstance.getObjectName().toString() + ": Contains unreadable or null attributes. ");
                            
                        } //else

                    } //for

                } //try
                catch (java.lang.UnsupportedOperationException _uoe) {

                    Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] MBean operation exception is not supported " + __oiInstance.getObjectName().toString() + " during promiscuous harvest.");
                    Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Message: " + _uoe.getMessage());
                } //catch
                catch (java.lang.Exception _e) {

                    Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Problem interrogating mbean: " + __oiInstance.getObjectName().toString() + " during promiscuous harvest.");
                    Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Message MBean Access Fail: " + _e.getMessage());

                } //catch


                if (__metricBuffer.size() > _jmxHarvesterConfig.getMetricBatchSize()) {

            		//send the metrics buffer contents ...
			        try {
			        	
			        	Response __response = _metricBatchSender.sendBatch(__metricBuffer.createBatch());
			        	Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] sending from promiscuous ... " + __response.getStatusMessage());
			        	__metricBuffer = new MetricBuffer(_jmxHarvesterConfig.getGlobalAttributes()); //reset the metric buffer
			        	
			        } //try
			        catch(java.lang.Exception _e) {
			        	
			        	Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] Problem sending this batch of metrics from promiscuous  - " + _e.toString());
			        } //catch
            	} //if

            } //while
        	
        } //try
        catch(java.lang.Exception _e) {
        	
            Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] Problem duruing promiscuous MBeans inspection.");
            Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] Message: " + _e.getMessage());
        } //catch
        
        
      if (__metricBuffer.size() > 0) {
        	
        	//send the metrics buffer contents ...
	        try {
	        	
	        	Response __response = _metricBatchSender.sendBatch(__metricBuffer.createBatch());
	        	Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] FINAL sending from promiscuous ... " + __response.getStatusMessage());
	        	__metricBuffer = null; //reset the metric buffer
	        	
	        } //try
	        catch(java.lang.Exception _e) {
	        	
	        	Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] Problem sending this FINAL metrics batch of metrics from promiscuous  - " + _e.toString());
	        } //catch
        } //if


        Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] WARNING ::: JMXHarvester is set to promiscuous mode. This will harvest data from all available MBeans (which can be a lot of data). "
                + "Please take caution when enabling this mode for your application. The next promiscuous havest will take place in "
                + _jmxHarvesterConfig.getFrequency() + " minute(s).");
        Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] JMXHarvester promiscuous mode only support metrics as of version 4.2.");
	} //execute

} //PromiscuousProcessor
