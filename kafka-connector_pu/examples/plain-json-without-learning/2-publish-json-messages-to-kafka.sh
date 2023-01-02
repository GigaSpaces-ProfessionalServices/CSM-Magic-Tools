#!/usr/bin/env bash
set -e

cat data/person.txt | docker exec -i broker kafka-console-producer --broker-list broker:29092 --topic persons -
