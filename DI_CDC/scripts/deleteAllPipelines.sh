#!/bin/bash
read -p "You are about to delete all the pipelines. Are you sure? ('YES' to confirm, any to abort.) " del
[[ $del == "YES" ]] || echo "Aborted."; exit
exit
 plList=$(curl -sX 'GET'   'http://localhost:6080/api/v1/pipeline/'   -H 'accept: */*' |jq -r '.[].pipelineId')
for i in ${plList[@]};do
	echo "Deleting pipelineID: $i ..."
        curl -X 'DELETE' \
  'http://'localhost:6080'/api/v1/pipeline/'$i'?deleteSubscription=true' \
  -H 'accept: */*'
done
