#!/usr/bin/env bash
set -e

WORK_DIR=${PWD}
cd ../..
mvn clean spring-boot:run \
  -Dspring-boot.run.profiles=learning \
  -Dspring.config.location=file:$WORK_DIR/application.yml \
  -Doutput.file.location=$WORK_DIR/data-pipeline.yml

printf "\n"
echo "Ready for the next step"
