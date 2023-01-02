#!/usr/bin/env bash
set -e

docker-compose up -d

# waiting for broker to be ready, by checking whether topic '_confluent-metrics' exists

printf "\n\n"
echo "Waiting for Kafka broker to be ready."
printf "\n\n"
sleep 3

ALL_TOPICS=$(docker exec broker kafka-topics --bootstrap-server localhost:9092 --list)
TOPIC=_confluent-metrics
while [[ "$ALL_TOPICS" != *"$TOPIC"* ]]; do
  ALL_TOPICS=$(docker exec broker kafka-topics --bootstrap-server localhost:9092 --list)
  echo $(date) " Not ready yet... "
  sleep 3
done
echo "Kafka broker is ready!"

printf "\n"
echo "Ready for the next step"
