#!/usr/bin/env bash
set -e

# launch the environment
export DEBEZIUM_VERSION=1.6
docker-compose up -d

echo "Waiting for Kafka Connect REST API to be ready. May take 1-2 minutes."
./wait-for-connect-rest-api.sh

printf "\n"
echo "Ready for the next step"
