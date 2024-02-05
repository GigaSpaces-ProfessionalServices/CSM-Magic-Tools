# COMMON - Distributed Task to Calculate Max value

### Run Example Application

1. Do maven build to generate jar file
> cd common
 
> mvn clean install 
   
2. Run jar file with arguments

#### Note: Run from the machine from where connection to space is allowed using space proxy.

Syntax: 
> java -jar target/common-0.0.1-SNAPSHOT-jar-with-dependencies.jar <space_name> <lookup_locator> <lookup_group> <data_type> <table_name> <column_name>

Example: 

> java -jar target/common-0.0.1-SNAPSHOT-jar-with-dependencies.jar dih-tau-space localhost xap-16.4.0 Long T_Long T_IDKUN
    
Here Set table name and column name for which max value to be calculated

Options for data_type are ['Long' , 'LocalDateTime']
