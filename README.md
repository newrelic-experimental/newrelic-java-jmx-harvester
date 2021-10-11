[![New Relic Experimental header](https://github.com/newrelic/opensource-website/raw/master/src/images/categories/Experimental.png)](https://opensource.newrelic.com/oss-category/#new-relic-experimental)

# [JMX2Insights] 

README for JMX2Insights :: Version 3.5.1 :: September 2020
===================================================

WHAT IT BE
==========
- JMX2Insights is a New Relic Java Agent extension that provides easy access to MBeans and attribute values by reporting them as custom event data to New Relic Insights.

HOW TO DEPLOY
==========
- Copy the jmx2insights_n.n.jar file to your Java Agent's ./newrelic/extensions/ directory. Ensure the directory and extension have the same owner as all other New Relic agent files. Create the extensions directory if doesn't exist.
- Edit the newrelic.yml file found in your Agent's root directory and include a jmx2insights section like below. Be sure to get the spacing right, 2 spaces before "jmx2insights" and four spaces before each property. 
- Place the jmx2insights section directly after the proxy section in newrelic.yml (You can place it anywhere, but I place it after proxy config).

 --> See Readme.txt for example blurb or jmx2insights.yml
	
HOW TO CONFIGURE
================
- There are a few options you can configure to change the behavior of JMX2Insights, see below. 
- The service is aware of changes to the newrelic.yml file, so changes will manifest a few minutes after newrelic.yml has been updated, this will depend on the frequency you have set for polling.

	Options
	=======
	- enabled: Either true or false, disables or enables the harvest of mbeans accordingly.
	- mode: One of the following options  (default is "strict").
		- strict: The mbeans listed in the jmx2insights mbean_N section and their attributes are polled.
		- open: All the attributes of the mbeans listed in the jmx2insights mbean_N section are polled. There is no need to define specific attribute values for the mbeans listed.
		- promiscuous: All accessible MBeans and Attributes are polled.
		- disco: Details of all MBeans are written to the newrelic agent logfile with "INFO" severity. This is a good way to discover the mbeans and attributes you wish to poll.   
	- event_name: Determines the custom event name you wish to store the JMX data within Insights (default is JMX).
	- frequency: The number of minutes between runs of the JMX harvest (default and lowest granularity is 1 minute). The service does not have a sleep function it simply skips the number of polling intervals defined herein.
	- memory_events: (on | off) collects second by second memory details for jvm - generates 86400 events per day 
	- mbean_N: There can be multiple "mbean" options defined in the jmx2insights section, this forms the list of defined MBean polled in strict or open mode.
		- The mbean property is defined in 2 parts, the name of the mbean followed by attributes you wish to poll, comma separated and enclosed in square brackets.
		- e.g. mbean_0: com.mypackage:type=ImportantData [Attribute0,Attribute1,Attribute2].
		- if you wish to use the open mode you must include square brackets within the definition line (e.g. mbean_0: com.mypackage:type=ImportantData [IgnoreMe])
		- if you want to define a unique polling interval for an mbean append a number to the mben definition after the final square bracket "]" (e.g. com.mypackage:type=ImportantData [Attribute0,Attribute1,Attribute2]5) - this works in strict and open configurations

HOW TO USE
==========
- Once the extension is deployed and configured you should start seeing events in the custom event table you defined for Insights. Go about creating dashboards using this new data source. 
- To configure the data elements you may want to override the default formatting New Relic Insights is asserting on the values. To do this select the "Formatter" tab in the "Manage Data" section of your Insights account.

KNOWN LIMITATIONS
=================
- Only reports Attributes from MBeans that return Objects of type: java.lang.Number, java.lang.String, or java.lang.Boolean, javax.management.openmbean.CompositeData, javax.management.openmbean.TabularData, java.lang.String[], java.util.Date. Provide feedback to the author if you would like other datatype processing options.
- Only reports MBeans registered to the default platform MBean server.
- Only polls Attributes not Operations.

TODOS
=====
- Provide an ability to alias MBean names.
- Process string values to escape special characters properly.
- Curate a list of useful MBean definitions for popular platforms and frameworks.

CHANGES PER RELEASE
===================
v3.5.1
- Events will now include any labels configured in the Java agent YML
- Added retry logic when registering with the Harvest Service if the agent takes longer than 60 seconds to start.

v3.5
- Added MemoryPool (java.lang:type=MemoryPool,name=*) Usage/PeakUsage MBean metrics to Memory event.
- Added GarbageCollector (java.lang:type=GarbageCollector,name=*) CollectionCount/CollectionTime - and calculate observation period deltas for these metrics.

v3.14
- Created a Memory Event thread that frequently polls memory related MBean data and saves events to "JMX_MEMORY" event table.

v3.0.1
- Included support for variable interval timing of each MBean definition. Just place a number after the MBean definition - that number multiplied by the jmx2insights configuration will be the interval for a given MBean reporting. 
-- mbean_6: java.lang:type=Runtime [Uptime]15 ... this will record the uptime metric every 15 minutes of the default frequency is set at 1 

v2.7
- Support for the tabular or composite data fields with new mbean definition syntax: "mbean_50: org.apache.jackrabbit.oak:type=RepositoryStats,name=Oak Repository Statistics [ObservationQueueMaxLength(per minute|per hour),SessionWriteCount(per second|per minute|per hour),SessionReadAverage(per second),SessionReadDuration(per second),SessionReadCount(per second)]"

v2.6.3
- Support for numeric arrays contained in CompositeData objects. The system will record the last value in array cardinality.

v2.5
- Added support for complex JMX Objects.

v1.1
- Prepared for official release by cleaning up naming conventions and log output statements.

v0.8
- The MBean definition in the jmx2insights section of the newrelic.yml can include wildcards per the MBean lookup options for https://docs.oracle.com/javase/7/docs/api/javax/management/MBeanServer.html#queryMBeans(javax.management.ObjectName,%20javax.management.QueryExp) - this relies on the ObjectName supporting wildcards per constrains defined by https://docs.oracle.com/javase/7/docs/api/javax/management/ObjectName.html. This would then support an MBean name definition of "*:type=BasicDataSource,connectionpool=connections [NumActive,MaxTotal,MinIdle,MaxIdle,NumIdle]" which would match any MBean domain. The problem is that the yml file format does not like to have attribute values start with "*:" so we have to escape such definitions with a character, in our case '\'. 

v0.7
- Display the real name of the MBean Object rather than the query string used to loop up the MBean - that was confusing.

v0.6
- Compiled to support the Java 1.6 standard (1.6.0_45)


## Support

New Relic has open-sourced this project. This project is provided AS-IS WITHOUT WARRANTY OR DEDICATED SUPPORT. Issues and contributions should be reported to the project here on GitHub.
>We encourage you to bring your experiences and questions to the [Explorers Hub](https://discuss.newrelic.com) where our community members collaborate on solutions and new ideas.


## Contributing

We encourage your contributions to improve [JMX2Insights]! Keep in mind when you submit your pull request, you'll need to sign the CLA via the click-through using CLA-Assistant. You only have to sign the CLA one time per project. If you have any questions, or to execute our corporate CLA, required if your contribution is on behalf of a company, please drop us an email at opensource@newrelic.com.

**A note about vulnerabilities**

As noted in our [security policy](../../security/policy), New Relic is committed to the privacy and security of our customers and their data. We believe that providing coordinated disclosure by security researchers and engaging with the security community are important means to achieve our security goals.

If you believe you have found a security vulnerability in this project or any of New Relic's products or websites, we welcome and greatly appreciate you reporting it to New Relic through [HackerOne](https://hackerone.com/newrelic).

## License

[JMX2Insights] is licensed under the [Apache 2.0](http://apache.org/licenses/LICENSE-2.0.txt) License.
