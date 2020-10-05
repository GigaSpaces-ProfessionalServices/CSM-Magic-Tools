#!/bin/bash
export PACKAGE="com.gs.csm.data"
export POJO_OUTPUT_DIRECTORY="src/main/java"
export POJO_NAME="StocksHistoriesPojo"
export CSV_FILE="src/main/resources/fh_5yrs_with_id.csv"

./build.sh
mvn exec:java  -Dexec.mainClass=com.gs.csm.CreatePojoFromCsvHeader -Dexec.args=$POJO_NAME