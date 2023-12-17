#!/bin/bash
numOfPl=$(curl -sX 'GET'   'http://localhost:6080/api/v1/pipeline/' | jq -r '.[] | "\(.name): \(.message)"' |wc -l)
plState=$(curl -sX 'GET'   'http://localhost:6080/api/v1/pipeline/' | jq -r '.[] | "\(.name): \(.message)"' |grep "Mirror.*RUNNING" |wc -l)
curl -sX 'GET'   'http://localhost:6080/api/v1/pipeline/' | jq -r '.[] | "\(.name): \(.message)"'
echo
if [[ $plState == $numOfPl ]];then
    echo "$plState/$numOfPl pipelines and subscriptions are running."
    exit 0
else
    echo "$plState/$numOfPl pipelines and subscriptions are running."
    exit 1
fi
