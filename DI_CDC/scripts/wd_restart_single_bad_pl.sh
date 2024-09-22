#!/bin/bash
## Functions
wait_for_pl_stopped ()
{
    PL=$1
    while [[ $(curl -sX GET http://localhost:6080/api/v1/pipeline/${PL}/status -H 'accept: */*' |jq -r '.message' |grep "Inactive.*STOPPED" |wc -l) -eq 0 ]]
    do 
        sleep 5
    done
}
######

pipelineId=$(curl -sX 'GET' 'http://localhost:6080/api/v1/pipeline/'   -H 'accept: */*' | jq -r '.[].pipelineId')
for pl in ${pipelineId[@]}
do 
   plName=$(curl -sX 'GET'   'http://localhost:6080/api/v1/pipeline/'${pl}''   -H 'accept: */*' |jq -r '.name')
   subName=$(curl -sX 'GET'   'http://localhost:6080/api/v1/pipeline/'${pl}''   -H 'accept: */*' |jq -r '.subscriptionName')
   plTables=$(curl -sX 'GET'  'http://localhost:6080/api/v1/pipeline/'${pl}'/tablepipeline' -H 'accept: */*' |jq -r '.[].spaceTypeName' )
   subStatus=$(curl -sX 'GET'   'http://gstest-iidr1.tau.ac.il:6082/api/v1/ORACLE/subscriptions/'$subName'/status' -H 'accept: */*' |jq -r .'state')
   plStatus=$(curl -sX 'GET'   'http://localhost:6080/api/v1/pipeline/'${pl}''/status   -H 'accept: */*' |jq -r .'jobs'.'CDC'.'status')
   
   echo "Pipeline: ${plName}"
   echo "Subscription: $subStatus"
   echo "DI JOB: ${plStatus}"
   echo
   
   # If the PL state is not JOB=RUNNING/RESTARTING then STOP the PL, Verify PL is STOPPED and START the PL
   if [[ (${plStatus^^} == "RUNNING") && (${subStatus^^} == "REFRESH BEFORE MIRROR" || ${subStatus^^} == "MIRROR_CONTINUOUS" || ${subStatus^^} == "RESTARTING") ]];then 
      echo "PL ${plName} is running."
      
   else
      echo "PL ${plName} will be restarted ..."
      # restart pl command
      curl -sX 'POST' "http://localhost:6080/api/v1/pipeline/${pl}/stop?stopSubscription=true"  -H 'accept: */*' -d '' |jq
      timeout 60 bash -c "wait_for_pl_stopped ${pl}" || { echo "PL did not stop within 60s."; exit 1; }
      
      echo "Starting the PL ..."
      curl -sX 'POST' \
        'http://localhost:6080/api/v1/pipeline/'${pl}'/start' \
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
        }' |jq

   fi
   echo "----------------------------"
done