#!/usr/bin/env bash
#set -e

echo "Updating connector schema definitions."
read -p "Press Enter to continue."
./run-learning-prices.sh

echo "Final step. Starting connector."
read -p "Press Enter to continue."

GS_LOOKUP_LOCATORS=localhost:4174

echo `basename "$0"` > last_step.txt

WORK_DIR=${PWD}
(
cd ../..
mvn clean spring-boot:run \
  -Dspring-boot.run.profiles=connector \
  -Dcom.gs.jini_lus.locators=$GS_LOOKUP_LOCATORS \
  -Dspring.config.location=file:$WORK_DIR/connector-prices.yml \
  -Dpipeline.config.location=$WORK_DIR/data-pipeline-prices.yml
)
