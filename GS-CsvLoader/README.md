# GS csv loader

## 1. Create a pojo based on the csv header

1. edit generate-pojo-from-csv-header.sh and set the following to fit your needs:<br>
   `export PACKAGE="com.gs.csm.data"`<br>
   `export POJO_OUTPUT_DIRECTORY="src/main/java"`<br>
   `export POJO_NAME="StocksHistoriesPojo"`<br>
   `export CSV_FILE="src/main/resources/fh_5yrs_with_id.csv"`<br>
   
2. `./generate-pojo-from-csv-header.sh`

3. The new pojo will be created in the POJO_OUTPUT_DIRECTORY.

4. Please add the @SpaceId annotation above the getId() method

**Note:**<br>
The pojo will be created with default String properties for other types (Date,Int...), you can modify the new pojo and change properties types for your convenient. 

## 2. Load the csv data to Space

1. edit import-csv-to-gs.sh and set the following to fit your needs:<br>
   `export GS_LOOKUP_GROUPS=15.5.0`<br>
   `export GS_LOOKUP_LOCATORS=localhost`<br>
   `export GS_SPACE_NAME=demo`<br>
   `export CSV_FILE=src/main/resources/fh_5yrs_with_id.csv`<br>
   `export CSV_POJO="com.gs.csm.data.StocksHistoriesPojo"`<br>
   `export LIMIT_ROWS=100000;`<br>
   `export NUM_OF_ITERATIONS=1;`<br>
    
2. `./import-csv-to-gs.sh`

3. Verify in the Ops Manager that all records were loaded to Space:<br>
   http://localhost:8090
   
## 3. Load the csv data to Space using a stateless pu.

1. edit pu.xml and and set the following to fit your needs:<br>
   `<prop key="numberOfIterations">1</prop>`<br>
   `<prop key="csvFileName">fh_5yrs_with_id.csv</prop>`<br>
   `<prop key="csvPojo">com.gs.csm.data.StocksHistoriesPojo</prop>`<br>
   `<prop key="SpaceName">demo</prop>`<br>
   
**Note:**<br>
The above properties can be set using gs cli during the deployment phase as well.<br>
(see an example in import-csv-to-gs-from-pu.sh script).<br>
      
2. ./import-csv-to-gs-from-pu.sh

3. Verify in the Ops Manager that all records were loaded to Space:<br>
   http://localhost:8090

