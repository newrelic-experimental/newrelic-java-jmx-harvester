package com.newrelic.gpo.jmx2insights;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

/* newrelic agent */
import com.newrelic.agent.Agent;
import com.newrelic.api.agent.NewRelic;

public class MemoryEventsThread implements Runnable {

	private boolean enabled = false;
	private long disabled_sleep = 300000l; //will sleep for 5 minutes if disabled
	private long enabled_sleep = 1000l; //will sleep for 1 second when enabled - maybe make this configurable?
	//Beans we will hold - no need to look up each time
	private Set<ObjectInstance> oiMemory = null;
	private Set<ObjectInstance> oiGC = null;
	private Set<ObjectInstance> oiMemoryPool = null;
	MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();

	
	HashMap<String,Long> hmDeltas = null;
	
	public MemoryEventsThread(boolean _bEnabled) {
		
		super();
		enabled = _bEnabled;
		hmDeltas = new HashMap<String, Long>();

	} //MemoryEventsThread
	
	public void setEnabled(boolean _bEnabled) {
			
		enabled = _bEnabled;		
	} //setEnabled
	
	@Override
	public void run() {
		
		while(true) {
			
			try {
				
				if (enabled) {
					
					//execute a harvest block
					harvest();
					
					//sleep the thread
					zzzzzzz(enabled_sleep);
				} //if
				else {
					
					zzzzzzz(disabled_sleep);			
				} //else
				
			} //try
			catch (java.lang.Exception _e) {
				
				Agent.LOG.error("[" + Constantz.EXTENSION_NAME_MEMORY + "] Problem with MemoryEventsThread - MemoryEvents will now terminate.");
				Agent.LOG.error("[" + Constantz.EXTENSION_NAME_MEMORY + "] " + _e.getMessage());
				Agent.LOG.error("[" + Constantz.EXTENSION_NAME_MEMORY + "] " + _e.getLocalizedMessage());
				_e.printStackTrace(System.err);
			} //catch
		}//while
	} //run
	
	private void zzzzzzz(long _zzzzzzz) {
		
		Agent.LOG.fine("[" + Constantz.EXTENSION_NAME_MEMORY + "] MemoryEventsThread sleeping for: " + _zzzzzzz + " milliseconds.");
		//no harvest so just sleep the thread for the disabled sleep period
		try {
			
			Thread.sleep(_zzzzzzz);
			
		} //try
		catch (java.lang.InterruptedException _ie) {
			
			Agent.LOG.error("[" + Constantz.EXTENSION_NAME_MEMORY + "] Problem sleeping the MemoryEventsTheread - MemoryEvents will retry in " + _zzzzzzz + " milliseconds.");
			Agent.LOG.error("[" + Constantz.EXTENSION_NAME_MEMORY + "] " + _ie.getMessage());
		}
		catch (java.lang.IllegalArgumentException _iae) {
			
			Agent.LOG.error("[" + Constantz.EXTENSION_NAME_MEMORY + "] Problem sleeping the MemoryEventsTheread - MemoryEvents will retry in " + _zzzzzzz + " milliseconds.");
			Agent.LOG.error("[" + Constantz.EXTENSION_NAME_MEMORY + "] " + _iae.getMessage());
		}
		//catch
	} //zzzzzzz
	
