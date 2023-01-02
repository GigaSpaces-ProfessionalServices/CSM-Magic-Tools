#!/usr/bin/env bash
set -e

mlr --icsv --ojson cat prices.csv | docker exec -i kafka kafka-console-producer --broker-list broker:29092 --topic prices -
