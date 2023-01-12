package com.newrelic.labs.jmxharvester;

import com.newrelic.agent.Agent;

/* agent deps */
import com.newrelic.agent.deps.org.json.simple.JSONObject;

public class MBeanOperationConfig {

	private String mbean_config_raw;
	private String mbean_operation_name;
	
	public MBeanOperationConfig(JSONObject _jsoMBeanOperation) {
		
		try {
			
			mbean_operation_name = (String)_jsoMBeanOperation.get("name");
			//TODO - additional support for introspection of the return type or parameters ... 

		} //try
		catch(java.lang.Exception _e) {
			
			Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] Problem getting mbean attribute via cloud config: " + _jsoMBeanOperation.toJSONString()); 
			
		} //catch
	} //MBeanOperationConfig
	
	public MBeanOperationConfig(String _stMBean) {
		
		mbean_config_raw = _stMBean.trim();
		
		//TODO parse the string to enable the operation configuration
	} //MBeanOperationConfig
	
	
	public String getMBeanOperationName() {
		
		return(mbean_operation_name);
	} //getMBeanName
		
} //MBeanOperationConfig