	private boolean harvest() {
		
		boolean __bRC = true;
		Map<String, Object> __tempEventAttributes = new HashMap<String, Object>();
		Iterator<ObjectInstance> __oiIterator = null;
		ObjectName __onMemory = null;
		ObjectName __onGC = null;
		ObjectName __onMemoryPool = null;
		ObjectInstance __oiInstance = null;
		long __lBeginTimestamp;
		long __lEndTimestamp;

		__lBeginTimestamp = System.currentTimeMillis();
		__tempEventAttributes.put("timestamp", __lBeginTimestamp);
		
		//MemoryMBean begin
		try {
			
			if (oiMemory == null) {

				//allocate handle to the Memory MBean
				__onMemory = new ObjectName("java.lang:type=Memory");
				oiMemory = mbeanServer.queryMBeans(__onMemory, null);				
			}//if
			
			//evaluate if the Set is null to see of we have a real instance - at this point we should so if we don't that is an issue
			if (oiMemory != null) {
				__oiIterator = oiMemory.iterator();
				
				while(__oiIterator.hasNext()){ 
					
					__oiInstance = __oiIterator.next();
					handleAttributeValue(mbeanServer.getAttribute(__oiInstance.getObjectName(), "HeapMemoryUsage"), "Memory", "HeapMemoryUsage", __tempEventAttributes);
					handleAttributeValue(mbeanServer.getAttribute(__oiInstance.getObjectName(), "NonHeapMemoryUsage"), "Memory", "NonHeapMemoryUsage", __tempEventAttributes);
					
				} //while
							
			} //if
			else {
				
				Agent.LOG.info("[" + Constantz.EXTENSION_NAME_MEMORY + "] We still don't have an instance of the Memory MBean, this polling cycle will be skipped.");
			} //else
			
			
		} //try
		catch(java.lang.Exception _e) {
			
			Agent.LOG.error("[" + Constantz.EXTENSION_NAME_MEMORY + "] Problem during harvest of Memory Events.");
			Agent.LOG.error("[" + Constantz.EXTENSION_NAME_MEMORY + "] " + _e.getMessage());
		} //catch
		//MemoryMBean end
		Agent.LOG.finer("[" + Constantz.EXTENSION_NAME_MEMORY + "] Finished memory going onto GC ....");
		//GCMBean begin
		try {
			
			if (oiGC == null) {
				
				__onGC = new ObjectName("java.lang:type=GarbageCollector,name=*");
				oiGC = mbeanServer.queryMBeans(__onGC, null);
			} //if
			
			if (oiGC != null) {
				
				__oiIterator = oiGC.iterator();
			
				while(__oiIterator.hasNext()){ 
					
					__oiInstance = __oiIterator.next();
					handleAttributeValue(mbeanServer.getAttribute(__oiInstance.getObjectName(), "CollectionTime"), (String)mbeanServer.getAttribute(__oiInstance.getObjectName(), "Name"), "CollectionTime", __tempEventAttributes);
					handleAttributeValue(mbeanServer.getAttribute(__oiInstance.getObjectName(), "CollectionCount"), (String)mbeanServer.getAttribute(__oiInstance.getObjectName(), "Name"), "CollectionCount", __tempEventAttributes);
					
				} //while
			} //if
			else {
				
				Agent.LOG.fine("[" + Constantz.EXTENSION_NAME_MEMORY + "] We still don't have an instance of the GarbageCollector MBean, this polling cycle will be skipped.");
			} //else
		
		} //try
		catch(java.lang.Exception _e) {
			
			Agent.LOG.error("[" + Constantz.EXTENSION_NAME_MEMORY + "] Problem during harvest of GarbageCollector Events.");
			Agent.LOG.error("[" + Constantz.EXTENSION_NAME_MEMORY + "] " + _e.getMessage());
		} //catch
		//GCMBean end
		Agent.LOG.finer("[" + Constantz.EXTENSION_NAME_MEMORY + "] Finished GC going onto MemoryPools ....");
		//MemoryPoolBeans Begin
		try {
			
			if (oiMemoryPool == null) {
				
				__onMemoryPool = new ObjectName("java.lang:type=MemoryPool,name=*");
				oiMemoryPool = mbeanServer.queryMBeans(__onMemoryPool, null);
			} //if
			
			
			if (oiMemoryPool != null) {
				
				__oiIterator = oiMemoryPool.iterator();
				
				while(__oiIterator.hasNext()){ 
					
					__oiInstance = __oiIterator.next();
					handleAttributeValue(mbeanServer.getAttribute(__oiInstance.getObjectName(), "Usage"), (String)mbeanServer.getAttribute(__oiInstance.getObjectName(), "Name"), "Usage", __tempEventAttributes);
					handleAttributeValue(mbeanServer.getAttribute(__oiInstance.getObjectName(), "PeakUsage"), (String)mbeanServer.getAttribute(__oiInstance.getObjectName(), "Name"), "PeakUsage", __tempEventAttributes);
					
				} //while
			} //if
			else {
				
				Agent.LOG.fine("[" + Constantz.EXTENSION_NAME_MEMORY + "] We still don't have an instance of the GarbageCollector MBean, this polling cycle will be skipped.");
			} //else

		} //try
		catch(java.lang.Exception _e) {
			
			
		} //catch
		
		//MemoryPoolBeans End
		
		__lEndTimestamp = System.currentTimeMillis();
		__tempEventAttributes.put("harvest_end_ts", __lEndTimestamp);
		__tempEventAttributes.put("harvest_duration", __lEndTimestamp - __lBeginTimestamp);
		//record
		NewRelic.getAgent().getInsights().recordCustomEvent("JMX_MEMORY", __tempEventAttributes);
		
		return(__bRC);
	} //harvest
	
