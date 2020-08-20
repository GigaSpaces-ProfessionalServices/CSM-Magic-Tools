#!/bin/bash
export PACKAGE="com.gs.csm.data"
export POJO_OUTPUT_DIRECTORY="src/main/java"
export POJO_NAME="MyNewPojo"
export CSV_FILE="src/main/resources/ibm.us.csv"

mvn exec:java  -Dexec.mainClass=com.gs.csm.CreatePojoFromCsvHeader -Dexec.args=$POJO_NAME