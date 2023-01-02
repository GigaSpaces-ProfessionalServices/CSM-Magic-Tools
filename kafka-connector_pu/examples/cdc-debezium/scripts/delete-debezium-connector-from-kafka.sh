#!/usr/bin/env bash
set -e

curl -X DELETE http://localhost:8083/connectors/debezium-connector
