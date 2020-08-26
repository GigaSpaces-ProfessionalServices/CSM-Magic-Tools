#!/bin/bash
export GS_LOOKUP_GROUPS="15.5.0"
export GS_LOOKUP_LOCATORS="localhost"
export GS_SPACE_NAME="demo"
export CSV_FILE="src/main/resources/ibm.us.csv"
export CSV_POJO="com.gs.csm.data.MyNewPojo"
export NUM_OF_ITERATIONS=1;

./build.sh
mvn exec:java  -Dexec.mainClass=com.gs.csm.LoadCSVData -Dexec.classpathScope=compile -Dexec.args=$GS_SPACE_NAME

