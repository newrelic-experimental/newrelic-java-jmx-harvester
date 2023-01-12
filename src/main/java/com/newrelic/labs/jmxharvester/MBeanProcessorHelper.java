package com.newrelic.labs.jmxharvester;

import java.util.Map;

import com.newrelic.agent.Agent;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.telemetry.metrics.MetricBuffer;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.metrics.Gauge;

public class MBeanProcessorHelper {

	
//	public static void handleAttributeValueAsMetric(MetricBuffer _metricBuffer, String _metricPrefix, String _stMBeanName, String _stAttributeName, Object _oAttributeValue, Attributes _metricAttributes) {
//
//		
//		try {
//			
//			String __stMetricName = _metricPrefix + "." + _stMBeanName + "." + _stAttributeName;
//			if (_oAttributeValue instanceof javax.management.openmbean.CompositeData) {
//				 
//				//for this iteration we will just grab all the elements from the composite object
//				Agent.LOG.finer("[" + Constantz.EXTENSION_NAME_MEMORY + "] Processing CompositeData MBean: " + _stMBeanName);
//				Agent.LOG.finer("[" + Constantz.EXTENSION_NAME_MEMORY + "] Processing CompositeData Attribute: " + _stAttributeName);
//				Agent.LOG.fine("[" + Constantz.EXTENSION_NAME_MEMORY + "] Recording all Composite Atrribute Header Elements.");
//				
//				 
//				for (String __key : ((javax.management.openmbean.CompositeData)_oAttributeValue).getCompositeType().keySet()) {
//				            
//					Agent.LOG.finer("[" + Constantz.EXTENSION_LOG_STRING + "] Processing CompositeData Attribute: " + _stMBeanName + " : "+ _stAttributeName + ", with key: " + __key);			
//					_metricBuffer.addMetric(new Gauge(__stMetricName + "." + __key, handleCompositeDataObject(((javax.management.openmbean.CompositeData)_oAttributeValue).get(__key)), System.currentTimeMillis(), _metricAttributes));
//
//				} //for
//
//			} //if --> CompositeData
//			
//			else  if (_oAttributeValue instanceof java.lang.Number) { 
//				
//				try {
//					
////					Long __lValue = (Long)_oAttributeValue;
////					Long __lPreviousValue = hmDeltas.get(_stMBeanName + "_" +_stAttributeName); //TODO maybe manage this string differently
////					long __lDelta;
//					
////					if (__lPreviousValue != null) {
////						
////						if (__lPreviousValue.longValue() <= 0l) {
////							
////							__lDelta =  ((java.lang.Long)_oAttributeValue).longValue();
////							Agent.LOG.fine("[" + Constantz.EXTENSION_NAME_MEMORY + "] GCCollectionTime was -1 or 0 the value is now: ." + __lDelta);
////						} //if
////						else {
////							//this should be a net value greater than the previous initialized value or the same ... 
////							Agent.LOG.fine("[" + Constantz.EXTENSION_NAME_MEMORY + "] GCCollectionTime was not -1 the value was: ." + __lPreviousValue);
////							__lDelta = ((java.lang.Long)_oAttributeValue).longValue() - __lPreviousValue;
////							Agent.LOG.fine("[" + Constantz.EXTENSION_NAME_MEMORY + "] GCCollectionTime was not -1 the value is now: ." + __lValue);
////							Agent.LOG.fine("[" + Constantz.EXTENSION_NAME_MEMORY + "] The Delta we are recording for GCCollectionTime is: " + __lDelta);
////						} //else
////						
////						_tmpEventAttributes.put(_stMBeanName + "_" +_stAttributeName + "_delta", new Long(__lDelta));
////					} //if
//					
////					hmDeltas.put(_stMBeanName + "_" +_stAttributeName, __lValue); //place the last value in the cache
//					
//					
//					_metricBuffer.addMetric(new Gauge(__stMetricName, ((Number)_oAttributeValue).doubleValue(), System.currentTimeMillis(), _metricAttributes));
//					
//				} //try
//				catch (java.lang.Exception _e) {
//					
//					Agent.LOG.error("[" + Constantz.EXTENSION_LOG_STRING + "] Problem processing Numeric value for Delta operation lookup.");
//					Agent.LOG.error("[" + Constantz.EXTENSION_LOG_STRING + "] Message: " + _e.getMessage());
//				} //catch				
//			}//else if --> long
//			else {
//				  
//				Agent.LOG.fine("[" + Constantz.EXTENSION_LOG_STRING + "] Attribute Check :: Unsupported attribute type: " + _oAttributeValue.getClass() + " for: " + _stAttributeName);
//			} //else							  
//		  } //try
//		  catch (java.lang.Exception _e) {
//			  
//			  Agent.LOG.fine("[" + Constantz.EXTENSION_LOG_STRING + "] Problem interrogating mbean attribute: " + _stAttributeName + " during harvest.");
//			  Agent.LOG.fine("[" + Constantz.EXTENSION_LOG_STRING + "] Message MBean Attribute Access Fail: " + _e.getMessage());
//		  } //catch
//			
//	} //handleAttributeValueAsMetric
//	
//	//handleAttributeValueAsEvent TODO
//	//handleAttributeValueAsLog  ??
//	
//	
//	private static double handleCompositeDataObject(java.lang.Object _object) {
//		
//		try {
//
//			  if (_object instanceof java.lang.String || _object instanceof java.lang.Boolean) { 
//				  
//				  Agent.LOG.fine("[" + Constantz.EXTENSION_LOG_STRING + "] Problem interrogating mbean attribute for Composite bean of type: " + _object.getClass() + ". Object value: " + _object.toString() );
//				  return(-273.2d); //invalid type returns water triple point temp in k 	as negative		  
//			  } //if
//			  else if (_object instanceof java.lang.Number) { 
//				  
//				  return(((Number)_object).doubleValue());			  
//			  } //if
//			  else if (_object instanceof java.util.Date) { 	  
//		
//				  java.util.Date __dateAttribute = (java.util.Date)_object;
//				  java.util.Calendar __calendarHelper = (java.util.Calendar.getInstance());
//				  __calendarHelper.setTime(__dateAttribute);
//				  return((double)(__calendarHelper.getTimeInMillis()));
//				  
//			  } //else if
//			  //take last value of an array
//			  else if (_object instanceof long[]) {
//				  
//				  long[] __longArrayAttribute = (long[])_object;
//				  //going to report the final value in the array of values
//				  return((double)(__longArrayAttribute[__longArrayAttribute.length - 1]));
//			  } //else if
//			  else if (_object instanceof int[]) {
//				  
//				  int[] __intArrayAttribute = (int[])_object;
//				  return((double)(__intArrayAttribute[__intArrayAttribute.length - 1]));
//			  } //else if
//			  else {
//				  
//				  Agent.LOG.fine("[" + Constantz.EXTENSION_LOG_STRING + "] Problem interrogating mbean attribute for Composite bean of type: " + _object.getClass() + ".");
//				  return(-234.3156d); //invalid type returns mercury triple point temp in k as negative
//			  } //else
//		}
//		catch(java.lang.Exception _e) {
//			
//			Agent.LOG.fine("[" + Constantz.EXTENSION_LOG_STRING + "] Problem interrogating mbean attribute for Composite bean of type: " + _object.getClass() + ". Object value: " + _object.toString() );
//			return(-83.8058d); //invalid type returns argon triple point temp in k as negative
//		} //catch
//
//	} //handleCompositeDataObject
//	
} //MBeanProcessorHelper
