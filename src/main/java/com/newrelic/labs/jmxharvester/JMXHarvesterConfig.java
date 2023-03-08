package com.newrelic.labs.jmxharvester;

/* java core */
import java.awt.*;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

/* newrelic agent */
import com.newrelic.agent.Agent;
import com.newrelic.agent.config.BaseConfig;
import com.newrelic.agent.config.LabelsConfig;
import com.newrelic.agent.config.LabelsConfigImpl;
import com.newrelic.agent.deps.org.json.simple.JSONArray;
import com.newrelic.agent.deps.org.json.simple.JSONObject;
import com.newrelic.labs.jmxharvester.MBeanConfig;
import com.newrelic.telemetry.Attributes;

/**
 * The configuration management object for the extension. Provides a single place to look up and evaluate the state of the configuration in newrelic.yml.
 * 
 * @author gil
 */
public class JMXHarvesterConfig extends BaseConfig {

    public static final String ENABLED = "enabled";
    public static final Boolean DEFAULT_ENABLED = Boolean.FALSE;
    public static final String MODE = "mode";
    public static final String DEFAULT_MODE = "strict";
    public static final String EVENT_NAME = "event_name";
    public static final String DEFAULT_EVENT_NAME = "JMX";
    public static final String MBEANS = "mbeans";
    public static final String FREQUENCY = "frequency";
    public static final int DEFAULT_FREQUENCY = 1; 
    public static final String PROPERTY_NAME = "jmx_harvester";
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
    public static final String METRIC_BATCH_SIZE = "metric_batch_size";
    public static final int DEFAULT_METRIC_BATCH_SIZE = 200;
    public static final String INVENTORY_FREQUENCY = "inventory_frequency";
    public static final int DEFAULT_INVENTORY_FREQUENCY = 1440; 
    public static final String NRCLOUD_CONFIG_ENABLED = "nrcloud_config_enabled";
    public static final Boolean DEFAULT_NRCLOUD_CONFIG_ENABLED = Boolean.FALSE;
    public static final String NRCLOUD_CONFIG_UUID = "nrcloud_config_uuid";
    public static final String NRCLOUD_CONFIG_KEY= "nrcloud_config_key";
    public static final String NRCLOUD_CONFIG_FREQUENCY = "nrcloud_config_frequency";
    public static final int DEFAULT_NRCLOUD_CONFIG_FREQUENCY = 60;
    public static final String NRCLOUD_REGION = "nrcloud_region";
    public static final String DEFAULT_NRCLOUD_REGION = "us";
    
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
    private int metric_batch_size = 200;
    private String telemetry_model = "metrics";
    private int inventory_frequency = 1440;
    private boolean nrcloud_config_enabled = false;
    private String nrcloud_config_uuid = null;
    private String nrcloud_config_key = null;
    private int nrcloud_config_frequency = 60;
    private String nrcloud_region = "us";

    private Map<String,String> labels;
    private Attributes globalAttributes = new Attributes();
    
