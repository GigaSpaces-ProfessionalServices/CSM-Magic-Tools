# Kafka-Gigaspaces connector

This connector supports two modes of operation
* Learning mode
* Connector mode

# Learning mode

In this mode connector reads sample data messages from Kafka (other methods supported as well) and generates
a `data-pipeline.yml` file, which includes space types definitions that correspond to given data model, 
with mapping between space types and their properties to the source JSON messages, using JSONPath.
For CDC it also generates CDC rules. This mode can be extended with plugins.
After running the connector in learning mode, user reviews created space types definitions, 
marks relevant properties with `spaceid` and `routingkey` attributes, adds indexes etc, and proceeds to the next step.

This mode is configured under `learning` profile in `application.yml` file.

# Connector mode

In this mode connector creates space documents based on space types definitions in `data-pipeline.yml` file and starts receiving data from Kafka for these types.

# Features

(all features listed below are POC-grade, demoable, but not necessarily GA ready)
* CDC with Debezium
* CDC with HVR
* Configurable conflicts handing in CDC (INSERT ifExists or UPDATE ifNotExists scenarios)
* Schema-less JSON with support for complex data structures such as lists and nested objects
* Source types can be mapped to Gigaspaces types via configuration before learning phase
* Mapping one Kafka message to multiple space types
* Type per topic and multiple types in the same topic
* Can be deployed as PU or run as standalone Spring boot application

# How to build and run

The project contains folder 'examples' which include build and run commands.

# Running the examples
* Start Gigaspaces (not part of this repo)
* Choose example under 'examples' dir, enter the example dir
* Update lookup locator address in the `N-start-connector.sh` script
* Run steps according to their numbers one after another 

# To build in Leumi site
`mvn clean package -DskipTests -P pu  -s E:\.m2\settings.xml`

# TODO
* POJOs support
* Add support for IIDR
* AVRO support
* XML support
* Tiered storage policies definition in data-pipeline.yml file.
* Configurable error handling
  * Print error and continue (current behaviour)
  * Stop reading
  * Dead letter topic
* Global space setting and option to override it in each type
* Compound keys support
* Detecting different date formats
* Nested properties as space id and as index props
* Performance optimizations - accumulate messages and use 'multiple' APIs
  * with an option to set different bulk size for initial load/insert/delete operations
  * error handling with bulk APIs
* Better lists support
  * List as a default value of a property
  * Partial update with nested properties and lists
* Use reflection to find all available Metadata/Parser plugins in Java class path
* Configurable handling of extra fields in Kafka messages which are not part of Space type definition
  * Ignore
  * Save in dynamic properties
  * Terminate
* Re-learning with existing pipeline definitions - preserve user configuration 
