#!/usr/bin/env bash
#set -e

echo "Step 1. Stopping kafka-connector if it is running."
read -p "Press Enter to continue."
kill -9 `jps -v | grep "kafka-connector" | cut -d " " -f 1`

echo "Step 2. Undeploying space 'demo'."
read -p "Press Enter to continue."
$GS_HOME/bin/gs.sh pu undeploy demo

echo "Step 3. Restarting Debezium to initiate full refresh."
read -p "Press Enter to continue."
curl -X DELETE http://localhost:8083/connectors/debezium-connector
docker compose stop connect
./delete-debezium-topics.sh
docker compose start connect

echo "Waiting for Kafka Connect REST API to be ready."
./wait-for-connect-rest-api.sh

./register-debezium-connector.sh

echo "Step 4. Deploying new empty space 'demo'."
read -p "Press Enter to continue."
$GS_HOME/bin/gs.sh space deploy demo

echo "Step 5. Updating connector schema definitions."
read -p "Press Enter to continue."
./run-learning-sqlserver.sh

echo `basename "$0"` > last_step.txt

echo "Step 6. Final step. Starting connector."
read -p "Press Enter to continue."
./2-start-connector.sh

