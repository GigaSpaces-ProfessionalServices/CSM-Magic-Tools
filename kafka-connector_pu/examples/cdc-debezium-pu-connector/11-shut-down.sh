#!/usr/bin/env bash
set -e

# Shut down the cluster
export DEBEZIUM_VERSION=1.6
docker-compose -f docker-compose.yaml down
