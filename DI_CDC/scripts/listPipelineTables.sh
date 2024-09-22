#!/bin/bash
pipelineId=$(curl -sX 'GET' 'http://localhost:6080/api/v1/pipeline/'   -H 'accept: */*' | jq -r '.[].pipelineId')
for pl in ${pipelineId[@]}
do 
   plName=$(curl -sX 'GET'   'http://localhost:6080/api/v1/pipeline/'$pl''   -H 'accept: */*' |jq -r '.name')
   subName=$(curl -sX 'GET'   'http://localhost:6080/api/v1/pipeline/'$pl''   -H 'accept: */*' |jq -r '.subscriptionName')
   plTables=$(curl -sX 'GET'  'http://localhost:6080/api/v1/pipeline/'$pl'/tablepipeline' -H 'accept: */*' |jq -r '.[].spaceTypeName' )
   
   echo "---------------------------------------------------------" 
   echo "| $plName | $subName | $pl  |"
   echo "---------------------------------------------------------" 
   for table in ${plTables[@]}
   do
     echo $table
   done
   echo "---------------------------------------------------------" 
   echo
   echo

done