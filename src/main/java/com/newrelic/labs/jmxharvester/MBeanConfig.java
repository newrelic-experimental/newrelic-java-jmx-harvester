
package com.newrelic.labs.jmxharvester;

import java.util.Iterator;
import java.util.Vector;

/* newrelic agent */
import com.newrelic.agent.Agent;

/* agent deps */
import com.newrelic.agent.deps.org.json.simple.JSONObject;
import com.newrelic.agent.deps.org.json.simple.JSONArray;

/**
 * Represents the configuration options to be found in newrelic.yml jmx2insights configuration section.
 * 
 * @author gil@newelic.com
 */
public class MBeanConfig {
	
	private String mbean_name;
	//old implementation - this will now store the raw forms of MBean definition
	private String[] mbean_attributes;
	//new implementation
	private MBeanAttributeConfig[] mbean_attribute_configs;
	private MBeanOperationConfig[] mbean_operation_configs;
	//interval management
	private int polling_interval = 1; //default
	private int mbean_polling_counter = 1; //default
	
	/**
	 * Creates a new instance of an MBean configuration based on the cloud config structure of jmx-harvester nr1 app. 
	 * @param __mbean_document
	 * @throws MBeanConfigException
	 */
	MBeanConfig(JSONObject __mbean_document) throws MBeanConfigException {
		
		MBeanAttributeConfig[] __mbean_attribute_configs = null;
		MBeanOperationConfig[] __mbean_operation_configs = null;
		Iterator<JSONObject> __array_iterator;
		JSONObject __tmpObj = null;
		Vector<MBeanAttributeConfig> __vAttributes = new Vector();
		Vector<MBeanOperationConfig> __vOperations = new Vector();
		
		
		try {
			
			mbean_name = (String)__mbean_document.get("domain") + ":type=" + (String)__mbean_document.get("name");
			Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] MBean >>>>>  " + mbean_name); //TODO remove / make debug ... 
			polling_interval = 1; //ugh I fucked up
			mbean_attributes = new String[] {"cloud config"};
			
			JSONArray __attributes = (JSONArray)__mbean_document.get("attributes");
			
			if (__attributes.size() > 0) {
				
				
				__array_iterator = __attributes.iterator();
				while(__array_iterator.hasNext()) {
					
					__tmpObj = __array_iterator.next();
					__vAttributes.add(new MBeanAttributeConfig(__tmpObj));
				} //while
				
			} //if
			
			JSONArray __operations = (JSONArray)__mbean_document.get("operations");
			
			if (__operations.size() > 0) {
				
				__array_iterator = __operations.iterator();
				while(__array_iterator.hasNext()) {
					
					__tmpObj = __array_iterator.next();
					__vOperations.add(new MBeanOperationConfig(__tmpObj));
				} //while
			} //if
			
			
			//attributes and operations processed, update the class level objects
			if (__vAttributes.size() > 0) {
				
				__mbean_attribute_configs = new MBeanAttributeConfig[__vAttributes.size()];
				for (int i = 0; i < __vAttributes.size(); i++) {
				
					__mbean_attribute_configs[i] = (MBeanAttributeConfig)__vAttributes.get(i);
				} //for
				
				mbean_attribute_configs = __mbean_attribute_configs;
			}//if
			
			if (__vOperations.size() > 0) {
				
				__mbean_operation_configs = new MBeanOperationConfig[__vOperations.size()];
				
				for (int j = 0; j < __vOperations.size(); j++) {
					
					__mbean_operation_configs[j] = (MBeanOperationConfig)__vOperations.get(j);
				} //for
				
				mbean_operation_configs = __mbean_operation_configs;
			} //if
		} //try
		catch (java.lang.Exception _e) {
			
			throw new MBeanConfigException("Problem creating mbean from document: " + __mbean_document.toJSONString());
			
		} //catch

	} //MBeanConfig
	
	/**
	 * Creates an MBean configuration object given the String which is the config for an individual MBean in newrelic.yml
	 * (e.g.   mbean_0: java.lang:type=Threading [ThreadCount,PeakThreadCount,DaemonThreadCount])
	 * @param _stMBean Sting represntation of a newrelic.yml mbean config
	 */
	MBeanConfig(String _stMBean) {
		
		/* manage the polling interval */
		try {
			

			//capture the polling interval if defined 
			if (_stMBean.lastIndexOf("]") + 1 < _stMBean.length()) {
				
				Agent.LOG.debug("[" + Constantz.EXTENSION_NAME + "] FOUND override interval defined for MBean: " + _stMBean);
				Integer __iTEMP = new Integer(_stMBean.substring(_stMBean.indexOf(']') + 1).trim());
				polling_interval = __iTEMP.intValue();
				Agent.LOG.finer("[" + Constantz.EXTENSION_NAME + "] The polling interval has been defined as: " + polling_interval);
			} //if
			else {
				
				Agent.LOG.finer("[" + Constantz.EXTENSION_NAME + "] No override interval defined for MBean: " + _stMBean);
				polling_interval = 1;
			} //else
			
		} //try
		catch(java.lang.Exception _e) {
			
			Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] Problem defining the polling interval for the mbean: " + _stMBean);
			Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] Polling interval should be defined as follows: mbean_X: java.lang:type=Threading [ThreadCount,PeakThreadCount,DaemonThreadCount]5.");
			Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] Numeric polling interval to immediately follow the final square bracket in the mbean deinition line and must be numeric.");
			Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] The polling interval will be set to the default.");
			Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] " + _e.getMessage());
			//fall back to defaults
			polling_interval = 1;
		} //catch
		
		/* manage the mbean name */
		try {	
			
			if (_stMBean.contains("[")) {
				
				mbean_name = _stMBean.substring(0, _stMBean.indexOf('[') - 1);				
			} //if
			else {
				mbean_name = _stMBean.trim();
			} //else

		} //try
		catch (java.lang.ArrayIndexOutOfBoundsException _aiobe) {
			
			Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] Problem derriving the name of the MBean to interrogate: " + _stMBean);
			Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] Accessing simple MBean Attributes should be defined as follows: mbean_X: java.lang:type=Threading [ThreadCount,PeakThreadCount,DaemonThreadCount].");
			Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] Accessing javax.management.openmbean.CompositeData or javax.management.openmbean.TabularData MBeans accept the following format :: mbean_X: org.apache.jackrabbit.oak:type=RepositoryStats,name=Oak Repository Statistics [ObservationQueueMaxLength(perminute|perhour),SessionWriteCount(persecond|perminute|perhour),SessionReadAverage(persecond),SessionReadDuration(persecond),SessionReadCount(persecond)]");
			Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] " + _aiobe.getMessage());
		} //catch

		/* manage the mbean attributes - some might be complex */
		try {
			
			mbean_attributes = (_stMBean.substring(_stMBean.indexOf('[') + 1, _stMBean.lastIndexOf(']'))).split("[,]");
			mbean_attribute_configs = new MBeanAttributeConfig[mbean_attributes.length];
			
			for (int __i = 0; __i < mbean_attributes.length; __i++) {
				
				mbean_attribute_configs[__i] = new MBeanAttributeConfig(mbean_attributes[__i]);
			} //for
			
		} //try
		catch (java.lang.ArrayIndexOutOfBoundsException _aiobe) {
			
			Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] Problem derriving the MBean atrributes to interrogate: " + _stMBean);
			Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] Accessing simple MBean Attributes should be defined as follows: mbean_X: java.lang:type=Threading [ThreadCount,PeakThreadCount,DaemonThreadCount].");
			Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] Accessing javax.management.openmbean.CompositeData or javax.management.openmbean.TabularData MBeans accept the following format :: mbean_X: org.apache.jackrabbit.oak:type=RepositoryStats,name=Oak Repository Statistics [ObservationQueueMaxLength(perminute|perhour),SessionWriteCount(persecond|perminute|perhour),SessionReadAverage(persecond),SessionReadDuration(persecond),SessionReadCount(persecond)]");
			Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] " + _aiobe.getMessage());
		} //catch
		
	} //MBeanConfig
	
	public String getMBeanName() {
		
		return(mbean_name);
	} //getMBeanName
	
	
	public String[] getMBeanAttributesAsString() {
		
		return(mbean_attributes);
	} //getMBeanAttributes

	public MBeanAttributeConfig[] getMbeanAttributeObjects() {
		
		return(mbean_attribute_configs);
	}//MBeanAttributeConfig
	
	public void setMBeanName(String _mbean_name) {
		
		mbean_name = _mbean_name;		
	} //setMBeanName
	
	public int getPollingInterval() {
		
		return(polling_interval);
	} //getPollingInterval
	
	public int getMBeanPollingCounter() {
		
		return(mbean_polling_counter);
	} //getMBeanPollingCounter
	
	public void setMBeanPollingCounter(int _mbean_polling_counter) {
		
		mbean_polling_counter = _mbean_polling_counter;
	} //setMBeanPollingCounter
	
	public void resetMBeanPollingCounter() {
		
		mbean_polling_counter = 1;
	} //resetMBeanPollingCounter
	
	public void incrementMBeanPollingCounter() {
		
		mbean_polling_counter = mbean_polling_counter + 1;
	} //incrementMBeanPollingCounter
	
} //MBeanConfig
