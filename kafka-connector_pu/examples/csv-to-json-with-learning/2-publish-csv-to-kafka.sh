#!/usr/bin/env bash
set -e

mlr --icsv --ojson cat data/sample.csv | docker exec -i broker kafka-console-producer --broker-list broker:29092 --topic sample -
