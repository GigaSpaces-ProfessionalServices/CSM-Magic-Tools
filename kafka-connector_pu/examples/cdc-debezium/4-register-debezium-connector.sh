#!/usr/bin/env bash
set -e

# Start debezium connector for sql server
curl -i -X POST http://localhost:8083/connectors/ -H "Content-Type: application/json" -d '
{
    "name": "debezium-connector",
    "config": {
        "connector.class" : "io.debezium.connector.sqlserver.SqlServerConnector",
        "tasks.max" : "1",
        "database.server.name" : "server1",
        "database.hostname" : "sqlserver",
        "database.port" : "1433",
        "database.user" : "sa",
        "database.password" : "Password!",
        "database.dbname" : "testDB",
        "database.history.kafka.bootstrap.servers" : "kafka:29092",
        "database.history.kafka.topic": "schema-changes.inventory"
    }
}'

# waiting for DDL topic to be created before we can move to the next step

DEBEZIUM_DDL_TOPIC=server1
ALL_TOPICS=$(curl -s http://localhost:8082/topics)

printf "\n\n"
echo "Waiting for Debezium connector to create DDL topic in Kafka."
while [[ "$ALL_TOPICS" != *"$DEBEZIUM_DDL_TOPIC"* ]]; do
  ALL_TOPICS=$(curl -s http://localhost:8082/topics)
  echo $(date) " Topic ${DEBEZIUM_DDL_TOPIC} has not been created yet... "
  sleep 3
done
echo "Topic ${DEBEZIUM_DDL_TOPIC} has been created!"

printf "\n"
echo "Ready for the next step"
