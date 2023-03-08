package com.newrelic.labs.jmxharvester.processors;

import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.management.ObjectInstance;
import javax.management.openmbean.CompositeData;

import com.newrelic.agent.Agent;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.labs.jmxharvester.Constantz;
import com.newrelic.labs.jmxharvester.JMXHarvesterConfig;
import com.newrelic.labs.jmxharvester.MBeanAttributeConfig;
import com.newrelic.labs.jmxharvester.MBeanConfig;
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.metrics.Gauge;
import com.newrelic.telemetry.metrics.MetricBuffer;

public class Utilities {

	public static boolean testAndIncrementMBeanPollingInterval(MBeanConfig _mbean) {
		
		boolean __bRC = false;
		
		if (_mbean.getPollingInterval() > _mbean.getMBeanPollingCounter()) {
			
			_mbean.incrementMBeanPollingCounter();
			__bRC= false;
		} //if
		else if (_mbean.getPollingInterval() < _mbean.getMBeanPollingCounter()) {
			
			// this is an invalid state and we reset the polling interval counter
			_mbean.resetMBeanPollingCounter();
			__bRC= false;
		} //else if
		else {
			// polling interval counter is equal so reset and indicate true
			_mbean.resetMBeanPollingCounter();
			__bRC= true;
		} //else
		
		return(__bRC);
	} //testAndIncrementMBeanPollingInterval
	
	public static void normalizeMBeanName(MBeanConfig _mbean) {
		
        if (_mbean.getMBeanName().charAt(0) == '\\') {
            
            _mbean.setMBeanName(_mbean.getMBeanName().substring(1));
        } //if

	} //normalizeMBeanName

	public static Map<String, Object> getEventAttributes(ObjectInstance _oiInstance) {
		
		Hashtable<?, ?> __htOIProperties = _oiInstance.getObjectName().getKeyPropertyList();
		Map<String, Object> __tempEventAttributes = new HashMap<String, Object>();
		
        __tempEventAttributes.put("MBean", _oiInstance.getObjectName().getCanonicalName());
        
        // We need to handle the possibility mbeans don't have name or type
        // attributes. If we record them we can kill the HashMap with a null entry.
        __tempEventAttributes.put("MBeanInstanceName", (__htOIProperties.get("name") == null) ? "unknown_name" : __htOIProperties.get("name"));
        __tempEventAttributes.put("MBeanInstanceType", (__htOIProperties.get("type") == null) ? "unknown_type" : __htOIProperties.get("type"));
        
        return(__tempEventAttributes);
	} //getEventAttributes

	public static Attributes getMetricAttributes(ObjectInstance _oiInstance) {
		
		Attributes __metricAttributes = new Attributes();

    	__metricAttributes.put("mbean_name", _oiInstance.getObjectName().getCanonicalName());
    	
    	return(__metricAttributes);
	} //getMetricAttributes
	
	
    public static void handleMBeanAttributeAsEvent(Object _oAttributeValue, MBeanAttributeConfig _macAttributeConfig, Map<String, Object> _mEventHolder, Vector<Map> _vTabularEvents) {

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
                Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Processing CompositeData Attribute: " + _macAttributeConfig.getAttributeName());

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
            /* adding javax.management.openmbean.TabularData support */
            else if (_oAttributeValue instanceof javax.management.openmbean.TabularData) {

                Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Processing TabularData Attribute: " + _macAttributeConfig.getAttributeName());
                Map<String, Object> __mTabularEventHolder = null;
                //Iterator<?> __iTabularEventValueIterator;


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

                                Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Recording a subset of the Composite Attribute Header Elements.");

                                String[] __stAttributeHeaders = _macAttributeConfig.getAttributeElementHeaders();
                                for (int i = 0; i < __stAttributeHeaders.length; i++) {

                                    __mTabularEventHolder.put(__stAttributeHeaders[i], handleCompositeDataObjectForEvents(__tempCD.get(__stAttributeHeaders[i])));
                                } //for
                            } //if
                            else {

                                Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Recording all Composite Attribute Header Elements.");

                                for (String __key : (__tempCD.getCompositeType().keySet())) {

                                    __mTabularEventHolder.put(__key, handleCompositeDataObjectForEvents(__tempCD.get(__key)));
                                } //for
                            } //else

                        } //if
                        else {

                            Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Non-CompositeData encountered in TabularData object: " + __tempObj.getClass().getName());
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

    } //handleMBeanAtrributeAsEvent
    
