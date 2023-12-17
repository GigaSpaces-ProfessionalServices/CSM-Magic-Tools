#!/bin/bash
confFile=./di_watchdog_rest.conf

[[ -f $confFile ]] && source $confFile || { echo "make sure $confFile does exist." ; exit 1; }

# remove log files older than $logRetention
find /gigalogs/di-iidr-watchdog/ -mtime $logRetention -name '*.log' -delete

echo 
date;
echo -----------------------------------------------------------------------------
echo

## check kafka and zookeeper systemctl services
echo "Checking Kafka and Zookeeper Services"
echo "-------------------------------------"
for kafkaHost in ${kafkaHosts[@]};do
    echo "[$kafkaHost]:"
    echo "---------------------"
    for service in ${kafkaServices[@]};do
        status="ssh ${kafkaHost} 'systemctl is-active --quiet ${service}'"
        eval $status
            if [[ $? -ne 0 ]];then
                echo "${service} is not active, trying to start the service ..."
                start="ssh ${kafkaHost} 'systemctl restart ${service}'"
                eval $start
            else
                echo "${service} is active."
            fi
            
    done
echo
done

## check di systemctl services
echo "Checking DI [$diHost] Services ..."
echo "----------------------------------"
for service in ${diServices[@]};do
    status="ssh ${diHost} 'systemctl is-active --quiet ${service}'"
    eval $status
        if [[ $? -ne 0 ]];then
            echo "${service} is not active, trying to start the service ..."
            start="ssh ${diHost} 'systemctl restart ${service}'"
            eval $start
        else
            echo "${service} is active."
        fi
        
done

echo
## check iidr systemctl services
echo "Checking IIDR [$iidrHost] Services ..."
echo "----------------------------------"
for service in ${iidrServices[@]};do
    status="ssh ${iidrHost} 'systemctl is-active --quiet ${service}'"
    eval $status
        if [[ $? -ne 0 ]];then
            echo "${service} is not active, trying to start the service ..."
            start="ssh ${iidrHost} 'systemctl restart ${service}'"
            eval $start
        else
            echo "${service} is active."
        fi
done

echo
## check iidr Oracle Agent systemctl services
echo "Checking IIDR Oracle Agent [$iidrOracleAgentHost] Services ..."
echo "----------------------------------"
for service in ${iidrOracleAgentService[@]};do
    status="ssh ${iidrOracleAgentHost} 'systemctl is-active --quiet ${service}'"
    eval $status
        if [[ $? -ne 0 ]];then
            echo "${service} is not active, trying to start the service ..."
            start="ssh ${iidrOracleAgentHost} 'systemctl restart ${service}'"
            eval $start
        else
            echo "${service} is active."
        fi
done

echo
## Check DI components availabilty
echo "Check DI components availabilty via rest api ..."
echo "------------------------------------------------"
for ((i=1; i<=6; i++));do
di_mdm_state=$(curl -sX 'GET' 'http://'$diMdmRest'/api/v1/about' -H 'accept: */*' |grep DI-MDM |wc -l)
di_manager_state=$(curl -sX 'GET' 'http://'$diManagerRest'/api/v1/about' -H 'accept: */*' |grep DI-Manager |wc -l)
di_sub_manager_state=$(curl -sX 'GET' 'http://'$diSubManagerRest'/api/v1/about' -H 'accept: */*' |grep DI-Subscription-Manager |wc -l)
di_flink=$(curl -vs "http://$diFlinkRest/" 2>&1 |grep "HTTP/1.1 200 OK" |wc -l)

#echo $di_mdm_state $di_manager_state $di_sub_manager_state $di_flink

if [[ $di_mdm_state -eq 1 && $di_manager_state -eq 1 && $di_sub_manager_state -eq 1 && $di_flink -eq 1 ]];then
    echo "### All DI components are available. ###";echo
    success=0
    break
else
    echo "Wainting for DI componets to be available ..."
    success=1
    sleep 15 

fi
done
if [[ $success -ne 0 ]];then
    echo "!!! DI compoenets have failed to start. !!!";echo
    exit 1
fi

## check DI pipelines
echo
echo "Checking DI pipelines ..."
echo "----------------------------------"
ssh $diHost 'su - gsods -c /dbagiga/scripts/statusPipelines.sh'
if [[ $? -ne 0 ]];then
    echo "Starting all pipelines ..."
    ssh $diHost 'su - gsods -c /dbagiga/scripts/stopAllPipelines.sh'
    sleep 10
    exit_code=$?; echo $exit_codef
    [[ $? -ne 0 ]] && { echo "Pipelines failed to stop. exit." ; exit 1; }
    ssh $diHost 'su - gsods -c /dbagiga/scripts/startAllPipelines.sh'
fi