	private void handleAttributeValue(Object _oAttributeValue, String _stMBeanName, String _stAttributeName, Map<String,Object> _tmpEventAttributes) {
		
		try {
			  
			if (_oAttributeValue instanceof javax.management.openmbean.CompositeData) {
				 
				  //for this iteration we will just grab all the elements from the composite object
				Agent.LOG.finer("[" + Constantz.EXTENSION_NAME_MEMORY + "] Processing CompositeData MBean: " + _stMBeanName);
				  Agent.LOG.finer("[" + Constantz.EXTENSION_NAME_MEMORY + "] Processing CompositeData Attribute: " + _stAttributeName);
				  Agent.LOG.fine("[" + Constantz.EXTENSION_NAME_MEMORY + "] Recording all Composite Atrribute Header Elements.");
					  
					  for (String __key : ((javax.management.openmbean.CompositeData)_oAttributeValue).getCompositeType().keySet()) {
				            
						  Agent.LOG.fine("[" + Constantz.EXTENSION_NAME_MEMORY + "] Beta: Processing CompositeData Attribute: " + _stMBeanName + " : "+ _stAttributeName + ", with key: " + __key);
						  _tmpEventAttributes.put(_stMBeanName + "_" + _stAttributeName + "_" + __key, handleCompositeDataObject(((javax.management.openmbean.CompositeData)_oAttributeValue).get(__key)));							  
					  } //for

			} //if
			else  if (_oAttributeValue instanceof java.lang.Number) { 
				
				try {
					
					Long __lValue = (Long)_oAttributeValue;
					Long __lPreviousValue = hmDeltas.get(_stMBeanName + "_" +_stAttributeName); //TODO maybe manage this string differently
					long __lDelta;
					
					if (__lPreviousValue != null) {
						
						if (__lPreviousValue.longValue() <= 0l) {
							
							__lDelta =  ((java.lang.Long)_oAttributeValue).longValue();
							Agent.LOG.fine("[" + Constantz.EXTENSION_NAME_MEMORY + "] GCCollectionTime was -1 or 0 the value is now: ." + __lDelta);
						} //if
						else {
							//this should be a net value greater than the previous initialized value or the same ... 
							Agent.LOG.fine("[" + Constantz.EXTENSION_NAME_MEMORY + "] GCCollectionTime was not -1 the value was: ." + __lPreviousValue);
							__lDelta = ((java.lang.Long)_oAttributeValue).longValue() - __lPreviousValue;
							Agent.LOG.fine("[" + Constantz.EXTENSION_NAME_MEMORY + "] GCCollectionTime was not -1 the value is now: ." + __lValue);
							Agent.LOG.fine("[" + Constantz.EXTENSION_NAME_MEMORY + "] The Delta we are recording for GCCollectionTime is: " + __lDelta);
						} //else
						
						_tmpEventAttributes.put(_stMBeanName + "_" +_stAttributeName + "_delta", new Long(__lDelta));
					} //if
					
					hmDeltas.put(_stMBeanName + "_" +_stAttributeName, __lValue); //place the last value in the cache
				} //try
				catch (java.lang.Exception _e) {
					
					Agent.LOG.error("[" + Constantz.EXTENSION_NAME_MEMORY + "] Problem processing Numeric value for Delta operation lookup.");
					Agent.LOG.error("[" + Constantz.EXTENSION_NAME_MEMORY + "] Message: " + _e.getMessage());
				} //catch				
				
				//place the realized value
				_tmpEventAttributes.put(_stMBeanName + "_" +_stAttributeName, _oAttributeValue);
				

			}//else if --> long
			else {
				  
				Agent.LOG.fine("[" + Constantz.EXTENSION_NAME_MEMORY + "] Attribute Check :: Unsupported attribute type: " + _oAttributeValue.getClass() + " for: " + _stAttributeName);
			} //else							  
		  } //try
		  catch (java.lang.Exception _e) {
			  
			  Agent.LOG.fine("[" + Constantz.EXTENSION_NAME_MEMORY + "] Problem interrogating mbean attribute: " + _stAttributeName + " during harvest.");
			  Agent.LOG.fine("[" + Constantz.EXTENSION_NAME_MEMORY + "] Message MBean Attribute Access Fail: " + _e.getMessage());
		  } //catch
		
	} //handleAttributeValue
	
	private Object handleCompositeDataObject(java.lang.Object _object) {
		
		  if (_object instanceof java.lang.Number || _object instanceof java.lang.String || _object instanceof java.lang.Boolean) { 
				  
			  return(_object);			  
		  } //if
		  else if (_object instanceof java.util.Date) { 	  
	
			  java.util.Date __dateAttribute = (java.util.Date)_object;
			  java.util.Calendar __calendarHelper = (java.util.Calendar.getInstance());
			  __calendarHelper.setTime(__dateAttribute);
			  return(new Long(__calendarHelper.getTimeInMillis()));
			  
		  } //else if
		  else if (_object instanceof long[]) {
			  
			  long[] __longArrayAttribute = (long[])_object;
			  //going to report the final value in the array of values
			  return(new Long(__longArrayAttribute[__longArrayAttribute.length - 1]));
		  } //else if
		  else if (_object instanceof int[]) {
			  
			  int[] __intArrayAttribute = (int[])_object;
			  return(new Integer(__intArrayAttribute[__intArrayAttribute.length - 1]));
		  } //else if
		  else {
			  
			  Agent.LOG.fine("[" + Constantz.EXTENSION_NAME_MEMORY + "] Problem interrogating mbean attribute for Composite bean of type: " + _object.getClass() + ".");
			  return(_object.getClass());
		  } //else
		
	} //handleCompositeDataObject
} //MemoryEventsThread
