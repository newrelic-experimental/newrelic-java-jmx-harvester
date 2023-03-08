package com.newrelic.labs.jmxharvester;

/* java core */
import java.io.IOException;
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

/* telemetry sdk */
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.MetricBatchSenderFactory;
import com.newrelic.telemetry.http.HttpPoster;
import com.newrelic.telemetry.metrics.MetricBatchSender;
import java.util.function.Supplier;
import com.newrelic.telemetry.SenderConfiguration.SenderConfigurationBuilder;

/* mbean processors */
import com.newrelic.labs.jmxharvester.processors.*;

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
    private IProcessor processor = null;
    
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

         
    	if (jmxHarvesterConfig == null) {
    		Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] JMXHarvesterConfig is null - attempting to load the config. This harvest will be skipped.");
    		getHarvesterConfig(null);
    	} //if
    	else {
    		
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
            
    	} //else

    	
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
	            
          Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Sleep time: " + sleepTime);
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
          
          getHarvesterConfig(null);
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
		
	} //doStart

    @Override
    protected void doStop() throws Exception {

        Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] The service is stopping.");
    } //doStop

    @SuppressWarnings("unchecked")
    private void getHarvesterConfig(AgentConfig _agentConfig) {
    	
    	// the harvester config can only be acquired when the agent has registered with the service
    	// therefore a service check must pass before the agent config is attempted to be loaded
    	HarvestService _hs = null;
    
        try {
        	
        	_hs = ServiceFactory.getHarvestService();
        	if (_hs == null) {
        		Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Haverst Service is null therefore the agent has not yet fully started for the jmxharvester.");
        		jmxHarvesterConfig = null;
        	} //if
        	else {
        		
                if (_agentConfig == null) {
                    _agentConfig = ServiceFactory.getConfigService().getLocalAgentConfig();
                } //if

                Map<String, Object> props = _agentConfig.getProperty(Constantz.YML_SECTION);
                Map<String, String> metadata = NewRelic.getAgent().getLinkingMetadata();
                jmxHarvesterConfig = new JMXHarvesterConfig(props);
                jmxHarvesterConfig.setLabels(_agentConfig.getLabelsConfig().getLabels()); //TODO Validate how this works with metric attributes 
                jmxHarvesterConfig.setAppName(_agentConfig.getApplicationName());
                jmxHarvesterConfig.setLicenseKey(_agentConfig.getLicenseKey());  
                jmxHarvesterConfig.setHostname(metadata.get("hostname"));
                jmxHarvesterConfig.setEntityGuid(metadata.get("entity.guid"));
                Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Successfully loaded JMXHarvester Configuration");
                Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Configuration = " + props);
                Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] labels = " + _agentConfig.getLabelsConfig().getLabels());
        	} //else


        } //try
        catch (java.lang.Exception _e) {

            Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] Problem loading the jmx_harvester config from newrelic.yml. JMXHarvester set to enabled: false.");
            Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] Message: " + _e.getMessage());
            
            Map<String, Object> __disabledConfig = new HashMap<String, Object>();
            __disabledConfig.put("enabled", "false");
            jmxHarvesterConfig = new JMXHarvesterConfig(__disabledConfig);
        } //catch

        setUpMetricsSystem();
    } //getHarvesterConfig
    
 
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
    	
        //initialize the metrics system
        Supplier<HttpPoster> __httpPosterCreator = () -> new OkHttpPoster(Duration.of(10, ChronoUnit.SECONDS));
    	SenderConfigurationBuilder __metricConfig = MetricBatchSenderFactory.fromHttpImplementation(__httpPosterCreator).configureWith(ServiceFactory.getConfigService().getLocalAgentConfig().getLicenseKey()).useLicenseKey(true);
    	metricBatchSender = MetricBatchSender.create(__metricConfig.build());
    	    	
    	//update jmxConfig with the global metric attributes
    	jmxHarvesterConfig.addGlobalAttribute("jmx_harvester_mode", jmxHarvesterConfig.getMode());
    	jmxHarvesterConfig.addGlobalAttribute("host", jmxHarvesterConfig.getHostname());
    	jmxHarvesterConfig.addGlobalAttribute("entity.guid", jmxHarvesterConfig.getEntityGuid());
    	jmxHarvesterConfig.addGlobalAttribute("appName", jmxHarvesterConfig.getAppName());    	
    	
    } //setupMetricsSystem

    private void executePromiscuous() {

        MBeanServer __mbeanServer = ManagementFactory.getPlatformMBeanServer();
        processor = new PromiscuousProcessor();
    	processor.execute(__mbeanServer, null, jmxHarvesterConfig, metricBatchSender);
        
    } //executePromiscuous

    private void executeStrict() {

        MBeanServer __mbeanServer = ManagementFactory.getPlatformMBeanServer();
        MBeanConfig[] __mbeans = jmxHarvesterConfig.getMBeans();
        processor = new StrictProcessor();
    	processor.execute(__mbeanServer, __mbeans, jmxHarvesterConfig, metricBatchSender);
    	
    } //executeStrict
    

    private void executeOpen() {

        MBeanServer __mbeanServer = ManagementFactory.getPlatformMBeanServer();
        MBeanConfig[] __mbeans = jmxHarvesterConfig.getMBeans();
        processor = new OpenProcessor();
    	processor.execute(__mbeanServer, __mbeans, jmxHarvesterConfig, metricBatchSender);
        
    } //executeOpen

    /**
     * Polls all MBean attributes and operations and persists them to New Relic as metrics. Metrics are used because
     * there can be many values we're trying to persist and persisting them as events can easily overwhelm the event 
     * system within the Java agent. 
     */
    private void executeInventory() {

    	MBeanServer __mbeanServer = ManagementFactory.getPlatformMBeanServer();
    	processor = new InventoryProcessor();
    	processor.execute(__mbeanServer, null, jmxHarvesterConfig, metricBatchSender);
    	
    } //executeInventory

    //listen to the agent configuration and reload if we need to - allows dynamic configuration of the jmxharvester
    protected final AgentConfigListener configListener = new AgentConfigListener() {
        @Override
        public void configChanged(String _appName, AgentConfig _agentConfig) {

            //reload the harvester configuration
            Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Reloading JMXHarvester Configuration");
            getHarvesterConfig(_agentConfig);

            //reload the memory thread enabled state
            if (memoryEventsThread != null){
                memoryEventsThread.setEnabled(jmxHarvesterConfig.memoryEventsEnabled());
            }

        } //configChanged
    }; //AgentConfigListener

} //JMXHarvester