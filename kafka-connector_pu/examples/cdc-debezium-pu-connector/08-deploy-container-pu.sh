#!/usr/bin/env bash
set -e

$GS_HOME/bin/gs.sh pu deploy kafka-connector \
  ../../target/kafka-connector-1.0.0-SNAPSHOT-jar-with-dependencies.jar \
  --zones connector $*

printf "\n"
echo "Ready for the next step"
