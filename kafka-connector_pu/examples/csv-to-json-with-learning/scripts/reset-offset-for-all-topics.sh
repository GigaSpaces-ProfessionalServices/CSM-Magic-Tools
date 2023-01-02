#!/usr/bin/env bash
set -e


docker exec broker kafka-consumer-groups --bootstrap-server kafka:29092 --group DIH \
  --reset-offsets --to-earliest --topic sample --execute

docker exec broker kafka-consumer-groups --bootstrap-server kafka:29092 --group DIH-learning \
  --reset-offsets --to-earliest --topic sample --execute
