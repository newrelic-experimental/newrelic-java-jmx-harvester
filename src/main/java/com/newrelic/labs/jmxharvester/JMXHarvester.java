package com.newrelic.labs.jmxharvester;

/* java core */
import javax.management.MBeanServer;
import javax.management.MBeanInfo;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanOperationInfo;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import java.net.InetAddress;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

/* newrelic agent */
import com.newrelic.agent.Agent;
import com.newrelic.agent.HarvestListener;
import com.newrelic.agent.HarvestService;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.labs.jmxharvester.Constantz;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigListener;
import com.newrelic.agent.deps.com.google.common.collect.Multiset.Entry;

/* telemetry sdk */
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.MetricBatchSenderFactory;
import com.newrelic.telemetry.http.HttpPoster;
import com.newrelic.telemetry.metrics.Gauge;
import com.newrelic.telemetry.metrics.MetricBatchSender;
import com.newrelic.telemetry.metrics.MetricBuffer;
import java.util.function.Supplier;
import com.newrelic.telemetry.SenderConfiguration.SenderConfigurationBuilder;
import com.newrelic.telemetry.Response;

/* experimental */
//import com.newrelic.agent.transport.apache.ApacheHttpClientWrapper;
//import com.newrelic.agent.transport.HttpClientWrapper;
//
//import com.newrelic.agent.deps.org.apache.http.impl.client.CloseableHttpClient;
//import com.newrelic.agent.deps.org.apache.http.client.methods.HttpPost;


import com.newrelic.agent.deps.org.apache.http.client.methods.CloseableHttpResponse;
import com.newrelic.agent.deps.org.apache.http.client.methods.HttpPost;
import com.newrelic.agent.deps.org.apache.http.client.methods.HttpGet;
import com.newrelic.agent.deps.org.apache.http.entity.StringEntity;
import com.newrelic.agent.deps.org.apache.http.impl.client.CloseableHttpClient;
import com.newrelic.agent.deps.org.apache.http.impl.client.HttpClients;
import com.newrelic.agent.deps.org.apache.http.util.EntityUtils;
import com.newrelic.agent.deps.org.json.simple.JSONObject;
import com.newrelic.agent.deps.org.json.simple.JSONArray;
import com.newrelic.agent.deps.org.json.simple.parser.JSONParser;

import java.io.IOException;


/**
 * The JMXHarvester service object
 *
 * @author gil@newrelic.com
 */
public class JMXHarvester extends AbstractService implements HarvestListener {

    private int invocationCounter = 1;
    private int inventoryCounter = 0;
    private int cloudConfigCounter = -9987;

    private JMXHarvesterConfig jmxHarvesterConfig = null;
    private MemoryEventsThread memoryEventsThread = null; //first implementation of the memory thread.
    
    //telemetry sdk 
    private MetricBatchSender metricBatchSender;
    private Attributes globalAttributes = new Attributes();
    	
                
    public JMXHarvester() {

        super(JMXHarvester.class.getSimpleName());
        Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Initializing Service Class.");

    } //JMXHarvester

    @Override
    public boolean isEnabled() {

        //always enabled - true --> configuration of agent provided through newrelic.yml file dynamically.
        return (true);
    } //isEnabled

