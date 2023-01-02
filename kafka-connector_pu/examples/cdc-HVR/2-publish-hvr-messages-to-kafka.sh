#!/usr/bin/env bash
set -e

cat data/HVR-Customers.txt | docker exec -i broker kafka-console-producer --broker-list broker:29092 --topic customers -
cat data/HVR-Cars.txt | docker exec -i broker kafka-console-producer --broker-list broker:29092 --topic cars -
