package com.newrelic.gpo.jmx2insights;

public class MBeanOperationConfig {

	private String mbean_name;
	
	public MBeanOperationConfig(String _stMBean) {
		
		mbean_name = _stMBean.trim();
		
	} //MBeanOperationConfig
	
	
	public String getMBeanName() {
		
		return(mbean_name);
	} //getMBeanName
		
} //MBeanOperationConfig