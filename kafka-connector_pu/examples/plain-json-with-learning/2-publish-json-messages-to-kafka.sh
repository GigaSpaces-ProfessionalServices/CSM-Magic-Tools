#!/usr/bin/env bash
set -e

cat data/person.txt | docker exec -i broker kafka-console-producer --broker-list broker:29092 --topic person -
cat data/bbw-prices.txt | docker exec -i broker kafka-console-producer --broker-list broker:29092 --topic prices -
cat data/bbw-products.txt | docker exec -i broker kafka-console-producer --broker-list broker:29092 --topic products -
cat data/advanced-nesting.txt | docker exec -i broker kafka-console-producer --broker-list broker:29092 --topic advanced -
