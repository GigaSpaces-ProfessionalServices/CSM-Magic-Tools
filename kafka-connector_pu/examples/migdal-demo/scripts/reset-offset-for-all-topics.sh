#!/usr/bin/env bash
set -e

group=DIH

while IFS= read -r topic; do
  echo ${topic}
    docker exec kafka kafka-consumer-groups --bootstrap-server kafka:29092 --group ${group} \
      --reset-offsets --to-earliest --topic ${topic} --execute
    echo "Offset reset for consumer group ${group} topic ${topic}"
done <<< $(./list-topics-filtered.sh)

