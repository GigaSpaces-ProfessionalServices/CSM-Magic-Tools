#!/bin/bash
confFile=/giga/di-iidr-watchdog/di_watchdog_rest.conf

[[ -f $confFile ]] && source $confFile || { echo "make sure $confFile does exist." ; exit 1; }

if [[ $1 != "telegraf" ]] ; then
cat << EOF
{
    "timestamp": "$(date)",
    "environment": "${env}",
    "iidrHost": "${iidrHost}",
    "diHost": "${diHost}",
    "pipelines": [
EOF
fi

pipelineId=($(curl -sX 'GET' 'http://'${diHost}':6080/api/v1/pipeline/'   -H 'accept: */*' | jq -r '.[].pipelineId'))
for pl in ${pipelineId[@]}
do
   plName=$(curl -sX 'GET'   'http://'${diHost}':6080/api/v1/pipeline/'${pl}''   -H 'accept: */*' |jq -r '.name')
   subName=$(curl -sX 'GET'   'http://'${diHost}':6080/api/v1/pipeline/'${pl}''   -H 'accept: */*' |jq -r '.subscriptionName')
   subStatus=$(curl -sX 'GET'   'http://'${iidrHost}':6082/api/v1/ORACLE/subscriptions/'$subName'/status' -H 'accept: */*' |jq -r .'state')
   plStatus=$(curl -sX 'GET'   'http://'${diHost}':6080/api/v1/pipeline/'${pl}''/status   -H 'accept: */*' |jq -r .'jobs'.'CDC'.'status')

   if [[ (${plStatus^^} == "RUNNING") && (${subStatus^^} == "REFRESH BEFORE MIRROR" || ${subStatus^^} == "MIRROR_CONTINUOUS") ]];then
      pl_state_code="UP" # The pipeline is active
   else
      pl_state_code="DOWN" # The pipeline is inactive/failed
   fi

   if [[ $1 != "telegraf" ]] ; then
cat << EOF
        {
            "name": "${plName}",
            "state": "${pl_state_code}"
EOF
     [[ "${pipelineId[-1]}" != "${pl}" ]] && echo "        }," || echo "        }"
   else
     echo "plState,pl=${plName} State=\"${pl_state_code}\""
   fi
done

if [[ $1 != "telegraf" ]] ; then
cat << EOF
    ]
}
EOF
fi
