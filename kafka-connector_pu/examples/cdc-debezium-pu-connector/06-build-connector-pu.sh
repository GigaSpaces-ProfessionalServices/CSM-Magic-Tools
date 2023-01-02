#!/usr/bin/env bash
set -e

cd ../..
mvn clean package -DskipTests -P pu

printf "\n"
echo "Ready for the next step"