    @Override
    public void afterHarvest(String _appName) {

        Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] after harvest event start.");
        Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] after harvest event end.");        
    } //afterHarvest

    @Override
    public void beforeHarvest(String _appName, StatsEngine _statsEngine) {

         
        /* measure the duration of the harvest operation for jmx data */
        long __lBeginTimestamp = System.currentTimeMillis();

        Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] before harvest event start.");

        try {

            Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Processing harvest event for Agent.");

            if (jmxHarvesterConfig != null) {

                if (jmxHarvesterConfig.isEnabled()) {

                    Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Enabled.");

                    //execute interval counter should the frequency be equal to the counter
                    if (jmxHarvesterConfig.getFrequency() == invocationCounter) {

                        Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Matched invocation counter ... execute ops pending ....");
                        if (jmxHarvesterConfig.getMode().equals("inventory")) {

                            Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Starting inventory jmx harvest.");
                            executeInventory();
                            Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Finished inventory jmx harvest.");

                        } //if
                        else if (jmxHarvesterConfig.getMode().equals("strict")) {

                            Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Starting strict jmx harvest.");
                            executeStrict();
                            Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Finished strict jmx harvest.");

                        } //else if
                        else if (jmxHarvesterConfig.getMode().equals("promiscuous")) {

                            Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Starting promiscuous jmx harvest.");
                            executePromiscuous();
                            Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Finished promiscuous jmx harvest.");

                        } //else if
                        else if (jmxHarvesterConfig.getMode().equals("open")) {

                            Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Starting open jmx harvest.");
                            executeOpen();
                            Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Finished open jmx harvest.");
                        } //else if
                        else {

                            Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Unknown JMXHarvester configuration mode: " + jmxHarvesterConfig.getMode() + " must be one of strict, open, inventory, or promiscuous.");
                        } //else

                        //process the listed operations
                        if (jmxHarvesterConfig.operationsDefined()) {

                            Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Starting MBean operations execution.");
                            executeOperations();
                            Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Finished MBean operations execution.");
                        } //if
                        else {

                            Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] No MBean operations have been defined.");
                        } //else

                        //reset the invocation counter to 1.
                        invocationCounter = 1;
                    } //if
                    else {

                        if (invocationCounter > jmxHarvesterConfig.getFrequency()) {

                            Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Invocation counter was placed in an unexpected state. Resetting to 1. ");
                            invocationCounter = 1;

                        } //if
                        else {

                            invocationCounter++;
                            Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Skipping the harvest. " + invocationCounter);
                        } //else

                    } //else

                } //if
                else {

                    Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Disabled.");
                } //else

                /* 
                 * Inventory is run on a separate timer defined in the configuration. If the mode is inventory we ignore the timed invocation. 
                 */
                if (inventoryCounter >= jmxHarvesterConfig.getInventoryFrequency() && !jmxHarvesterConfig.getMode().equals(Constantz.MODE_INVENTORY)) {
                	
                	long __ctms = java.lang.System.currentTimeMillis();
                	Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Starting inventory jmx harvest.");
                	executeInventory();
                  	Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Finished inventory jmx harvest. " + "Inventory duration: " + (java.lang.System.currentTimeMillis() - __ctms) + " (ms).");
                	inventoryCounter = 0;
                } //if
                
                inventoryCounter++; //incremented each 1 minute harvest
                
            } //if

        } //try
        catch (java.lang.Exception _e) {

            Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] Error during harvest of JMX MBeans Message: " + _e.getLocalizedMessage());
            Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] Error during harvest of JMX MBeans Cause: " + _e.getCause());
            Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] Error during harvest of JMX MBeans Type: " + _e.getClass());

        } //catch

        Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] before harvest event end.");
        Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] JMX Harvest duration: " + (System.currentTimeMillis() - __lBeginTimestamp) + " (ms)");

    } //beforeHarvest

    @Override
    protected void doStart() throws Exception {
	// Extension Service is running before Harvest Service is initialized
    // So this code waits 60 seconds, otherwise you could get NPE
    // Added retry logic in case Harvest Service is not running after 60 seconds.
    final JMXHarvester coquette = this;
    final int sleepTime = 60000;
    new java.util.Timer().schedule(
      
      new java.util.TimerTask() {
        
		@Override
        public void run() {
	            
          Agent.LOG.finer("[" + Constantz.EXTENSION_NAME + "] Sleep time: " + sleepTime);
          int attempts = 1;
          int maxAttempts = 3;
          HarvestService hs = null;
          while (attempts < (maxAttempts + 1) && hs == null)
          {
            hs = ServiceFactory.getHarvestService();
            if (hs == null)
            {
              if (attempts < maxAttempts)
              {
                Agent.LOG.finer("[" + Constantz.EXTENSION_NAME + "] Harvest service not available. Retrying. Attempt: "+attempts);
                try
                {
                  Thread.sleep(sleepTime);
                }
                catch(InterruptedException ex)
                {
                  Thread.currentThread().interrupt();
                  Agent.LOG.finer("[" + Constantz.EXTENSION_NAME + "] Sleep during retry attempt interrupted on attempt: "+attempts);
                }
              }
              else 
              {
                Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] Unable to register as harvest listener after "+attempts+" attempts");
              }
            }
            else 
            {
              Agent.LOG.finer("[" + Constantz.EXTENSION_NAME + "] Harvest service found on attempt "+attempts);
            }
          attempts++;
          }
          try {
            ServiceFactory.getHarvestService().addHarvestListener(coquette);
	        }
          catch (NullPointerException ex)
          {
             Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] Harvest listener registration failed. JMX events will not be available.");
          }
          Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] The service has been registered to the harvest listener");	
          
          getCoquetteConfig(null);
          //have to wait until listener is up
          //memoryEventsThread = new MemoryEventsThread(jmx2insightsConfig.memoryEventsEnabled());
          memoryEventsThread = new MemoryEventsThread(jmxHarvesterConfig, metricBatchSender, globalAttributes);
          Thread thMemoryEvents = new Thread(memoryEventsThread);
          thMemoryEvents.start(); 
          Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Memory events thread started.");	
         } 
      },
      	sleepTime
    );
    
    ServiceFactory.getConfigService().addIAgentConfigListener(configListener);	
    getCoquetteConfig(null); //initialize the config
		
	} //doStart


    @Override
    protected void doStop() throws Exception {

        Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] The service is stopping.");
    } //doStop

    @SuppressWarnings("unchecked")
    private void getCoquetteConfig(AgentConfig _agentConfig) {

        try {

            if (_agentConfig == null) {
            	
                _agentConfig = ServiceFactory.getConfigService().getLocalAgentConfig();
            } //if
            
            Map<String, Object> props = _agentConfig.getProperty(Constantz.YML_SECTION);
            Map<String, String> metadata = NewRelic.getAgent().getLinkingMetadata();
            
            jmxHarvesterConfig = new JMXHarvesterConfig(props);
            jmxHarvesterConfig.setLabels(_agentConfig.getLabelsConfig().getLabels());
            jmxHarvesterConfig.setAppName(_agentConfig.getApplicationName());
            jmxHarvesterConfig.setLicenseKey(_agentConfig.getLicenseKey());            
            jmxHarvesterConfig.setHostname(metadata.get("hostname"));
            jmxHarvesterConfig.setEntityGuid(metadata.get("entity.guid"));
            Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Successfully loaded JMXHarvester Configuration");
            Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Configuration = " + props);
            Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] labels = " + _agentConfig.getLabelsConfig().getLabels());
        } //try
        catch (java.lang.Exception _e) {

            Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] Problem loading the jmx2insights config from newrelic.yml. JMXHarvester set to enabled: false.");
            Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] Message: " + _e.getMessage());

            Map<String, Object> __disabledConfig = new HashMap<String, Object>();
            __disabledConfig.put("enabled", "false");
            jmxHarvesterConfig = new JMXHarvesterConfig(__disabledConfig);
        } //catch

        setUpMetricsSystem();
    } //getCouquetteConfig
 
    /**
     * Gets the jmx harvester config from nerdstore and sets the current config object
     */
    @SuppressWarnings("unchecked")
	private void getCloudConfig() {
    	
    	String __result = "";
    	HttpPost __post = null;
    	    	
    	Agent.LOG.info("counter: " + cloudConfigCounter);//devel
    	Agent.LOG.info("freq config: " + jmxHarvesterConfig.getCloudConfigFrequency());//devel
    	Agent.LOG.info("is enabled: " + jmxHarvesterConfig.isCloudConfigEnabled());//devel
    	
    	if ((cloudConfigCounter == -9987 || cloudConfigCounter >= jmxHarvesterConfig.getCloudConfigFrequency()) && jmxHarvesterConfig.isCloudConfigEnabled()) {
    		cloudConfigCounter = 0;
    		Agent.LOG.debug("[" + Constantz.EXTENSION_NAME + "] Cloud config check begin.");
			try {

				if (jmxHarvesterConfig.getCloudRegion().equalsIgnoreCase("us")) {
					
					Agent.LOG.debug("[" + Constantz.EXTENSION_NAME + "] Cloud region set to United States of America.");
					__post = new HttpPost(Constantz.US_DC);
				} //if
				else if (jmxHarvesterConfig.getCloudRegion().equalsIgnoreCase("eu")) {

					Agent.LOG.debug("[" + Constantz.EXTENSION_NAME + "] Cloud region set to European Union.");
					__post = new HttpPost(Constantz.EU_DC);
				} //else if
				else {
					
					Agent.LOG.debug("[" + Constantz.EXTENSION_NAME + "] Unknown cloud region setting to US.");
					__post = new HttpPost(Constantz.US_DC);
					
				} //else
		   
		    	JSONObject __json_body = new JSONObject();
		    	JSONObject __json_response = new JSONObject();
		    	StringBuffer __sb_json = new StringBuffer();
		    	__sb_json.append("{ actor { entity(guid: \"");
		    	__sb_json.append(jmxHarvesterConfig.getEntityGuid());
		    	__sb_json.append("\") { nerdStorage { collection(collection: \"");
		    	__sb_json.append(Constantz.NERDSTORE_COLLECTION);
		    	__sb_json.append("\") { document } } } } }");
		    	Agent.LOG.info("graph query? " + __sb_json.toString());//devel
		    	__json_body.put("query", __sb_json.toString());
				__post.addHeader("Content-Type", "application/json");
				__post.addHeader("newrelic-package-id", jmxHarvesterConfig.getCloudConfigUUID());
				__post.addHeader("API-Key", jmxHarvesterConfig.getCloudConfigKey());				
	        	__post.setEntity(new StringEntity(__json_body.toString()));		    	
	        	CloseableHttpClient httpClient = HttpClients.createDefault();
	        	CloseableHttpResponse response = httpClient.execute(__post);
	        	__result = EntityUtils.toString(response.getEntity());

	        	//TODO log level
				Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Cloud config response: " + response.getStatusLine().getStatusCode() + " - " + response.getStatusLine().getReasonPhrase());
				
	        	
	        	if (response.getStatusLine().getStatusCode() == 200) {

		        	JSONParser parser = new JSONParser();
		        	__json_response = (JSONObject)parser.parse(__result);
		        	Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Cloud config reponse: " + __json_response.toJSONString());
		        	
		        	JSONArray __errors = (JSONArray)__json_response.get("errors");
		        	
		        	if (__errors == null) {
		        		

			        	JSONObject __data = (JSONObject)__json_response.get("data");
			        	JSONObject __actor = (JSONObject)__data.get("actor");
			        	JSONObject __entity = (JSONObject)__actor.get("entity");
			        	JSONObject __nerdstorage = (JSONObject)__entity.get("nerdStorage");
			        	JSONArray __collection = (JSONArray)__nerdstorage.get("collection");
			        	jmxHarvesterConfig.setCloudConfig(__collection);
			        	
		        	} //if
		        	else {
		        		
		        		Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Error getting cloud config: " + __errors.toJSONString());
		        		Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Check the jmx_harvester settings in your newrelic.yml file.");        	
		        	} //else
		        	
	        	} //if
	        	else {
	        		
					Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Cloud config request failed with response status code: " + response.getStatusLine().getStatusCode() + " - " + response.getStatusLine().getReasonPhrase());
					Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Check your jmx_harvester cloud config in the newrelic.yml");					
	        	} //else
	   
			} //try
			catch(java.lang.Exception _e) {
				
				Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] Problem checking cloud config.");
				Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "]" + _e.toString());
			} //catch
			
    		Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Cloud config check end."); //TODO
    	} //if
    	else {
    		Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Cloud config no-check."); //TODO
    		//reset the counter to the check config, otherwise it will continue to increment. 
    		cloudConfigCounter =  jmxHarvesterConfig.getCloudConfigFrequency() -1;
    	} //else
        
    	cloudConfigCounter++;
    } //getCloudConfig
    
    @SuppressWarnings("unchecked")
    private void setUpMetricsSystem() {
    	
    	Agent.LOG.info("set up metrics");
        //initialize the metrics system
        Supplier<HttpPoster> __httpPosterCreator = () -> new OkHttpPoster(Duration.of(10, ChronoUnit.SECONDS));
    	SenderConfigurationBuilder __metricConfig = MetricBatchSenderFactory.fromHttpImplementation(__httpPosterCreator).configureWith(ServiceFactory.getConfigService().getLocalAgentConfig().getLicenseKey()).useLicenseKey(true);
    	metricBatchSender = MetricBatchSender.create(__metricConfig.build());
    	
    	//populate the global metric attributes 
    	globalAttributes.put("jmx_harvester_mode", jmxHarvesterConfig.getMode());
    	globalAttributes.put("host", jmxHarvesterConfig.getHostname());
    	globalAttributes.put("entity.guid", jmxHarvesterConfig.getEntityGuid());
    	globalAttributes.put("appName", jmxHarvesterConfig.getAppName());    	
    	globalAttributes.putAll(jmxHarvesterConfig.getLabelsAsAttributes());

        
    } //setupMetricsSystem

    private void executePromiscuous() {

        MBeanServer __mbeanServer = ManagementFactory.getPlatformMBeanServer();
        Set<ObjectInstance> __mbeanInstances = __mbeanServer.queryMBeans(null, null);
        Iterator<ObjectInstance> __iterator = __mbeanInstances.iterator();

        Map<String, Object> __tempEventAttributes = null;
        Vector<Map> __tempTabularEventVector = new Vector<Map>();

        ObjectInstance __oiInstance = null;
        Hashtable<?, ?> __htOIProperties = null;
        MBeanAttributeInfo[] __mbaiAttributes = null;
        MBeanInfo __mbiTempInfo = null;

        //loop each of the available MBeans
        while (__iterator.hasNext()) {

            try {

                __oiInstance = __iterator.next();
                __tempEventAttributes = new HashMap<String, Object>();
                __tempEventAttributes.put("MBean", __oiInstance.getObjectName().toString());
                __htOIProperties = __oiInstance.getObjectName().getKeyPropertyList();

                // We need to handle the possiblity mbeans don't have name or type
                // attributes. If we record them we can kill the HashMap with a null entry.
                __tempEventAttributes.put("MBeanInstanceName",  (__htOIProperties.get("name") == null)? "unknown_name": __htOIProperties.get("name"));
                __tempEventAttributes.put("MBeanInstanceType",  (__htOIProperties.get("type") == null)? "unknown_type": __htOIProperties.get("type"));


                __mbiTempInfo = __mbeanServer.getMBeanInfo(__oiInstance.getObjectName());
                __mbaiAttributes = __mbiTempInfo.getAttributes();

                for (int i = 0; i < __mbaiAttributes.length; i++) {

                    if (__mbaiAttributes[i].isReadable() && __mbaiAttributes[i].getName() != null) {

                    	handleAttributeValueAsEvent(__mbeanServer.getAttribute(__oiInstance.getObjectName(), __mbaiAttributes[i].getName()), new MBeanAttributeConfig(__mbaiAttributes[i].getName()), __tempEventAttributes, __tempTabularEventVector);

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


            publishInsightsEvent(__tempEventAttributes);

            //add any tabular events - this is a poop work around for tabular support and breaks the whole simplicty of this derp derp ...
            if (__tempTabularEventVector.size() > 0) {

                Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Adding TabularData entries for this mbean execution.");

                for (int __itabs = 0; __itabs < __tempTabularEventVector.size(); __itabs++) {
                    publishInsightsEvent(__tempTabularEventVector.get(__itabs));
                } //for

            } //if

        } //while


        Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] WARNING ::: JMXHarvester is set to promiscuous mode. This will harvest data from all available MBeans (which can be a lot of data). "
                + "Please take caution when enabling this mode for your application. The next promiscuous havest will take place in "
                + jmxHarvesterConfig.getFrequency() + " minute(s).");
    } //executePromiscuous

    private void executeStrict() {

        MBeanServer __mbeanServer = ManagementFactory.getPlatformMBeanServer();
        MBeanConfig[] __mbeans = jmxHarvesterConfig.getMBeans();
        ObjectName __tempMBean = null;

        String __stTelemetryModel = jmxHarvesterConfig.getTelemetryModel(); //this will be the telemetry model for this run regardless of config change
        
        // events model elements
        Map<String, Object> __tempEventAttributes = null;
        Vector<Map> __tempTabularEventVector = new Vector<Map>();

        //metric model elements 
        MetricBuffer __metricBuffer = new MetricBuffer(globalAttributes);
        Attributes __tempMetricAttributes = null;
        Attributes __tempMetricAtrributeAttributes = null;
        
        MBeanAttributeConfig[] __macAttributes = null;
        Iterator<ObjectInstance> __oiIterator = null;
        Set<ObjectInstance> __oiSet = null;
        ObjectInstance __oiInstance = null;
        Hashtable<?, ?> __htOIProperties = null;

        for (int i = 0; i < __mbeans.length; i++) {

            try {

                if (__mbeans[i].getPollingInterval() != __mbeans[i].getMBeanPollingCounter()) {

                    Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] This MBean's polling interval is not satisfied it will be increased by one from: " + __mbeans[i].getMBeanName() + " : " + __mbeans[i].getMBeanPollingCounter());
                    __mbeans[i].incrementMBeanPollingCounter();
                    continue;
                } //if

                Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] This MBean's polling interval has been satisfied: " + __mbeans[i].getMBeanName() + " : " + __mbeans[i].getPollingInterval());

                /* determine if the mbean definition is using a leading wildcard - this has to be escaped by a special
                 * character because there is an issue with yml properties in the format "*:" - which could represent a valid
                 * MBean query string value.
                 */
                //remove Agent.LOG.info("b4 if ... ");
                if (__mbeans[i].getMBeanName().charAt(0) == '\\') {
                    //remove Agent.LOG.info("in if ... ");
                    __mbeans[i].setMBeanName(__mbeans[i].getMBeanName().substring(1));
                } //if

                //remove Agent.LOG.info("after if ... ");
                
                
                __tempMBean = new ObjectName(__mbeans[i].getMBeanName());
                __oiSet = __mbeanServer.queryMBeans(__tempMBean, null);
                
                if (__oiSet == null) {
                	
                    Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Unable to find the bean defined by configuration: " + __mbeans[i].getMBeanName());
                    Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Bean representation as MBean Domain: " + __tempMBean.getDomain());
                    Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Bean representation as MBean Property List: " + __tempMBean.getKeyPropertyListString());
                    Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Bean representation as MBean Object String: " + __tempMBean.toString());
                } //if
                else {
                
                    __oiIterator = __oiSet.iterator();

                    while (__oiIterator.hasNext()) {
                    	//remove Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Iterating the OI ????");
                        __oiInstance = __oiIterator.next();
                        __htOIProperties = __oiInstance.getObjectName().getKeyPropertyList();
                        
                        if (__stTelemetryModel.equals("events")) {
                        	Agent.LOG.finer("[" + Constantz.EXTENSION_NAME + "] EVENTS have been selected as the telemetry model.");
                            __tempEventAttributes = new HashMap<String, Object>();
                            __tempEventAttributes.put("MBean", __oiInstance.getObjectName().getCanonicalName());
                            
                            // We need to handle the possibility mbeans don't have name or type
                            // attributes. If we record them we can kill the HashMap with a null entry.
                            __tempEventAttributes.put("MBeanInstanceName", (__htOIProperties.get("name") == null) ? "unknown_name" : __htOIProperties.get("name"));
                            __tempEventAttributes.put("MBeanInstanceType", (__htOIProperties.get("type") == null) ? "unknown_type" : __htOIProperties.get("type"));
                            
                            
                        } //if
                        else if (__stTelemetryModel.equals("metrics")) {
                        	Agent.LOG.finer("[" + Constantz.EXTENSION_NAME + "] METRICS have been selected as the telemetry model");
                        	__tempMetricAttributes = new Attributes();
                        	__tempMetricAttributes.put("jmx_harvester_mode", "strict");
                        	__tempMetricAttributes.put("jmx_harvester_type", "measurment");
                        	__tempMetricAttributes.put("mbean_name", __oiInstance.getObjectName().getCanonicalName());
                        	
                        } //else if
                        else {
                        	
                        	Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Problem processing MBeans, unknown telemetry model! Check your jmx_harvester config.");
                        } //else
                        
                        
                        //might want to add an MBean Instance
                        //removed to support complex type sub members __stAttributes = __mbeans[i].getMBeanAttributes();
                        __macAttributes = __mbeans[i].getMbeanAttributeObjects();

                        for (int ii = 0; ii < __macAttributes.length; ii++) {
                        	
                        	if (__stTelemetryModel.equals("events")) {

                        		handleAttributeValueAsEvent(__mbeanServer.getAttribute(__oiInstance.getObjectName(), __macAttributes[ii].getAttributeName()), __macAttributes[ii], __tempEventAttributes, __tempTabularEventVector);
                        	} //if
                        	else if (__stTelemetryModel.equals("metrics")) {
                        		
                        		__tempMetricAtrributeAttributes = new Attributes();
                        		__tempMetricAtrributeAttributes.put("mbean_attribute_name", __macAttributes[ii].getAttributeName());
                        		__tempMetricAtrributeAttributes.putAll(__tempMetricAttributes);
                        		handleAttributeValueAsMetric(__metricBuffer, __mbeanServer.getAttribute(__oiInstance.getObjectName(), __macAttributes[ii].getAttributeName()), __oiInstance.getObjectName().getCanonicalName(), __macAttributes[ii], __tempMetricAtrributeAttributes);
                        	} //else if
                        	else {
                        		
                        		Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Problem processing MBeans, unknown telemetry model! Check your jmx_harvester config.");
                        	} //else

                        } //for


                        if (__stTelemetryModel.equals("events")) {
                        	
                            publishInsightsEvent(__tempEventAttributes);

                            //TODO Rework handling for tabular events
                            if (__tempTabularEventVector.size() > 0) {

                                Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Adding TabularData entries for this mbean execution.");

                                for (int __itabs = 0; __itabs < __tempTabularEventVector.size(); __itabs++) {

                                    publishInsightsEvent(__tempTabularEventVector.get(__itabs));

                                } //for

                            } //if
                            
                        } //if
                        else if (__stTelemetryModel.equals("metrics")) {
                        	
                        	if (__metricBuffer.size() > jmxHarvesterConfig.getMetricBatchSize()) {

                        		//send the metrics buffer contents ...
        				        try {
        				        	
        				        	Response __response = metricBatchSender.sendBatch(__metricBuffer.createBatch());
        				        	Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] sending from strict ... " + __response.getStatusMessage());
        				        	__metricBuffer = new MetricBuffer(globalAttributes); //reset the metric buffer
        				        	
        				        } //try
        				        catch(java.lang.Exception _e) {
        				        	
        				        	Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Problem sending this batch of metrics from strict  - " + _e.toString());
        				        } //catch
                        	} //if
                        	
                        } //else if
                        else {
                        	
                        	Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Problem processing MBeans, unknown telemetry model! Check your jmx_harvester config. Nothing will be recorded.");
                        } //else

                    } //while
                    
                } //else 

                __mbeans[i].resetMBeanPollingCounter();

            } //try
            catch (java.lang.Exception _e) {

                Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Problem loading the mbean: " + __mbeans[i].getMBeanName());
                Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Message MBean Fail: " + _e.getMessage());
                //adding additional Logging for the NULL MBEAN ISSUE:
                Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Message MBean Fail Cause: " + _e.getCause());
            } //catch
        } //for
        
        // send the remaining values in the buffer
        if (__metricBuffer.size() > 0) {
        	
        	//send the metrics buffer contents ...
	        try {
	        	
	        	Response __response = metricBatchSender.sendBatch(__metricBuffer.createBatch());
	        	Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] FINAL sending from strict ... " + __response.getStatusMessage());
	        	__metricBuffer = null; //reset the metric buffer
	        	
	        } //try
	        catch(java.lang.Exception _e) {
	        	
	        	Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Problem sending this FINAL metrics batch of metrics from strict  - " + _e.toString());
	        } //catch
        } //if

    } //executeStrict

    private void executeOpen() {

        MBeanServer __mbeanServer = ManagementFactory.getPlatformMBeanServer();
        MBeanConfig[] __mbeans = jmxHarvesterConfig.getMBeans();
        ObjectName __tempMBean = null;
        
        String __stTelemetryModel = jmxHarvesterConfig.getTelemetryModel(); //this will be the telemetry model for this run regardless of config change
        
        // events model objects 
        Map<String, Object> __tempEventAttributes = null;
        Vector<Map> __tempTabularEventVector = new Vector<Map>();
        
        // metrics model objects
        MetricBuffer __metricBuffer = new MetricBuffer(globalAttributes);
        Attributes __tempMetricAttributes = null;
        Attributes __tempMetricAtrributeAttributes = null;
        
        Iterator<ObjectInstance> __oiIterator = null;
        Set<ObjectInstance> __oiSet = null;
        ObjectInstance __oiInstance = null;
        Hashtable<?, ?> __htOIProperties = null;
        MBeanAttributeInfo[] __mbaiAttributes = null;
        MBeanInfo __mbiTempInfo = null;

        for (int i = 0; i < __mbeans.length; i++) {

            try {

                //determine if the candidate mbean satisfied the desired polling interval
                if (__mbeans[i].getPollingInterval() == __mbeans[i].getMBeanPollingCounter()) {

                    Agent.LOG.finer("[" + Constantz.EXTENSION_NAME + "] This MBean's polling interval has been satisfied: " + __mbeans[i].getMBeanName() + " : " + __mbeans[i].getPollingInterval());

                    /* determine if the mbean definition is using a leading wildcard - this has to be escaped by a special
                     * character because there is an issue with yml properties in the format "*:" - which could represent a valid
                     * MBean query string value.
                     */
                    if (__mbeans[i].getMBeanName().charAt(0) == '\\') {

                        __mbeans[i].setMBeanName(__mbeans[i].getMBeanName().substring(1));
                    } //if


                    __tempMBean = new ObjectName(__mbeans[i].getMBeanName());
                    __oiSet = __mbeanServer.queryMBeans(__tempMBean, null);
                    
                    if (__oiSet == null) {
                    	
                        Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Unable to find the bean defined by configuration: " + __mbeans[i].getMBeanName());
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
                                __tempEventAttributes = new HashMap<String, Object>();
                                __tempEventAttributes.put("MBean", __oiInstance.getObjectName().getCanonicalName());

                                // We need to handle the possibility mbeans don't have name or type
                                // attributes. If we record them we can kill the HashMap with a null entry.
                                __tempEventAttributes.put("MBeanInstanceName", (__htOIProperties.get("name") == null) ? "unknown_name" : __htOIProperties.get("name"));
                                __tempEventAttributes.put("MBeanInstanceType", (__htOIProperties.get("type") == null) ? "unknown_type" : __htOIProperties.get("type"));
                                
                            } //if
                            else if (__stTelemetryModel.equals("metrics")) {
                            	
                            	Agent.LOG.finer("[" + Constantz.EXTENSION_NAME + "] METRICS have been selected as the telemetry model");
                            	__tempMetricAttributes = new Attributes();
                            	__tempMetricAttributes.put("jmx_harvester_mode", "opem");
                            	__tempMetricAttributes.put("jmx_harvester_type", "measurment");
                            	__tempMetricAttributes.put("mbean_name", __oiInstance.getObjectName().getCanonicalName());
                            } //else if
                            else {
                            	
                            	Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Problem processing MBeans, unknown telemetry model! Check your jmx_harvester config.");
                            } //else
                            
                            

                            try {

                                __mbiTempInfo = __mbeanServer.getMBeanInfo(__oiInstance.getObjectName());
                                __mbaiAttributes = __mbiTempInfo.getAttributes();

                                //report all the attributes for this mbean
                                for (int ii = 0; ii < __mbaiAttributes.length; ii++) {

                                    if (__mbaiAttributes[ii].isReadable()) {

                                    	if (__stTelemetryModel.equals("events")) {
                                        	handleAttributeValueAsEvent(__mbeanServer.getAttribute(__oiInstance.getObjectName(), __mbaiAttributes[ii].getName()), new MBeanAttributeConfig(__mbaiAttributes[ii].getName()), __tempEventAttributes, __tempTabularEventVector);                                    		
                                    	} //if
                                    	else if (__stTelemetryModel.equals("metrics")) {
                                    		
                                    		__tempMetricAtrributeAttributes = new Attributes();
                                    		__tempMetricAtrributeAttributes.put("mbean_attribute_name", __mbaiAttributes[ii].getName());
                                    		__tempMetricAtrributeAttributes.putAll(__tempMetricAttributes);
                                    		handleAttributeValueAsMetric(__metricBuffer, __mbeanServer.getAttribute(__oiInstance.getObjectName(), __mbaiAttributes[ii].getName()), __oiInstance.getObjectName().getCanonicalName(), new MBeanAttributeConfig(__mbaiAttributes[ii].getName()), __tempMetricAtrributeAttributes);
                                    	} //else if
                                    	else {
                                    		Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Problem processing MBeans, unknown telemetry model! Check your jmx2insights config.");
                                    	}


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
                            	
                                publishInsightsEvent(__tempEventAttributes);

                                //add any tabular events - this is a poop work around for tabular support and breaks the whole simplicty of this derp derp ...
                                if (__tempTabularEventVector.size() > 0) {

                                    Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Adding TabularData entries for this mbean execution.");

                                    for (int __itabs = 0; __itabs < __tempTabularEventVector.size(); __itabs++) {

                                        publishInsightsEvent(__tempTabularEventVector.get(__itabs));
                                    } //for

                                } //if
                                
                            } //if
                            else if (__stTelemetryModel.equals("metrics")) {
                            	
                            	if (__metricBuffer.size() > jmxHarvesterConfig.getMetricBatchSize()) {

                            		//send the metrics buffer contents ...
            				        try {
            				        	
            				        	Response __response = metricBatchSender.sendBatch(__metricBuffer.createBatch());
            				        	Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] sending from open ... " + __response.getStatusMessage());
            				        	__metricBuffer = new MetricBuffer(globalAttributes); //reset the metric buffer
            				        	
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

                    Agent.LOG.finer("[" + Constantz.EXTENSION_NAME + "] This MBean's polling interval is not satisfied it will be increased by one from: " + __mbeans[i].getMBeanName() + " : " + __mbeans[i].getMBeanPollingCounter());
                    __mbeans[i].incrementMBeanPollingCounter();
                } //else

            } //try
            catch (java.lang.Exception _e) {

                Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Problem loading the mbean: " + __mbeans[i].getMBeanName());
                Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Message MBean Fail: " + _e.getMessage());
            } //catch
        } //for

        if (__metricBuffer.size() > 0) {
        	
        	//send the metrics buffer contents ...
	        try {
	        	
	        	Response __response = metricBatchSender.sendBatch(__metricBuffer.createBatch());
	        	Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] FINAL sending from open ... " + __response.getStatusMessage());
	        	__metricBuffer = null; //reset the metric buffer
	        	
	        } //try
	        catch(java.lang.Exception _e) {
	        	
	        	Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Problem sending this FINAL metrics batch of metrics from open  - " + _e.toString());
	        } //catch
        } //if
        
    } //executeOpen

    /**
     * Polls all MBean attributes and operations and persists them to New Relic as metrics. Metrics are used because
     * there can be many values we're trying to persist and persisting them as events can easily overwhelm the event 
     * system within the Java agent. 
     */
    private void executeInventory() {

    	//metrics 
        MetricBuffer __metricBuffer = null;
        MBeanServer __mbeanServer = ManagementFactory.getPlatformMBeanServer();
        Set<ObjectInstance> __mbeanInstances = __mbeanServer.queryMBeans(null, null);
        Iterator<ObjectInstance> __iterator = __mbeanInstances.iterator();
        
        Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Running Inventory .... ");
                    
        try {

        	__metricBuffer = new MetricBuffer(globalAttributes);
        	
            while (__iterator.hasNext()) {
            	                
                ObjectInstance instance = __iterator.next();
                ObjectName objectName = instance.getObjectName();
                MBeanInfo __info = __mbeanServer.getMBeanInfo(objectName);
      
                // going to reuse the metric buffer and reallocate when it is cleared __metricBuffer = new MetricBuffer(globalAttributes);
                Attributes __standardAttributes = new Attributes();
                __standardAttributes.put("jmx_harvester_mode", "inventory");
                __standardAttributes.put("jmx_harvester_type", "inventory"); //keeping both mode and type to support more exotic types in the future - the harvester ui depends on _type attribute
                __standardAttributes.put("object_type", "mbean");
                __standardAttributes.put("mbean_name", objectName.toString());
                __standardAttributes.put("mbean_domain", objectName.getDomain());                
            	
                MBeanAttributeInfo[] __mbai = __info.getAttributes();
                
                for (int i = 0; i < __mbai.length; i++) {

                	 Attributes __attributeAttributes = new Attributes();
                	 __attributeAttributes.put("mbean_element", "attribute");
                     __attributeAttributes.put("mbean_attribute_type", __mbai[i].getType());
                     __attributeAttributes.put("mbean_attribute_description", __mbai[i].getDescription());
                     __attributeAttributes.put("mbean_attribute_readable", __mbai[i].isReadable());
                     __attributeAttributes.put("mbean_attribute_name", __mbai[i].getName());
                     __attributeAttributes.put("jmx_harvester_frequency", jmxHarvesterConfig.getFrequency());
                     __attributeAttributes.putAll(__standardAttributes);
                     Gauge __gauge = new Gauge("jmx_harvester.inventory." + instance.getObjectName() + "." + __mbai[i].getName(), 0d, System.currentTimeMillis(), __attributeAttributes);
                    __metricBuffer.addMetric(__gauge);
                } //for


                MBeanOperationInfo[] __mboi = __info.getOperations();

                for (int i = 0; i < __mboi.length; i++) {

                	Attributes __operationAttributes = new Attributes();
                	__operationAttributes.put("mbean_element", "operation");
                	__operationAttributes.put("mbean_operation_return_type", __mboi[i].getReturnType());
                	__operationAttributes.put("mbean_operation_description", __mboi[i].getDescription());
                	__operationAttributes.put("mbean_operation_name", __mboi[i].getName());
                	__operationAttributes.put("jmx_harvester_frequency", jmxHarvesterConfig.getFrequency());
                	__operationAttributes.putAll(__standardAttributes);
                   Gauge __operation_gauge = new Gauge("jmx_harvester.inventory." + instance.getObjectName() + "." + __mboi[i].getName(), 0d, System.currentTimeMillis(), __operationAttributes);
               		__metricBuffer.addMetric(__operation_gauge);
                } //for

                if (__metricBuffer.size() > jmxHarvesterConfig.getMetricBatchSize()) {
                    try {
                    	
                    	Response __response = metricBatchSender.sendBatch(__metricBuffer.createBatch());
                    	Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] metricBatchSend Status Message >>>> " + __response.getStatusMessage());
                    	__metricBuffer = new MetricBuffer(globalAttributes);
                    } //try
                    catch(java.lang.Exception _e) {
                    	
                    	Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] metricBatchSendr blew right up. " + _e.toString());
                    } //catch                	
                } //if
                
            } //while
            
            // send remaining batch of metrics ... 
            if (__metricBuffer.size() > 0) {
            	
	    	   try {
	           	
	    		   Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Sending " + __metricBuffer.size() + " metrics to New Relic.");
		           Response __response = metricBatchSender.sendBatch(__metricBuffer.createBatch());
		           Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] FINAL metricBatchSend Status Message >>>> " + __response.getStatusMessage());
		           	__metricBuffer = null;
	           } //try
	           catch(java.lang.Exception _e) {
	           	
	        	   Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] FINAL metricBatchSendr blew right up. " + _e.toString());
	           } //catch
            } //if

        } //try
        catch (java.lang.Exception _e) {

            Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Problem discovering MBeans and Attributes.");
            Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Message: " + _e.getMessage());
        } //catch

    } //executeInventory


    private void handleAttributeValueAsMetric(MetricBuffer _metricBuffer, Object _oAttributeValue, String _stMBeanName, MBeanAttributeConfig _macAttributeConfig, Attributes _attributes) {
    	
    	Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] 1. _metricBuffer >>>>>>>>> " + _metricBuffer);
    	Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] 2. _oAttributeValue >>>>>>>>> " + _oAttributeValue);
    	Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] 3. _stMBeanName >>>>>>>>> " + _stMBeanName);
    	Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] 4. _macAtrributeConfig >>>>>>>>> " + _macAttributeConfig);
    	Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] 5. _attributes >>>>>>>>> " + _attributes);
    	
    	try {
    		
    		if (_oAttributeValue instanceof java.lang.Number) {

    			_metricBuffer.addMetric(new Gauge(jmxHarvesterConfig.getMetricPrefix() + "." + _stMBeanName + "." + _macAttributeConfig.getAttributeName(), ((Number)_oAttributeValue).doubleValue(), System.currentTimeMillis(), _attributes));
              
            } //if
            else if (_oAttributeValue instanceof java.lang.String || _oAttributeValue instanceof java.lang.Boolean) {

            	_attributes.put("raw_value", _oAttributeValue.toString());
    			_metricBuffer.addMetric(new Gauge(jmxHarvesterConfig.getMetricPrefix() + "." + _stMBeanName + "." + _macAttributeConfig.getAttributeName(), 0d, System.currentTimeMillis(), _attributes));
    			
            } //else if
            else if (_oAttributeValue instanceof java.util.Date) {

                java.util.Date __dateAttribute = (java.util.Date) _oAttributeValue;
                java.util.Calendar __calendarHelper = (java.util.Calendar.getInstance());
                __calendarHelper.setTime(__dateAttribute);
                _attributes.put("raw_value", new Long(__calendarHelper.getTimeInMillis()));
    			_metricBuffer.addMetric(new Gauge(jmxHarvesterConfig.getMetricPrefix() + "." + _stMBeanName + "." + _macAttributeConfig.getAttributeName(), 0d, System.currentTimeMillis(), _attributes));
            } //else if 
            else if (_oAttributeValue instanceof javax.management.openmbean.CompositeData) {

                //for this iteration we will just grab all the elements from the composite object
                Agent.LOG.finer("[" + Constantz.EXTENSION_NAME + "] Processing CompositeData Attribute: " + _macAttributeConfig.getAttributeName());

                /* determine if we are looking up discreet header values or getting all of them */
                if (_macAttributeConfig.hasAttributeElements()) {

                    Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Recording a subset of the Composite Atrribute Header Elements.");

                    String[] __stAttributeHeaders = _macAttributeConfig.getAttributeElementHeaders();
                    
                    for (int i = 0; i < __stAttributeHeaders.length; i++) {

            			_metricBuffer.addMetric(
        					new Gauge(
        							jmxHarvesterConfig.getMetricPrefix() + "." + _stMBeanName + "." + _macAttributeConfig.getAttributeName() + "." + __stAttributeHeaders[i], 
        							handleCompositeDataObjectForMetrics(((javax.management.openmbean.CompositeData)_oAttributeValue).get(__stAttributeHeaders[i]), __stAttributeHeaders[i]), 
            					    System.currentTimeMillis(), 
            					   _attributes));
            			
                    } //for
                } //if
                else {

                    Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Recording all Composite Atrribute Header Elements.");

                    for (String __key : ((javax.management.openmbean.CompositeData) _oAttributeValue).getCompositeType().keySet()) {
                    	
                        _metricBuffer.addMetric(
            					new Gauge(
            							jmxHarvesterConfig.getMetricPrefix() + "." + _stMBeanName + "." + _macAttributeConfig.getAttributeName() + "." + __key, 
            							handleCompositeDataObjectForMetrics(((javax.management.openmbean.CompositeData)_oAttributeValue).get(__key), __key), 
                					    System.currentTimeMillis(), 
                					   _attributes));
                    } //for
                } //else
            } //else if --> CompositeData MBean
    		//////
            /* adding javax.management.openmbean.Y=TabularData support */
            else if (_oAttributeValue instanceof javax.management.openmbean.TabularData) {

                Agent.LOG.finer("[" + Constantz.EXTENSION_NAME + "] Processing TabularData Attribute as gauge metric: " + _macAttributeConfig.getAttributeName());
                Iterator<?> __tabularDataIterator = ((javax.management.openmbean.TabularData) _oAttributeValue).values().iterator();
                Object __tempObj;
                CompositeData __tempCD;

                try {

                    while (__tabularDataIterator.hasNext()) {

                        __tempObj = __tabularDataIterator.next();

                        if (__tempObj instanceof CompositeData) {

                            __tempCD = (CompositeData) __tempObj;

                            /* determine if we are looking up discreet header values or getting all of them */
                            if (_macAttributeConfig.hasAttributeElements()) {

                                Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Recording a subset of the Composite Atrribute Header Elements for Tabular MBean type.");

                                String[] __stAttributeHeaders = _macAttributeConfig.getAttributeElementHeaders();
                                for (int i = 0; i < __stAttributeHeaders.length; i++) {

                                    //__mTabularEventHolder.put(__stAttributeHeaders[i], handleCompositeDataObject(__tempCD.get(__stAttributeHeaders[i])));
                                    _metricBuffer.addMetric(
                        					new Gauge(
                        							jmxHarvesterConfig.getMetricPrefix() + "." + _stMBeanName + "." + _macAttributeConfig.getAttributeName() + "." + __stAttributeHeaders[i], 
                        							handleCompositeDataObjectForMetrics(((javax.management.openmbean.CompositeData)_oAttributeValue).get(__stAttributeHeaders[i]), __stAttributeHeaders[i]), 
                            					    System.currentTimeMillis(), 
                            					   _attributes));
                                    
                                } //for
                            } //if
                            else {

                                Agent.LOG.finer("[" + Constantz.EXTENSION_NAME + "] Recording all Composite Atrribute Header Elements for Tabular Mbean type.");

                                for (String __key : (__tempCD.getCompositeType().keySet())) {

                                    //__mTabularEventHolder.put(__key, handleCompositeDataObject(__tempCD.get(__key)));
                                    _metricBuffer.addMetric(
                        					new Gauge(
                        							jmxHarvesterConfig.getMetricPrefix() + "." + _stMBeanName + "." + _macAttributeConfig.getAttributeName() + "." + __key, 
                        							handleCompositeDataObjectForMetrics(((javax.management.openmbean.CompositeData)_oAttributeValue).get(__key), __key), 
                            					    System.currentTimeMillis(), 
                            					   _attributes));                        
                                } //for
                            } //else

                        } //if
                        else {

                            Agent.LOG.finer("[" + Constantz.EXTENSION_NAME + "] Non-CompositeData encountered in TabularData object: " + __tempObj.getClass().getName());
                        } //else

                    } //while

                } //try
                catch (java.lang.Exception _e) {

                    Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Issue interrogating TabularData CompositeData object. Turn on fine logging for more details.");
                    Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Problem interrogating mbean attribute: " + _macAttributeConfig.getAttributeName() + " during harvest.");
                    Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Message MBean Attribute Access Fail: " + _e.getMessage());
                } //catch

            } //else if --> TabularData Mbean
    		else {
    			
    			Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] IGNORING ANYTHING THAT ISN'T A NUMBER OR SIMPLE NUMERIC ");
    		} //else
    		
    	} //try
    	catch (java.lang.Exception _e) {
    		
    		Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] A problem occurred processing the mbean attribute as a metric: " + _e.getCause());
    	} //catch
    	
    } //handleAttributeValueAsMetric
     
    @SuppressWarnings("unchecked")
    private void handleAttributeValueAsEvent(Object _oAttributeValue, MBeanAttributeConfig _macAttributeConfig, Map<String, Object> _mEventHolder, Vector<Map> _vTabularEvents) {

        try {

            if (_oAttributeValue instanceof java.lang.Number) {

                _mEventHolder.put(_macAttributeConfig.getAttributeName(), _oAttributeValue);

            } //if
            else if (_oAttributeValue instanceof java.lang.String) {

                _mEventHolder.put(_macAttributeConfig.getAttributeName(), _oAttributeValue);

            } //else if
            else if (_oAttributeValue instanceof java.lang.Boolean) {

                _mEventHolder.put(_macAttributeConfig.getAttributeName(), _oAttributeValue);

            } //else if
            else if (_oAttributeValue instanceof java.util.Date) {

                java.util.Date __dateAttribute = (java.util.Date) _oAttributeValue;
                java.util.Calendar __calendarHelper = (java.util.Calendar.getInstance());
                __calendarHelper.setTime(__dateAttribute);
                _mEventHolder.put(_macAttributeConfig.getAttributeName(), new Long(__calendarHelper.getTimeInMillis()));

            } //else if
            else if (_oAttributeValue instanceof javax.management.openmbean.CompositeData) {

                //for this iteration we will just grab all the elements from the composite object
                Agent.LOG.finer("[" + Constantz.EXTENSION_NAME + "] Processing CompositeData Attribute: " + _macAttributeConfig.getAttributeName());

                /* determine if we are looking up discreet header values or getting all of them */
                if (_macAttributeConfig.hasAttributeElements()) {

                    Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Recording a subset of the Composite Atrribute Header Elements.");

                    String[] __stAttributeHeaders = _macAttributeConfig.getAttributeElementHeaders();
                    for (int i = 0; i < __stAttributeHeaders.length; i++) {

                        _mEventHolder.put(_macAttributeConfig.getAttributeName() + "_" + __stAttributeHeaders[i], handleCompositeDataObjectForEvents(((javax.management.openmbean.CompositeData) _oAttributeValue).get(__stAttributeHeaders[i])));
                    } //for
                } //if
                else {

                    Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Recording all Composite Atrribute Header Elements.");

                    for (String __key : ((javax.management.openmbean.CompositeData) _oAttributeValue).getCompositeType().keySet()) {

                        //Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Beta: Processing CompositeData Attribute: " + _stAttributeName + ", with key: " + __key);
                        //_mEventHolder.put(_stAttributeName + "_" + __key, ((javax.management.openmbean.CompositeData)_oAttributeValue).get(__key));
                        _mEventHolder.put(_macAttributeConfig.getAttributeName() + "_" + __key, handleCompositeDataObjectForEvents(((javax.management.openmbean.CompositeData) _oAttributeValue).get(__key)));
                    } //for
                } //else
            } //else if
            else if (_oAttributeValue instanceof java.lang.String[]) {

                //Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Beta: Processing String Array Attribute: " + _stAttributeName);

                for (int i = 0; i < ((java.lang.String[]) _oAttributeValue).length; i++) {

                    _mEventHolder.put(_macAttributeConfig.getAttributeName() + "_" + i, ((java.lang.String[]) _oAttributeValue)[i]);
                } //for

            } //else if
            /* adding javax.management.openmbean.Y=TabularData support */
            else if (_oAttributeValue instanceof javax.management.openmbean.TabularData) {

                Agent.LOG.finer("[" + Constantz.EXTENSION_NAME + "] Processing TabularData Attribute: " + _macAttributeConfig.getAttributeName());
                Map<String, Object> __mTabularEventHolder = null;
                Iterator<?> __iTabularEventValueIterator;


                Iterator<?> __compositeDataIterator = ((javax.management.openmbean.TabularData) _oAttributeValue).values().iterator();
                Object __tempObj;
                CompositeData __tempCD;
                int __iTabIndex = 0;

                try {

                    while (__compositeDataIterator.hasNext()) {

                        __mTabularEventHolder = new HashMap<String, Object>();
                        //copy the needed elements into this new hashmap ...
                        __mTabularEventHolder.put("MBeanInstanceType", _mEventHolder.get("MBeanInstanceType"));
                        __mTabularEventHolder.put("MBeanInstanceName", _mEventHolder.get("MBeanInstanceName"));
                        __mTabularEventHolder.put("MBean", _mEventHolder.get("MBean"));

                        __mTabularEventHolder.put("MBeanAttributeType", "TabularDataRow"); //TODO this needs to be derived from the record itself
                        __mTabularEventHolder.put("MBeanAttributeName", _macAttributeConfig.getAttributeName());

                        __tempObj = __compositeDataIterator.next();

                        if (__tempObj instanceof CompositeData) {

                            __tempCD = (CompositeData) __tempObj;

                            /* determine if we are looking up discreet header values or getting all of them */
                            if (_macAttributeConfig.hasAttributeElements()) {

                                Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Recording a subset of the Composite Atrribute Header Elements.");

                                String[] __stAttributeHeaders = _macAttributeConfig.getAttributeElementHeaders();
                                for (int i = 0; i < __stAttributeHeaders.length; i++) {

                                    __mTabularEventHolder.put(__stAttributeHeaders[i], handleCompositeDataObjectForEvents(__tempCD.get(__stAttributeHeaders[i])));
                                } //for
                            } //if
                            else {

                                Agent.LOG.finer("[" + Constantz.EXTENSION_NAME + "] Recording all Composite Atrribute Header Elements.");

                                for (String __key : (__tempCD.getCompositeType().keySet())) {

                                    __mTabularEventHolder.put(__key, handleCompositeDataObjectForEvents(__tempCD.get(__key)));
                                } //for
                            } //else

                        } //if
                        else {

                            Agent.LOG.finer("[" + Constantz.EXTENSION_NAME + "] Non-CompositeData encountered in TabularData object: " + __tempObj.getClass().getName());
                        } //else

                        __iTabIndex++; //TODO need to validate this is needed or best approach
                        if (__mTabularEventHolder.size() > 0) {

                            Agent.LOG.debug("[" + Constantz.EXTENSION_NAME + "] Recording this tabular record as an event.");
                            _vTabularEvents.addElement(__mTabularEventHolder);
                        }//if
                        else {

                            Agent.LOG.debug("[" + Constantz.EXTENSION_NAME + "] No tabular events recorded in this iteration.");
                        } //else
                    } //while

                } //try
                catch (java.lang.Exception _e) {

                    Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Issue interrogating TabularData CompositeData object. Turn on fine logging for more details.");
                    Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Problem interrogating mbean attribute: " + _macAttributeConfig.getAttributeName() + " during harvest.");
                    Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Message MBean Attribute Access Fail: " + _e.getMessage());
                } //catch


            } //else if
            else {

                Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Attribute Check :: Unsupported attribute type: " + _oAttributeValue.getClass() + " for: " + _macAttributeConfig.getAttributeName());
            } //else
        } //try
        catch (java.lang.Exception _e) {

            Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Problem interrogating mbean attribute: " + _macAttributeConfig.getAttributeName() + " during harvest.");
            Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Message MBean Attribute Access Fail: " + _e.getMessage());
        } //catch

    } //handleAttributeValueAsEvent
    
    private double handleCompositeDataObjectForMetrics(java.lang.Object _object, String _key) {
		
		try {

			  if (_object instanceof java.lang.String || _object instanceof java.lang.Boolean) { 
				  
				  Agent.LOG.fine("[" + Constantz.EXTENSION_LOG_STRING + "] Problem interrogating mbean attribute for Composite bean of type: " + _object.getClass() + ". Object value: " + _object.toString() );
				  return(-273.2d); //invalid type returns water triple point temp in k 	as negative		  
			  } //if
			  else if (_object instanceof java.lang.Number) { 
				  
				  return(((Number)_object).doubleValue());			  
			  } //if
			  else if (_object instanceof java.util.Date) { 	  
		
				  java.util.Date __dateAttribute = (java.util.Date)_object;
				  java.util.Calendar __calendarHelper = (java.util.Calendar.getInstance());
				  __calendarHelper.setTime(__dateAttribute);
				  return((double)(__calendarHelper.getTimeInMillis()));
				  
			  } //else if
			  //take last value of an array
			  else if (_object instanceof long[]) {
				  
				  long[] __longArrayAttribute = (long[])_object;
				  //going to report the final value in the array of values
				  return((double)(__longArrayAttribute[__longArrayAttribute.length - 1]));
			  } //else if
			  else if (_object instanceof int[]) {
				  
				  int[] __intArrayAttribute = (int[])_object;
				  return((double)(__intArrayAttribute[__intArrayAttribute.length - 1]));
			  } //else if
			  else {
				  
				  Agent.LOG.fine("[" + Constantz.EXTENSION_NAME_MEMORY + "] Problem interrogating mbean attribute for Composite bean of type: " + _object.getClass() + ".");
				  return(-234.3156d); //invalid type returns mercury triple point temp in k as negative
			  } //else
		}
		catch(java.lang.Exception _e) {
			
			Agent.LOG.fine("[" + Constantz.EXTENSION_NAME_MEMORY + "] Problem interrogating mbean attribute for Composite bean of type: " + _object.getClass() + ". Object value: " + _object.toString() );
			return(-83.8058d); //invalid type returns argon triple point temp in k as negative
		} //catch

	} //handleCompositeDataObjecthandleCompositeDataObjectForMetrics

    /*
     * For the time being using this as a second stage to interrogate the objects embedded within a CompositeData object.
     */
    private Object handleCompositeDataObjectForEvents(java.lang.Object _object) {

        if (_object instanceof java.lang.Number || _object instanceof java.lang.String || _object instanceof java.lang.Boolean) {

            return (_object);
        } //if
        else if (_object instanceof java.util.Date) {

            java.util.Date __dateAttribute = (java.util.Date) _object;
            java.util.Calendar __calendarHelper = (java.util.Calendar.getInstance());
            __calendarHelper.setTime(__dateAttribute);
            return (new Long(__calendarHelper.getTimeInMillis()));

        } //else if
        else if (_object instanceof long[]) {

            long[] __longArrayAttribute = (long[]) _object;
            //going to report the final value in the array of values
            return (new Long(__longArrayAttribute[__longArrayAttribute.length - 1]));
        } //else if
        else if (_object instanceof int[]) {

            int[] __intArrayAttribute = (int[]) _object;
            return (new Integer(__intArrayAttribute[__intArrayAttribute.length - 1]));
        } //else if
        else {

            Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Problem interrogating mbean attribute for Composite bean of type: " + _object.getClass() + ".");
            return (_object.getClass());
        } //else

    } //handleCompositeDataObjectForEvents

    private void executeOperations() {

        MBeanServer __mbeanServer = ManagementFactory.getPlatformMBeanServer();
        MBeanOperationConfig[] __operations = jmxHarvesterConfig.getMBeanOperationConfigs();

        ObjectName __tempMBean = null;
        Map<String, Object> __tempEventAttributes = null;
        Iterator<ObjectInstance> __oiIterator = null;
        Set<ObjectInstance> __oiSet = null;
        ObjectInstance __oiInstance = null;
        Hashtable<?, ?> __htOIProperties = null;

        for (int i = 0; i < __operations.length; i++) {

            try {

                __tempMBean = new ObjectName(__operations[i].getMBeanOperationName());
                __oiSet = __mbeanServer.queryMBeans(__tempMBean, null);

                //determine what to do with a null result
                if (__oiSet != null) {

                    __oiIterator = __oiSet.iterator();

                    while (__oiIterator.hasNext()) {

                        __oiInstance = __oiIterator.next();

                        if (__oiInstance.getObjectName().getCanonicalName().contains("DynaCache")) {

                            __tempEventAttributes = new HashMap<String, Object>();
                            __tempEventAttributes.put("MBean", __oiInstance.getObjectName().getCanonicalName());
                            __tempEventAttributes.put("jmx2insights", "operation");

                            __htOIProperties = __oiInstance.getObjectName().getKeyPropertyList();

                            // We need to handle the possiblity mbeans don't have name or type
                            // attributes. If we record them we can kill the HashMap with a null entry.
                            if (__htOIProperties.get("name") == null) {

                                __tempEventAttributes.put("MBeanInstanceName", "unknown_name");
                                //Agent.LOG.info("null name");
                            } //if
                            else {

                                __tempEventAttributes.put("MBeanInstanceName", __htOIProperties.get("name"));
                            } //else

                            if (__htOIProperties.get("type") == null) {

                                __tempEventAttributes.put("MBeanInstanceType", "unknown_type");
                                //Agent.LOG.info("null type");
                            } //if
                            else {

                                __tempEventAttributes.put("MBeanInstanceType", __htOIProperties.get("type"));
                            } //else

                            //dynacache stats ...
                            Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Beta --> Attempting WebSphere DynaCache operations. ");

                            String __cacheinstancenames[] = (String[]) __mbeanServer.invoke(__tempMBean, "getCacheInstanceNames", null, null);

                            for (int k = 0; k < __cacheinstancenames.length; k++) {

                                String __cacheInstance = __cacheinstancenames[k];
                                String __cacheStats[] = {"MemoryCacheEntries", "MemoryCacheSizeInMB", "CacheHits", "CacheMisses", "CacheRemoves"};
                                Object __params[] = {__cacheInstance, __cacheStats};
                                String __signature[] = {String.class.getName(), String[].class.getName()};
                                String __result[] = (String[]) __mbeanServer.invoke(__tempMBean, "getCacheStatistics", __params, __signature);

                                for (int l = 0; l < __result.length; l++) {

                                    if (__result[l].indexOf("=") > -1) {

                                        String[] __valuez = __result[l].split("=");
                                        Double __dValue = null;

                                        try {

                                            __dValue = new Double(__valuez[1]);
                                        } //try
                                        catch (java.lang.NumberFormatException _nfe) {

                                            //do nothing
                                            Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Problem converting value from DynaCache metrics, expected a number and got: . " + __valuez[1]);
                                        } //catch

                                        if (__dValue == null) {
                                            //put the string in there
                                            __tempEventAttributes.put(__cacheinstancenames[k] + "." + __valuez[0], __valuez[1]);
                                        } //if
                                        else {

                                            __tempEventAttributes.put(__cacheinstancenames[k] + "." + __valuez[0], __dValue);
                                        } //else

                                    } //if
                                    else {

                                        __tempEventAttributes.put(__cacheinstancenames[k] + ".UNKNOWN", __result[l]);
                                    } //else

                                } //for

                            } //for --> iterate cache instances


                        } //if --> dynacache


                    } //while

                } //if


            } //try
            catch (java.lang.Exception _e) {

                Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Problem performing operations on mbean: " + __operations[i].getMBeanOperationName());
                Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] MBean Fail Message: " + _e.getMessage());
                Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] MBean Fail Cause: " + _e.getCause());
            } //catch

        } //for

    } //executeOperation

    //listen to the agent configuration and reload if we need to - allows dynamic configuration of jmx2insights
    protected final AgentConfigListener configListener = new AgentConfigListener() {
        @Override
        public void configChanged(String _appName, AgentConfig _agentConfig) {

            //reload the coquette configuration
            Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Reloading JMXHarvester Configuration");
            getCoquetteConfig(_agentConfig);

            //reload the memory thread enabled state
            if (memoryEventsThread != null){
                memoryEventsThread.setEnabled(jmxHarvesterConfig.memoryEventsEnabled());
            }

        } //configChanged
    }; //AgentConfigListener

    private void publishInsightsEvent(Map<String,Object> mAttribs){

        Map<String, Object> params = new HashMap<String, Object>();
        params.putAll(mAttribs);
        params.putAll(jmxHarvesterConfig.getLabels());

        NewRelic.getAgent().getInsights().recordCustomEvent(jmxHarvesterConfig.getEventName(), params);

    }
} //JMXHarvester