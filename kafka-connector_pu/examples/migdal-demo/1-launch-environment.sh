#!/usr/bin/env bash

echo "Step 1. Launching Kafka with Kafka Connect and SQL server."
read -p "Press Enter to continue."
./launch-docker-compose.sh

echo "Step 2. Redeploying 'demo' space to make sure we start with empty DIH."
read -p "Press Enter to continue."
kill -9 `jps -v | grep "kafka-connector" | cut -d " " -f 1`
$GS_HOME/bin/gs.sh pu undeploy demo --drain-mode=NONE
$GS_HOME/bin/gs.sh space deploy demo

echo "Step 3. Creating SQL server database."
read -p "Press Enter to continue."
./init-sql-server.sh

echo "Step 4. Creating Debezium connector in Kafka."
read -p "Press Enter to continue."
./register-debezium-connector.sh

echo "Step 5. Updating DIH connector schema definitions."
read -p "Press Enter to continue."
./run-learning-sqlserver.sh

echo `basename "$0"` > last_step.txt
echo "Done. Connector can be started now."
