# GS csv loader

## 1. Create a pojo based on the csv header

1. `cd csvLoader`
2. `mvn exec:java  -Dexec.mainClass=com.gs.csm.CreatePojoFromCsvHeader -Dexec.args="src/main/resources/ibm.us.csv myNewPojo"`
3. Based on the csv header a new pojo will be created under the package:<br>
   **com.gs.csm.data**

* The above is an example on how to create a pojo based on your csv header<br>
  Please modify args1 and args2 for you specific needs and run again.<br>
  
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


