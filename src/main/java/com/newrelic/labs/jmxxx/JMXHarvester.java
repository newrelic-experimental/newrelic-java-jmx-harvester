package com.newrelic.labs.jmxxx;

/* java core */
import java.lang.management.ManagementFactory;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import java.util.Collection;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

/* newrelic agent */
import com.newrelic.agent.Agent;
import com.newrelic.agent.HarvestListener;
import com.newrelic.agent.HarvestService;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigListener;
import com.newrelic.agent.deps.com.google.common.collect.Multiset.Entry;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.api.agent.NewRelic;


/* project */
import com.newrelic.labs.jmxxx.Constanz;
//
import com.newrelic.gpo.jmx2insights.Constantz;
import com.newrelic.gpo.jmx2insights.JMX2Insights;
import com.newrelic.gpo.jmx2insights.JMX2InsightsConfig;
import com.newrelic.gpo.jmx2insights.MemoryEventsThread;

/**
 * The main agent extension object for JMXHarvester
 * 
 * @author gil
 * @since 1.0
 */
public class JMXHarvester extends AbstractService implements HarvestListener {

    private int invocationCounter = 1;
    private JMXHarvesterConfig jmxharvesterConfig = null;
    private MemoryEventsThread memoryEventsThread = null; //first implementation of the memory thread.

    public JMXHarvester() {

        super(JMXHarvester.class.getSimpleName());
        Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] Initializing Service Class.");


    } //JMX2Insights

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
    protected void doStop() throws Exception {

        Agent.LOG.info("[" + Constantz.EXTENSION_NAME + "] The service is stopping.");
    } //doStop

    @SuppressWarnings("unchecked")
    private void getJMXHarvesterConfig(AgentConfig _agentConfig) {

        try {

            if (_agentConfig == null) {
                _agentConfig = ServiceFactory.getConfigService().getLocalAgentConfig();
            }
            Map<String, Object> props = _agentConfig.getProperty(Constantz.YML_SECTION);

            jmx2insightsConfig = new JMX2InsightsConfig(props);
            jmx2insightsConfig.setLabels(_agentConfig.getLabelsConfig().getLabels());

            Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Successfully loaded JMX2Insights Configuration");
            Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Configuration = " + props);
            Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] labels = " + _agentConfig.getLabelsConfig().getLabels());
        } //try
        catch (java.lang.Exception _e) {

            Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] Problem loading the jmx2insights config from newrelic.yml. JMX2Insights set to enabled: false.");
            Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] Message: " + _e.getMessage());

            Map<String, Object> __disabledConfig = new HashMap<String, Object>();
            __disabledConfig.put("enabled", "false");
            jmx2insightsConfig = new JMX2InsightsConfig(__disabledConfig);
        } //catch

    } //getJMXHarvesterConfig
    
    /**
     * Agent config listener innerclass to listen for change events. 
     */
    protected final AgentConfigListener configListener = new AgentConfigListener() {
        @Override
        public void configChanged(String _appName, AgentConfig _agentConfig) {

            //reload the coquette configuration
            Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Reloading JMXHarvester Configuration");
            getJMXHarvesterConfig(_agentConfig);

            //reload the memory thread enabled state
            if (memoryEventsThread != null){
                memoryEventsThread.setEnabled(jmx2insightsConfig.memoryEventsEnabled());
            }

        } //configChanged
    }; //AgentConfigListener

    /**
     * 
     * @param mAttribs
     */
    private void publishEvent(Map<String,Object> mAttribs){

        Map<String, Object> params = new HashMap<String, Object>();
        params.putAll(mAttribs);
        params.putAll(jmxharvesterConfig.getLabels());

        NewRelic.getAgent().getInsights().recordCustomEvent(jmxharvesterConfig.getEventName(), params);

    } //publishEvent
    
	
} //JMXHarvester