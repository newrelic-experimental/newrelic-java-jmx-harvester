package com.newrelic.labs.jmxharvester;

/**
 * The String or other static constants for this extension.
 * 
 * @author gil@newrelic.com
 */
public class Constantz {

	public static final String EXTENSION_NAME = "JMXHarvester";
	public static final String EXTENSION_LOG_STRING = "[JMXHarvester]";
	public static final String YML_SECTION = "jmx_harvester";	
	public static final String EXTENSION_NAME_MEMORY = "JMXHarvester_Memory";
	public static final String EXTENSION_LOG_STRING_MEMORY = "[JMXHarvester_Memory]";
	public static final String MODE_STRICT = "strict";
	public static final String MODE_OPEN = "open";
	public static final String MODE_DISCO = "disco";
	public static final String MODE_PROMISCUOUS = "promiscuous";
	public static final String MODE_INVENTORY = "inventory";
	public static final String NERDSTORE_COLLECTION = "jmx-harvester";
	public static final String INVENTORY_METRIC_PREFIX = "jmx_harvester.inventory";
	public static final String MBEAN_DOC_TYPE = "mbean";
	public static final String AGENT_CONFIG_DOC_TYPE = "agent_config";	
	public static final String EU_DC = "https://api.eu.newrelic.com/graphql";
	public static final String US_DC = "https://api.newrelic.com/graphql";
	public static final String STAGING_DC = "https://staging-api.newrelic.com/graphql"; //TODO
	//BETA Configuration Options - IMPLMENTATION NOT GUARANTEED 
	public static final String MODE_OPTOUT = "optout";
	public static final String MODE_REGEX = "regex";
	public static final String MODE_REGEX_OPTOUT = "regex_optout";
	
} //Constantz