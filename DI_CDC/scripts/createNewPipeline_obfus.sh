#!/bin/bash
diManagerURL="localhost:6080"
iidrHost="gstest-iidr1.tau.ac.il"

dataSource=ORACLE

plName=$1
spaceName=$2
sourceSchema=$3
sourceTables=$4
routkey=$5

if [[ $# < 4 ]];then
  echo "Please provide all the parameters:"
  echo "./createNewPipeline.sh pipelineName spaceName sourceSchema tableName routkey)"
  echo
  exit
fi
IFS=',' read -ra tables <<< "$sourceTables"

pipelinId=$(curl -sX 'POST' \
  'http://'$diManagerURL'/api/v1/pipeline/' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
  "name": "'$plName'",
  "sorName": "ORACLE",
  "cdcProvider":"IIDR",
  "spaceName":"'$spaceName'",
  "batchWrite":1000,
  "checkpointInterval": 6000
}'| jq -r '.pipelineId')

echo "The Pipeline '$plName' has been created. PipelineID: '$pipelinId'"
echo

for table in ${tables[@]};do
# Add a table to subscription
result=$(curl -sX POST \
    "http://$diManagerURL/api/v1/pipeline/$pipelinId/tablepipeline" \
    -H "accept: */*" \
    -H "Content-Type: application/json" \
    -d '{
      "sourceSchema": "'"$sourceSchema"'",
      "sourceTable": "'"$table"'",
      "spaceTypeName": "'"$sourceSchema.$table"'",
      "store": true,
      "patches": [
        {
          "patch": "routing-key",
          "routingKey": "'"$routkey"'"
        }
      ]
    }'| jq -r '.message')
  
  if [[ $result == "Success" ]];then
    echo ">> $sourceSchema.$table table successfuly added to $plName."
    # Flag table for refresh
    subName=$(curl -sX 'GET'   'http://localhost:6080/api/v1/pipeline/'$pipelinId''   -H 'accept: */*' |jq -r '.subscriptionName')
    echo "schema: $sourceSchema   table:$table   dataSource:$dataSource   subName:$subName"
    echo "Table is being flagged for refresh ..."
      output=$(curl -sX 'POST' \
      'http://'$iidrHost':6082/api/v1/'$dataSource'/subscriptions/'$subName'/refresh' \
      -H 'accept: */*' \
      -H 'Content-Type: application/json' \
      -d '{
      "schema": "'"$sourceSchema"'",
      "table": "'"$table"'",
      "forceRefresh": true
       }'|jq -r '.status')
    [[ "${output}" = "SUCCESS" ]] && echo $output || { echo "Failed to flag for refresh [$table]."; exit 1; }

  else
    echo "$table table could not be added. Skipping ..."
  fi 
  echo ----------------------------------------------------------------
done
echo "Finished."

