#!/usr/bin/env bash
set -e

WORK_DIR=${PWD}
cd ../..
mvn spring-boot:run \
  -Dspring-boot.run.profiles=learning \
  -Dspring.config.location=file:$WORK_DIR/connector-sqlserver.yml \
  -Doutput.file.location=$WORK_DIR/data-pipeline-sqlserver.yml

printf "\n"
echo "Ready for the next step"
