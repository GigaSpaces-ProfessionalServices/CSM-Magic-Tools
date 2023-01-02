#!/usr/bin/env bash
#set -e

cat sql/drop-column-and-table-cities.sql | docker exec -i sqlserver bash -c '/opt/mssql-tools/bin/sqlcmd -U sa -P $SA_PASSWORD'

echo "Restarting Debezium to make it 'forget' deleted column and table."
read -p "Press Enter to continue."
docker compose stop connect
docker compose start connect

echo "Waiting for Kafka Connect REST API to be ready."
./wait-for-connect-rest-api.sh

echo `basename "$0"` > last_step.txt
printf "\n"
echo "Ready for the next step"
