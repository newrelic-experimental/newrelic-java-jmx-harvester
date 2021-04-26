
package com.newrelic.gpo.jmx2insights;

/* newrelic agent */
import com.newrelic.agent.Agent;

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
	//interval management
	private int polling_interval = 1; //default
	private int mbean_polling_counter = 1; //default
	
	
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
