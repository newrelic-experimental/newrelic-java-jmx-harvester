/**
 * This module encapsulates a New Relic Java Agent extension for collecting JMX telemetry.
 * <p>
 * JMX telemetry is harvested through direct collection from MBeans within the runtime 
 * instance of the JVM. The extension permits the configuration of data collection and 
 * polling frequency through extension to newrelic.yml agent configuration options or
 * as defined by the New Relic One JMX Application. 
 * </p>
 * 
 * @since 1.0
 * @author gil
 * @version 1.1
 *
 */
package com.newrelic.labs.jmxxx;