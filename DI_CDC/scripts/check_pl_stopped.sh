#!/bin/bash
wait_for_pl_stopped ()
{
    PL=82d8bbbb-81a9-4f55-a9e2-9fa7e9e34f77
    while [[ $(curl -sX GET http://gstest-di1:6080/api/v1/pipeline/82d8bbbb-81a9-4f55-a9e2-9fa7e9e34f77/status -H 'accept: */*' |jq -r '.message' |grep "Inactive.*STOPPED" |wc -l) -eq 0 ]]
    do 
        sleep 5
    done
}

timeout .1 bash -c wait_for_pl_stopped || echo "PL did not stop within 60s."