	public JMXHarvesterConfig(Map<String, Object> _props) {
		
		super(_props, PROPERTY_ROOT);

		try {
			
			isEnabled = getProperty(ENABLED, DEFAULT_ENABLED).booleanValue();
			mode = getProperty(MODE, DEFAULT_MODE);
			frequency = (getProperty(FREQUENCY, DEFAULT_FREQUENCY)).intValue();
			event_name = getProperty(EVENT_NAME, DEFAULT_EVENT_NAME);
			memory_events = getProperty(MEMORY_EVENTS, DEFAULT_MEMORY_EVENTS);
			beta_features = getProperty(BETA_FEATURES, DEFAULT_BETA_FEATURES);
			metric_prefix = getProperty(METRIC_PREFIX, DEFAULT_METRIC_PREFIX);
			metric_batch_size = getProperty(METRIC_BATCH_SIZE, DEFAULT_METRIC_BATCH_SIZE);
			telemetry_model = getProperty(TELEMETRY_MODEL, DEFAULT_TELEMETRY_MODEL);
			inventory_frequency = (getProperty(INVENTORY_FREQUENCY, DEFAULT_INVENTORY_FREQUENCY)).intValue();
			nrcloud_config_enabled = getProperty(NRCLOUD_CONFIG_ENABLED, DEFAULT_NRCLOUD_CONFIG_ENABLED);
			nrcloud_config_uuid = getProperty(NRCLOUD_CONFIG_UUID);
			nrcloud_config_key = getProperty(NRCLOUD_CONFIG_KEY); 
			nrcloud_config_frequency = getProperty(NRCLOUD_CONFIG_FREQUENCY, DEFAULT_NRCLOUD_CONFIG_FREQUENCY);
			nrcloud_region = getProperty(NRCLOUD_REGION, DEFAULT_NRCLOUD_REGION);
	        Vector<MBeanConfig> __vTEMP = new Vector<MBeanConfig>();
	     
	        //collect all mbean operations definitions too and stash them as an array too
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
			
	        //TODO Removed due to second step to configure labels as metric attributes (version 4.2+)
	        //initialize the global attributes with labels  
	        //globalAttributes.putAll(this.getLabelsAsAttributes());
		} //try
		catch(java.lang.Exception _e) {
			
			Agent.LOG.error(Constantz.EXTENSION_LOG_STRING + " Problem loading the JMXHarvester configuration. All features disabled.");
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
        
	} //JMXHarvesterConfig

	public String getTelemetryModel() {
		
		return(telemetry_model);
	} //getTelemetryModel
	
	public String getMetricPrefix() {
		
		return(metric_prefix);
	} //getMetricPrefix
	
	public int getMetricBatchSize() {
		
		return(metric_batch_size);
	} //getMetricBatchSize 
	
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
    
    public int getInventoryFrequency() {
    	
    	return(inventory_frequency);
    } //getInventoryFrequency
    
    public boolean isCloudConfigEnabled() {
    	return(nrcloud_config_enabled);
    } //nrCloudConfigEnabled
 
    public String getCloudConfigUUID() {
    	return(nrcloud_config_uuid);
    } //getCloudConfigUUID
    
    public String getCloudConfigKey() {
    	return(nrcloud_config_key);
    } //getCloudConfigKey
    
    public int getCloudConfigFrequency() {
    	
    	return(nrcloud_config_frequency);
    } //getCloudConfigFrequency
    
    public String getCloudRegion() {
    	
    	return(nrcloud_region);
    } //getCloudRegion
    
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
    } //getLabels

    public void setLabels(Map<String, String> labels) {
        
    	this.labels = labels;
    	//initialize the global attributes with labels
        globalAttributes.putAll(this.getLabelsAsAttributes());
    } //setLabels

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
		
		if (labels != null) {
			
			for (Map.Entry<String, String> entry : labels.entrySet()) {
				__attributes.put(entry.getKey(), entry.getValue());
			} //for			
		} //if
		else {
			Agent.LOG.fine(Constantz.EXTENSION_LOG_STRING + " No labels defined, none will be added to attributes.");
		} //else
		
