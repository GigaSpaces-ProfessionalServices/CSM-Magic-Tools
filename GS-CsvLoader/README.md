# GS csv loader

## 1. Create a pojo based on the csv header

1. edit generate-pojo-from-csv-header.sh and set the following to fit your needs:<br>
   `export PACKAGE="com.gs.csm.data"`
   `export POJO_OUTPUT_DIRECTORY="src/main/java"`
   `export POJO_NAME="myNewPojo"`
   `export CSV_FILE="src/main/resources/ibm.us.csv"`
   
2. `./generate-pojo-from-csv-header.sh`

3. the new pojo will be created in the POJO_OUTPUT_DIRECTORY.

**Note:**<br>
The pojo will be created with default String properties for other types (Date,Int...), please modify the new pojo and change properties types 

## 2. Load the csv data to Space

1. edit import-csv-to-gs.sh and set the following to fit your needs:<br>
   `export GS_LOOKUP_GROUPS=15.5.0`<br>
   `export GS_LOOKUP_LOCATORS=localhost`<br>
   `export GS_SPACE_NAME=demo`<br>
   `export CSV_FILE=src/main/resources/ibm.us.csv`
    
2. `./import-csv-to-gs.sh`

3. Verify in the Ops Manager that all records were loaded to Space:<br>
   http://localhost:8090


