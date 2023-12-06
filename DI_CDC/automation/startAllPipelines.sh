#!/bin/bash
plList=$(curl -sX 'GET'   'http://localhost:6080/api/v1/pipeline/'   -H 'accept: */*')
numOfPl=$(curl -sX 'GET'   'http://localhost:6080/api/v1/pipeline/'   -H 'accept: */*'  | jq -r '.[].pipelineId' |wc -w)
for pl in $(echo "${plList}" | jq -r '.[] | @base64'); do
    _jq() {
     echo ${pl} | base64 --decode | jq -r ${1}
    }
    pipelineId=$(_jq '.pipelineId')
    pipelineName=$(_jq '.name')
    echo "[$pipelineName]"
    echo "Request: Start pipeline"
	plState=$(curl -sX 'POST' \
        'http://localhost:6080/api/v1/pipeline/'$pipelineId'/start' \
        -H 'accept: */*' \
        -H 'Content-Type: application/json' \
        -d '{
            "reconciliationPolicy": "NONE",
            "kafkaRunParameters": {
                "CDC": {
                    "kafkaOffsetStrategy": "COMMITTED",
                    "kafkaOffset": -1
                }
            }
        }'|jq -r '.status')
        echo "Response: $plState"
        plMessage=$(curl -sX 'GET'   'http://localhost:6080/api/v1/pipeline/' | jq -r '.[] | "[\(.name)] \(.message)"' |grep "Subscription status is Mirror Continuous. Job of type CDC has status RUNNING" |grep $pipelineName)
        echo $plMessage
        echo

    [[ $(echo $plState |grep -v '^ *$\|SUCCESS\|JOBS_ALREADY_RUNNING\|Starting Pipeline' |wc -l) -gt 0 ]] && exit 1
done

numOfPl=$(curl -sX 'GET'   'http://localhost:6080/api/v1/pipeline/' | jq -r '.[] | "\(.name) \(.message)"' |wc -l)
for ((i=0;i<3;i++));do
   plState=$(curl -sX 'GET'   'http://localhost:6080/api/v1/pipeline/' | jq -r '.[] | "\(.name) \(.message)"' |grep "Subscription status is Mirror Continuous. Job of type CDC has status RUNNING" |wc -l) 
   echo "$plState/$numOfPl subscriptions and pipilines are running".
   
  [[ $plState -eq $numOfPl ]] && { exit 0; } || { exit 1; }
  
  
done

