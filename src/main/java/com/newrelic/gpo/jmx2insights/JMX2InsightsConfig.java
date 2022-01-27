package com.newrelic.gpo.jmx2insights;

/* java core */
import java.awt.*;
import java.util.Map;
import java.util.Vector;

/* newrelic agent */
import com.newrelic.agent.Agent;
import com.newrelic.agent.config.BaseConfig;
import com.newrelic.agent.config.LabelsConfig;
import com.newrelic.agent.config.LabelsConfigImpl;
import com.newrelic.gpo.jmx2insights.MBeanConfig;
import com.newrelic.telemetry.Attributes;

/**
 * The configuration management object for the extension. Provides a single place to look up and evaluate the state of the configuration in newrelic.yml.
 * 
 * @author gil
 */
public class JMX2InsightsConfig extends BaseConfig {

    public static final String ENABLED = "enabled";
    public static final Boolean DEFAULT_ENABLED = Boolean.FALSE;
    public static final String MODE = "mode";
    public static final String DEFAULT_MODE = "strict";
    public static final String EVENT_NAME = "event_name";
    public static final String DEFAULT_EVENT_NAME = "JMX";
    public static final String MBEANS = "mbeans";
    public static final String FREQUENCY = "frequency";
    public static final int DEFAULT_FREQUENCY = 1; 
    public static final String PROPERTY_NAME = "jmx2insights";
    public static final String PROPERTY_ROOT = "newrelic.config." + PROPERTY_NAME + ".";
    public static final String MEMORY_EVENTS = "memory_events";
    public static final Boolean DEFAULT_MEMORY_EVENTS = Boolean.FALSE;
    public static final String BETA_FEATURES = "beta_features";
    public static final Boolean DEFAULT_BETA_FEATURES = Boolean.FALSE;
    public static final String LABELS = "labels";
    public static final String TELEMETRY_MODEL = "telemetry_model";
    public static final String DEFAULT_TELEMETRY_MODEL = "metrics";
    public static final String METRIC_PREFIX = "metric_prefix";
    public static final String DEFAULT_METRIC_PREFIX = "labs.jmx";
    
    //defaults
    private boolean isEnabled = false;
    private int frequency = 1;
    private String mode = "strict";
    private String event_name = "JMX";
    private MBeanConfig[] mbeans;
    private MBeanOperationConfig[] mbean_operations;
    private boolean memory_events = false;
    private boolean beta_features = false;
    private String appName = "My Application";
    private String licenseKey = "n0ne";
    private String hostname = "unknown";
    private String entityGuid = "unknown";
    private String metric_prefix = "labs.jmx";
    private String telemetry_model = "metrics";


    private Map<String,String> labels;

