#!/bin/bash
plList=$(curl -sX 'GET'   'http://localhost:6080/api/v1/pipeline/'   -H 'accept: */*' | jq -r '.[].pipelineId')
for i in ${plList[@]};do
	echo "Request: Stop pipelineID: $i ..."
	echo "Response: $(curl -sX 'POST' \
  'http://localhost:6080/api/v1/pipeline/'$i'/stop' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '' | jq -r '.status')"

  plMessage=$(curl -sX 'GET'   'http://localhost:6080/api/v1/pipeline/' | jq -r '.[] | "[\(.name)] \(.pipelineId) \(.message)"' |grep $i  |sed 's/\] .* Subscription/] Subscription/') 
  echo $plMessage
  echo
done
echo -e "Sleeping 10s before checking pipeline status." ; sleep 10
/giga/scripts/killAllFlinkJobs.sh
/giga/scripts/statusPipelines.sh
# Return 0 if all pipelines are stopped.
[[ $(curl -sX 'GET'   'http://localhost:6080/api/v1/pipeline/' | jq -r '.[] | "\(.name): \(.message)"' |grep 'Refresh\|Mirror\|RUNNING' |wc -l) -eq 0 ]] && exit 0 || exit 1
