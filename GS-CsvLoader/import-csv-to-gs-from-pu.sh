#!/bin/bash

export GS_HOME="/Users/aharonmoll/XAPBuilds/gigaspaces-insightedge-enterprise-15.5.0"

./build.sh
$GS_HOME/bin/gs.sh pu deploy --property=numberOfIterations=20 CSVFeeder /Users/aharonmoll/CSM-Magic-Tools/GS-CsvLoader/target/csvLoader-1.0-SNAPSHOT.jar
