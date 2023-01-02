#!/usr/bin/env bash
#set -e

source ~/.bash_gigaspaces

echo "Deployed PUs"
${GS_HOME}/bin/gs.sh pu list
echo "--------------"

echo "Standalone PUs"
jps | grep ProcessingUnit
echo "--------------"

echo "Spaces:"
curl -s http://localhost:8090/v2/spaces | jq '.'| grep name
echo "--------------"

echo "Types:"
gtypes demo
echo "--------------"

echo "Running connectors:"
jps -v | grep kafka-connector
echo "--------------"

echo "Debezium status"
./scripts/get-debezium-connector-status.sh
echo "--------------"