	public JMX2InsightsConfig(Map<String, Object> _props) {
		
		super(_props, PROPERTY_ROOT);

		try {
			
			isEnabled = getProperty(ENABLED, DEFAULT_ENABLED).booleanValue();
			mode = getProperty(MODE, DEFAULT_MODE);
			frequency = (getProperty(FREQUENCY, DEFAULT_FREQUENCY)).intValue();
			event_name = getProperty(EVENT_NAME, DEFAULT_EVENT_NAME);
			memory_events = getProperty(MEMORY_EVENTS, DEFAULT_MEMORY_EVENTS);
			beta_features = getProperty(BETA_FEATURES, DEFAULT_BETA_FEATURES);
			metric_prefix = getProperty(METRIC_PREFIX, DEFAULT_METRIC_PREFIX);
			telemetry_model = getProperty(TELEMETRY_MODEL, DEFAULT_TELEMETRY_MODEL);
	        Vector<MBeanConfig> __vTEMP = new Vector<MBeanConfig>();
	        
	        //collect all mbean operations definitions too and stach them as an array too
	        Vector<MBeanOperationConfig> __vOPER_TEMP = new Vector<MBeanOperationConfig>();
	        
	        for (Map.Entry<String, Object> entry : _props.entrySet()) {
	            
	        		if (entry.getKey().contains("mbean_")) {
	        		
	        			__vTEMP.add(new MBeanConfig((entry.getValue()).toString()));
	        		} //if
	        		else if (entry.getKey().contains("operation_")) {
	        		
	        			__vOPER_TEMP.add(new MBeanOperationConfig((entry.getValue()).toString()));
	        		} //else if 
	        } //for
	        
	        //populate the mbean attributes configuration
	        mbeans = new MBeanConfig[__vTEMP.size()];
	        
	        for (int i = 0; i < __vTEMP.size(); i++) {
	        		
	        		mbeans[i] = (MBeanConfig)__vTEMP.get(i);
	        } //for
			
	        mbean_operations = new MBeanOperationConfig[__vOPER_TEMP.size()];
	        
	        for (int i = 0; i < __vOPER_TEMP.size(); i++) {
	        		
	        	mbean_operations[i] = (MBeanOperationConfig)__vOPER_TEMP.get(i);
	        } //for
			
		} //try
		catch(java.lang.Exception _e) {
			
			Agent.LOG.error(Constantz.EXTENSION_LOG_STRING + " Problem loading the JMX2Insights configuration. All features disabled.");
			Agent.LOG.error(Constantz.EXTENSION_LOG_STRING + " Message: " + _e.getMessage());
			isEnabled = false;
			mode = "strict";
			frequency = 1;
			event_name = "JMX";
			memory_events = false;
			beta_features = false;
			metric_prefix = METRIC_PREFIX;
			telemetry_model	= TELEMETRY_MODEL;
		} //catch
        
	} //JMX2InsightsConfig

	public String getTelemetryModel() {
		
		return(telemetry_model);
	} //getTelemetryModel
	
	public String getMetricPrefix() {
		
		return(metric_prefix);
	} //getMetricPrefix
	
    public boolean isEnabled() {
	    	
	    	return(isEnabled);
    } //isEnabled
    
    public boolean memoryEventsEnabled() {
    	
    		return(memory_events);
    } //memoryEventsEnabled
    
    public boolean betaFeaturesEnabled() {
    	
    		return(beta_features);
    } //betaFeaturesEnabled
    
    public String getMode() {
    	
    	return(mode);
    } //getMode
    
    public String getEventName() {
    	
    	return(event_name);
    } //getEventName
    
    public int getFrequency() {
    	
    	return(frequency);
    } //getFrequency
    
    public MBeanConfig[] getMBeans() {
    	
    	return(mbeans);
    } //getMBeans
    	
    public MBeanOperationConfig[] getMBeanOperationConfigs() {
    	
    	return(mbean_operations);
    } //getMBeanOperationConfigs
    
    public boolean operationsDefined() {
    	
    	boolean __bRC = false;
    	
    	if (mbean_operations.length > 0) {
    		
    		__bRC = true;
    	} //if
    	
    	return(__bRC);
    	
    } //operationsDefined

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    public String getAppName() {
    	
    	return(appName);
    } //getAppName
    
    public void setAppName(String _appName) {
    	
    	appName = _appName;
    } //setAppName

    public String getLicenseKey() {
    	
    	return(licenseKey);
    } //getLicenseKey
    
	public void setLicenseKey(String _licenseKey) {
		
		licenseKey = _licenseKey;
	} //setLicenseKey

	public String getHostname() {
		
		return(hostname);
	} //getHostname
	
	public void setHostname(String _hostname) {

		if (_hostname != null) {
			hostname = _hostname;
		} //if
		
		
	} //setHostname

	public String getEntityGuid() {
		
		return(entityGuid);
	} //getEntityGuid
	
	public void setEntityGuid(String _entityGuid) {

		if (_entityGuid != null) {
			entityGuid = _entityGuid;
		} //if		
	} //setEntityGuid

	public Attributes getLabelsAsAttributes() {

		Attributes __attributes = new Attributes();
		
		for (Map.Entry<String, String> entry : labels.entrySet()) {
			__attributes.put(entry.getKey(), entry.getValue());
		} //for
		
		return(__attributes);
		
	} //getLabelsAsAttributes
	
} //JMX2InsightsConfig