		return(__attributes);
		
	} //getLabelsAsAttributes
	
	public void setCloudConfig(JSONArray __doc_collection) {

		Agent.LOG.info("calling setCloudConfig with: " + __doc_collection.toJSONString());
		//MBeanConfig[] __local_mbeans = new MBeanConfig[0];
		Vector<MBeanConfig> __vMBeans = new Vector<MBeanConfig>();
		MBeanConfig[] __mbeans = null;
		JSONObject __envelope = null;
		JSONObject __document = null;
		
		String __type = null;
		Iterator<JSONObject> __docIterator = __doc_collection.iterator();
		boolean __memory_events = DEFAULT_MEMORY_EVENTS;
		boolean __beta_features = DEFAULT_BETA_FEATURES;
		boolean __isEnabled = DEFAULT_ENABLED;
		boolean __nrcloud_config_enabled = DEFAULT_NRCLOUD_CONFIG_ENABLED;
		String __telemetry_model = DEFAULT_TELEMETRY_MODEL;
		String __event_name = DEFAULT_EVENT_NAME;
		String __metric_prefix = DEFAULT_METRIC_PREFIX;
		String __mode = DEFAULT_MODE;
		int __frequency = DEFAULT_FREQUENCY;
		int __inventory_frequency = DEFAULT_INVENTORY_FREQUENCY;
		
		try {
			
			while (__docIterator.hasNext()) {
				
				__envelope = __docIterator.next();
				__document = (JSONObject)__envelope.get("document");
				__type = (String)__document.get("documentType");
				
				Agent.LOG.info("wtf type did I encounter: " + __type);
				
				if (__type.equals(Constantz.MBEAN_DOC_TYPE)) {
					
					Agent.LOG.info("processing mbean doc type");
					
					__vMBeans.add(new MBeanConfig(__document));
				} //if
				else if (__type.equals(Constantz.AGENT_CONFIG_DOC_TYPE)) {
		
					//TODO test and validate these? ...
					__mode = (String)__document.get("mode");
					__event_name = (String)__document.get("event_name");
					__metric_prefix = (String)__document.get("event_name");
					__frequency = ((Long)__document.get("frequency")).intValue();
					//__nrcloud_config_enabled = ((Boolean)__document.get("cloudConfigEnabled")).booleanValue();
					__inventory_frequency = ((Long)__document.get("inventoryFrequency")).intValue();
					__telemetry_model = (String)__document.get("telemetryModel");
					__isEnabled = ((Boolean)__document.get("enabled")).booleanValue();
					
					if (((String)__document.get("betaFeatures")).equalsIgnoreCase("off")) {
						__beta_features = false;
					} //if
					else {
						__beta_features = true;
					} //else
					
					
					if (((String)__document.get("memoryEvents")).equalsIgnoreCase("off")) {
						__memory_events = false;
					} //if
					else {
						__memory_events = true;
					} //else
			            
				} //else if
				else {
					
					Agent.LOG.error(Constantz.EXTENSION_LOG_STRING + " Encountered an unexpected document type for jmx harvester. " + __document.toJSONString());
					//TODO set config to default client side 
				} //else
				
			} //while
			
			//populate the main system config
			memory_events = __memory_events;
			beta_features = __beta_features;
			isEnabled = __isEnabled;
			telemetry_model = __telemetry_model;
			inventory_frequency = __inventory_frequency;
			//nrcloud_config_enabled = __nrcloud_config_enabled; - only client side?
			frequency = __frequency;
			metric_prefix = __metric_prefix;
			event_name = __event_name;
			mode = __mode;
			
		    //populate the mbean config
		    if (__vMBeans.size() > 0) {
		    	
		    	__mbeans = new MBeanConfig[__vMBeans.size()];
		    	
		    	for (int i = 0; i < __mbeans.length; i++) {
		    		
		    		__mbeans[i] = (MBeanConfig)__vMBeans.get(i);
		    	} //for
//		    	mbeans = (MBeanConfig[]) __vMBeans.toArray();
		    	
		    	mbeans = __mbeans;
		    } //if
		    
		    
		    
		    
		} //try
		catch (java.lang.Exception _e) {
			
			Agent.LOG.error(Constantz.EXTENSION_LOG_STRING + " Unexpected problem processing with cloud config. " + __document.toJSONString());
			Agent.LOG.error(Constantz.EXTENSION_LOG_STRING + " Will revent to Agent config to defaults.");
			Agent.LOG.error(Constantz.EXTENSION_LOG_STRING + _e.getMessage());	
			memory_events = DEFAULT_MEMORY_EVENTS;
			beta_features = DEFAULT_BETA_FEATURES;
			isEnabled = DEFAULT_ENABLED;
			telemetry_model = DEFAULT_TELEMETRY_MODEL;
			inventory_frequency = DEFAULT_INVENTORY_FREQUENCY;
			nrcloud_config_enabled = DEFAULT_NRCLOUD_CONFIG_ENABLED;
			frequency = DEFAULT_FREQUENCY;
			metric_prefix = DEFAULT_METRIC_PREFIX;
			event_name = DEFAULT_EVENT_NAME;
			mode = DEFAULT_MODE;
		} //catch

		
		Agent.LOG.info(Constantz.EXTENSION_LOG_STRING + " Cloud config should now be changed. ");
		
	} //setCloudConfig
	
	public void addGlobalAttribute(String _key, String _attribute) {
		
		globalAttributes.put(_key, _attribute);
	} //addGlobalAttribute
	
	//TODO add remove attribute method?
	
	public Attributes getGlobalAttributes() {
		
		return(globalAttributes);
	} //getGlobalAttributes
	
	
} //JMXHarvesterConfig