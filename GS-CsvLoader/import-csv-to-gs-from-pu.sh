#!/bin/bash

export GS_HOME="/Users/aharonmoll/XAPBuilds/gigaspaces-insightedge-enterprise-15.5.0"

./build.sh
$GS_HOME/bin/gs.sh pu deploy --property=numberOfIterations=1 --property=csvFileName=fh_5yrs_with_id.csv --property=csvPojo=com.gs.csm.data.StocksHistoriesPojo --property=limitRows=100000 --property=SpaceName=demo CSVFeeder /Users/aharonmoll/CSM-Magic-Tools/GS-CsvLoader/target/csvLoader-1.0-SNAPSHOT.jar