    /*
     * For the time being using this as a second stage to interrogate the objects embedded within a CompositeData object.
     */
    private static Object handleCompositeDataObjectForEvents(java.lang.Object _object) {

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


	public static void handleMBeanAttributeAsMetric(JMXHarvesterConfig _jmxHarvesterConfig, MetricBuffer _metricBuffer, Object _oAttributeValue, String _stMBeanName, MBeanAttributeConfig _macAttributeConfig, Attributes _attributes) {
    	
//    	Agent.LOG.debug("[" + Constantz.EXTENSION_NAME + "] 1. _metricBuffer >>>>>>>>> " + _metricBuffer);
//    	Agent.LOG.debug("[" + Constantz.EXTENSION_NAME + "] 2. _oAttributeValue >>>>>>>>> " + _oAttributeValue);
//    	Agent.LOG.debug("[" + Constantz.EXTENSION_NAME + "] 3. _stMBeanName >>>>>>>>> " + _stMBeanName);
//    	Agent.LOG.debug("[" + Constantz.EXTENSION_NAME + "] 4. _macAtrributeConfig >>>>>>>>> " + _macAttributeConfig);
//    	Agent.LOG.debug("[" + Constantz.EXTENSION_NAME + "] 5. _attributes >>>>>>>>> " + _attributes);
    	
    	try {
    		
    		if (_oAttributeValue instanceof java.lang.Number) {

    			_metricBuffer.addMetric(new Gauge(_jmxHarvesterConfig.getMetricPrefix() + "." + _stMBeanName + "." + _macAttributeConfig.getAttributeName(), ((Number)_oAttributeValue).doubleValue(), System.currentTimeMillis(), _attributes));
              
            } //if
            else if (_oAttributeValue instanceof java.lang.String || _oAttributeValue instanceof java.lang.Boolean) {

            	_attributes.put("raw_value", _oAttributeValue.toString());
    			_metricBuffer.addMetric(new Gauge(_jmxHarvesterConfig.getMetricPrefix() + "." + _stMBeanName + "." + _macAttributeConfig.getAttributeName(), 0d, System.currentTimeMillis(), _attributes));
    			
            } //else if
            else if (_oAttributeValue instanceof java.util.Date) {

                java.util.Date __dateAttribute = (java.util.Date) _oAttributeValue;
                java.util.Calendar __calendarHelper = (java.util.Calendar.getInstance());
                __calendarHelper.setTime(__dateAttribute);
                _attributes.put("raw_value", new Long(__calendarHelper.getTimeInMillis()));
    			_metricBuffer.addMetric(new Gauge(_jmxHarvesterConfig.getMetricPrefix() + "." + _stMBeanName + "." + _macAttributeConfig.getAttributeName(), 0d, System.currentTimeMillis(), _attributes));
            } //else if 
            else if (_oAttributeValue instanceof javax.management.openmbean.CompositeData) {

                //for this iteration we will just grab all the elements from the composite object
                Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Processing CompositeData Attribute: " + _macAttributeConfig.getAttributeName());

                /* determine if we are looking up discreet header values or getting all of them */
                if (_macAttributeConfig.hasAttributeElements()) {

                    Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Recording a subset of the Composite Atrribute Header Elements.");

                    String[] __stAttributeHeaders = _macAttributeConfig.getAttributeElementHeaders();
                    
                    for (int i = 0; i < __stAttributeHeaders.length; i++) {

            			_metricBuffer.addMetric(
        					new Gauge(
        							_jmxHarvesterConfig.getMetricPrefix() + "." + _stMBeanName + "." + _macAttributeConfig.getAttributeName() + "." + __stAttributeHeaders[i], 
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
            							_jmxHarvesterConfig.getMetricPrefix() + "." + _stMBeanName + "." + _macAttributeConfig.getAttributeName() + "." + __key, 
            							handleCompositeDataObjectForMetrics(((javax.management.openmbean.CompositeData)_oAttributeValue).get(__key), __key), 
                					    System.currentTimeMillis(), 
                					   _attributes));
                    } //for
                } //else
            } //else if --> CompositeData MBean
    		//////
            /* adding javax.management.openmbean.Y=TabularData support */
            else if (_oAttributeValue instanceof javax.management.openmbean.TabularData) {

                Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Processing TabularData Attribute as gauge metric: " + _macAttributeConfig.getAttributeName());
                Iterator<?> __tabularDataIterator = ((javax.management.openmbean.TabularData) _oAttributeValue).values().iterator();
                Object __tempObj;
                CompositeData __tempCD;

                try {

                    while (__tabularDataIterator.hasNext()) {

                        __tempObj = __tabularDataIterator.next();

                        if (__tempObj instanceof CompositeData) {
//                        	Agent.LOG.info("instance of composite then cast");
//                        	Agent.LOG.info(__tempObj.toString());
//                        	
//                        	if (__tempObj instanceof javax.management.openmbean.CompositeDataSupport) {
//                        		
//                        		Agent.LOG.info("this is a composite data object");
//                        		javax.management.openmbean.CompositeDataSupport foo = (javax.management.openmbean.CompositeDataSupport)__tempObj;
//                        		Collection<?> womp = foo.values();
//                        		Iterator<?> itty = womp.iterator();
//                        		while(itty.hasNext()) {
//                        			
//                        			Agent.LOG.info("wtf is this " + itty.next().toString());
//                        		}
//                        	} //if
//                        	else {
//                        		
//                        		Agent.LOG.info("it's a what?: " + __tempObj.getClass().getCanonicalName());
//                        	} //else
                        	
                        	/* Encountered a situation where the object passed isn't a composite object and will throw an exception.
                        	 *  javax.management.openmbean.CompositeDataSupport(compositeType=javax.management.openmbean.CompositeType(name=java.util.Map<java.lang.String, java.lang.String>,items=((itemName=key,itemType=javax.management.openmbean.SimpleType(name=java.lang.String)),(itemName=value,itemType=javax.management.openmbean.SimpleType(name=java.lang.String)))),contents={key=awt.toolkit, value=sun.awt.X11.XToolkit})
                        	 *  key: wtf is this awt.toolkit
                        	 *  value: sun.awt.X11.XToolkit
                        	 */
                        	//TODO NEED A BETTER WAY TO HANDLE THIS SITUATION
                            __tempCD = (CompositeData) __tempObj;

                            /* determine if we are looking up discreet header values or getting all of them */
                            if (_macAttributeConfig.hasAttributeElements()) {

                                Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Recording a subset of the Composite Atrribute Header Elements for Tabular MBean type.");

                                String[] __stAttributeHeaders = _macAttributeConfig.getAttributeElementHeaders();
                                for (int i = 0; i < __stAttributeHeaders.length; i++) {

//                                	Agent.LOG.info(_stMBeanName);
//                                	Agent.LOG.info(_macAttributeConfig.getAttributeName());
//                                	Agent.LOG.info("--------------");
//                           
                                    _metricBuffer.addMetric(
                        					new Gauge(
                        							_jmxHarvesterConfig.getMetricPrefix() + "." + _stMBeanName + "." + _macAttributeConfig.getAttributeName() + "." + __stAttributeHeaders[i], 
                        							handleCompositeDataObjectForMetrics(((javax.management.openmbean.CompositeData)_oAttributeValue).get(__stAttributeHeaders[i]), __stAttributeHeaders[i]), 
                            					    System.currentTimeMillis(), 
                            					   _attributes));
                                    
                                } //for
                            } //if
                            else {

                                Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Recording all Composite Atrribute Header Elements for Tabular Mbean type.");

                                for (String __key : (__tempCD.getCompositeType().keySet())) {
//
//                                	Agent.LOG.info(_stMBeanName);
//                                	Agent.LOG.info(_macAttributeConfig.getAttributeName());
//                                	Agent.LOG.info("--------------");
                          
                                    _metricBuffer.addMetric(
                        					new Gauge(
                        							_jmxHarvesterConfig.getMetricPrefix() + "." + _stMBeanName + "." + _macAttributeConfig.getAttributeName() + "." + __key, 
                        							handleCompositeDataObjectForMetrics(((javax.management.openmbean.CompositeData)_oAttributeValue).get(__key), __key), 
                            					    System.currentTimeMillis(), 
                            					   _attributes));                        
                                } //for
                            } //else

                        } //if
                        else {

                            Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Non-CompositeData encountered in TabularData object: " + __tempObj.getClass().getName());
                        } //else

                    } //while

                } //try
                catch (java.lang.Exception _e) {

                    Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] Issue interrogating TabularData CompositeData object. Turn on fine logging for more details.");
                    Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] Problem interrogating mbean attribute: " + _macAttributeConfig.getAttributeName() + " during harvest.");
                    Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] Message MBean Attribute Access Fail: " + _e.getMessage());
                } //catch

            } //else if --> TabularData Mbean
    		else {
    			
    			Agent.LOG.fine("[" + Constantz.EXTENSION_NAME + "] Encountered a non-numeric value in compsite object, ignoring. ");
    		} //else
    		
    	} //try
    	catch (java.lang.Exception _e) {
    		
    		Agent.LOG.error("[" + Constantz.EXTENSION_NAME + "] A problem occurred processing the mbean attribute as a metric: " + _e.getCause());
    	} //catch
    	
    } //handleMBeanAttributeAsMetric

	private static double handleCompositeDataObjectForMetrics(java.lang.Object _object, String _key) {
		
//		Agent.LOG.info("THE KEY THAT IS BEING PASSSED?" + _key);
		try {

			  if (_object instanceof java.lang.String || _object instanceof java.lang.Boolean) { 
				  
				  Agent.LOG.finer("[" + Constantz.EXTENSION_LOG_STRING + "] Problem interrogating mbean attribute for Composite bean of type: " + _object.getClass() + ". Object value: " + _object.toString() );
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
			  else if (_object instanceof javax.management.openmbean.TabularDataSupport) {
//				  Agent.LOG.info("tabular data");
//				  javax.management.openmbean.TabularDataSupport __tds = (javax.management.openmbean.TabularDataSupport)_object;
//				  Set<Object> foo = __tds.keySet();
//				  Iterator<Object> itty = foo.iterator();
//				  while(itty.hasNext()) {
//					  Agent.LOG.info("is --> " + itty.next().toString());
//				  } //while
				  /*
				   * A composite object containing a tabular datastructure ... eg ...
				   * composite mbean entry >> memoryUsageBeforeGc (contains keys)
				   *  --> [Survivor Space]
				   *  --> [Compressed Class Space]
				   *  --> [Eden Space]
				   *  --> [Metaspace]
				   *  --> [Code Cache]
				   *  --> [Tenured Gen]
				   *  
				   *  or
				   *  
				   *  composite mbean entry >> memoryUsageAfterGc (contains keys)
				   *  --> [Survivor Space]
				   *  --> [Compressed Class Space]
				   *  --> [Eden Space]
				   *  --> [Metaspace]
				   *  --> [Code Cache]
				   *  --> [Tenured Gen]
				   */
				  //TODO Have to refactor the way these composite objects are handled from the ground up. ^^^
				  Agent.LOG.fine("[" + Constantz.EXTENSION_LOG_STRING + "] Tabular data found within a composite object. JMXHarvester doesn't currently support MBean aattributes of this type.");
				  return(-63.18d); // don't support this nested tabular data structure - this is the triple point of nitrogen as a negative
			  } //else if
			  else {
				  
				  return(-234.3156d); //invalid type returns mercury triple point temp in k as negative
			  } //else
		}
		catch(java.lang.Exception _e) {
			
			Agent.LOG.error("[" + Constantz.EXTENSION_LOG_STRING + "] Problem interrogating mbean attribute for Composite bean of type: " + _object.getClass() + ". Object value: " + _object.toString() );
			return(-83.8058d); //invalid type returns argon triple point temp in k as negative
		} //catch

	} //handleCompositeDataObjecthandleCompositeDataObjectForMetrics
	
    public static void publishInsightsEvent(JMXHarvesterConfig _jmxHarvesterConfig, Map<String,Object> mAttribs){

        Map<String, Object> params = new HashMap<String, Object>();
        params.putAll(mAttribs);
        params.putAll(_jmxHarvesterConfig.getLabels());

        NewRelic.getAgent().getInsights().recordCustomEvent(_jmxHarvesterConfig.getEventName(), params);

    } //publishInsightsEvent
} //Utilities
