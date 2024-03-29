package com.newrelic.labs.jmxharvester;

import com.newrelic.agent.Agent;

/* agent deps */
import com.newrelic.agent.deps.org.json.simple.JSONObject;

public class MBeanAttributeConfig {

	private String mbean_config_raw;
	private String mbean_attribute_name;
	private String[] mbean_attribute_element_headers;
	private boolean b_hasAttributeElements;
	
	/**
	 * 
	 * @param _jsoMBeanAttribute
	 */
	public MBeanAttributeConfig(JSONObject _jsoMBeanAttribute) { 
		
		try {
			
			mbean_attribute_name = (String)_jsoMBeanAttribute.get("name");
			
			//TODO This needs to be able to handle composite and tabular mbean types - the current
			// inventory system does not allow for this need to make appropriate mods to support. 
			b_hasAttributeElements = false;
			mbean_attribute_element_headers = null;
			
			
			if (!b_hasAttributeElements) {
				
				Agent.LOG.finer("[" + Constantz.EXTENSION_NAME + "] No attribute element headers detected for the candidate mbean attribute : " + mbean_attribute_name);
			} //if
			
		} //try
		catch(java.lang.Exception _e) {
			
			Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] Problem getting mbean attribute via cloud config: " + _jsoMBeanAttribute.toJSONString()); 
			
		} //catch
		
	} //MBeanAttributeConfig
	
	/**
	 * 
	 * @param _stMBeanAttribute
	 */
	public MBeanAttributeConfig(String _stMBeanAttribute) {

		mbean_config_raw = _stMBeanAttribute;
		/* manage the attribute name */
		try {
			
			if (_stMBeanAttribute.contains("(")) {
				
				mbean_attribute_name = _stMBeanAttribute.substring(0, _stMBeanAttribute.indexOf('('));
				b_hasAttributeElements = true;
			} //if
			else {
				
				mbean_attribute_name = _stMBeanAttribute.trim();
				b_hasAttributeElements = false;
			} //else
		} //try
		catch (java.lang.ArrayIndexOutOfBoundsException _aiobe) {
			
			Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] Problem derriving the MBean atrribute name: " + _stMBeanAttribute);
			Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] Accessing simple MBean Attributes should be defined as follows: mbean_X: java.lang:type=Threading [ThreadCount,PeakThreadCount,DaemonThreadCount].");
			Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] Accessing javax.management.openmbean.CompositeData or javax.management.openmbean.TabularData MBeans accept the following format :: mbean_X: org.apache.jackrabbit.oak:type=RepositoryStats,name=Oak Repository Statistics [ObservationQueueMaxLength(perminute|perhour),SessionWriteCount(persecond|perminute|perhour),SessionReadAverage(persecond),SessionReadDuration(persecond),SessionReadCount(persecond)]");
			Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] " + _aiobe.getMessage());
		} //catch
		
		/* process the attribute header values */
		try {
			
			if (b_hasAttributeElements) {
				
				mbean_attribute_element_headers = (_stMBeanAttribute.substring(_stMBeanAttribute.indexOf('(') + 1, _stMBeanAttribute.lastIndexOf(')'))).split("[|]");
				Agent.LOG.finer("JMXHarvester number of mbean header elements " + mbean_attribute_element_headers.length);
			} //if
			else {
				
				Agent.LOG.finer("[" + Constantz.EXTENSION_NAME + "] No attribute element headers detected for the candidate mbean attribute : " + _stMBeanAttribute);
			} //else
			
		} //try
		catch(java.lang.ArrayIndexOutOfBoundsException _aiobe) {
			
			Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] Problem derriving the MBean atrribute element headers to interrogate: " + _stMBeanAttribute);
			Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] Accessing simple MBean Attributes should be defined as follows: mbean_X: java.lang:type=Threading [ThreadCount,PeakThreadCount,DaemonThreadCount].");
			Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] Accessing javax.management.openmbean.CompositeData or javax.management.openmbean.TabularData MBeans accept the following format :: mbean_X: org.apache.jackrabbit.oak:type=RepositoryStats,name=Oak Repository Statistics [ObservationQueueMaxLength(perminute|perhour),SessionWriteCount(persecond|perminute|perhour),SessionReadAverage(persecond),SessionReadDuration(persecond),SessionReadCount(persecond)]");
			Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] " + _aiobe.getMessage());
		} //catch
		
	} //MBeanAttributeConfig
	
	/**
	 * 
	 * @param _stCandidateElement
	 * @return boolean 
	 */
	public boolean hasAttributeElement(String _stCandidateElement) {
		
		// do we have the given string as part of this attribute config?
		//doing it as a comparator for loop for Java6 support - don't want to use
		//array classes in case we're running in an old VM
		boolean __bRC = false;
		
		/* in the case of no header provided we're going to assume all headers match */
		if (!hasAttributeElements()) {
			
			__bRC = true;
		} //if
		else {
			
			for (int i=0; i < mbean_attribute_element_headers.length;i++) {
				
				//Agent.LOG.fine("JMXHarvester THE attributeelement being compared: " + _stCandidateElement + " to " + mbean_attribute_element_headers[i]);
				if (_stCandidateElement.equalsIgnoreCase(mbean_attribute_element_headers[i])) {
					
					__bRC = true;
					break;
				} //if
			} //for
		} //else
		
		//Agent.LOG.fine("returning " + __bRC);
		
		return(__bRC);
	} //hasAttributeElement
	
	public boolean hasAttributeElements() {
		
		return(b_hasAttributeElements);
	} //hasAttributeElements
	
	public String getAttributeName() {
		
		return(mbean_attribute_name);
	} //getAttributeName
	
	public String[] getAttributeElementHeaders() {
		
		return(mbean_attribute_element_headers);
	} //getAttributeName
	
} //MBeanAttributeConfig