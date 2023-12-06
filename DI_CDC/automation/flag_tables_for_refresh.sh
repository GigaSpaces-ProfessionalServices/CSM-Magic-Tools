#!/bin/bash
source ./di.env

# Extract all the subscription names inthis environment
subscription=$(curl -sX 'GET' 'http://'$iidrHost':6082/api/v1/'$dataSource'/subscriptions/' -H 'accept: */*' | jq -r '.[].name')

for sub in ${subscription[@]};do
        echo "Subscription: $sub"
        echo "======================="
        echo
# Get mapped tables for a givven subscription
        tables=$(curl -sX 'GET'   'http:/'/$iidrHost':6082/api/v1/'$dataSource'/subscriptions/'$sub'/tables'   -H 'accept: */*' |jq -r '.[].sourceTable' |cut -d'.' -f2)

# Loop over $tables and mark table for refresh
        for table in ${tables[@]};do
                echo "$schema.$table is being flagged for refresh ..."
                output=$(curl -sX 'POST' 'http://'$iidrHost':6082/api/v1/'$dataSource'/subscriptions/'$sub'/refresh' \
                      -H 'accept: */*' -H 'Content-Type: application/json' \
                      -d '{"schema": "'$schema'","table": "'$table'","forceRefresh": true }'|jq -r '.status')
                echo $output
                echo
                [[ "${output}" != "SUCCESS" ]] && exit 1
        done
done


topics=$($kafkaHome/bin/kafka-topics.sh --bootstrap-server $kafkaHost:9092 --list --exclude-internal)

echo "Kafka topics:"
echo "-------------"

for topic in ${topics[@]};do
        echo "$topic"
done

if [[ "${1}" != "deleteTopics" ]];then
   read -p "Would you like to delete the subscription's topics? ('YES' to confirm or any to skip.)" delTopic
   if [[ "${delTopic}" != "YES" ]];then
        echo "Deletion topics skipped."
	exit 
   fi
fi

for sub in ${subscription[@]};do
        echo "Deleting $sub topic ..."
        $kafkaHome/bin/kafka-topics.sh --bootstrap-server $kafkaHost:9092 --delete --if-exists --topic $sub
        echo "Deleting KAFKA-$sub-commitstream topic ..."   
	$kafkaHome/bin/kafka-topics.sh --bootstrap-server $kafkaHost:9092 --delete --if-exists --topic KAFKA-$sub-commitstream
done
exit 0
