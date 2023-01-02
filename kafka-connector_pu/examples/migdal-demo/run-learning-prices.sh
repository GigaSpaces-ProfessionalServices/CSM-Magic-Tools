#!/usr/bin/env bash
set -e

WORK_DIR=${PWD}
cd ../..
mvn spring-boot:run \
  -Dspring-boot.run.profiles=learning \
  -Dspring.config.location=file:$WORK_DIR/connector-prices.yml \
  -Doutput.file.location=$WORK_DIR/data-pipeline-prices.yml

printf "\n"
echo "Updated schema definitions. Next step - run in connector mode."
