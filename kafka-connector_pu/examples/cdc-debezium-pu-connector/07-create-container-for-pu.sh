#!/usr/bin/env bash
set -e

echo "This example is for local run only (assuming GS container can access local file)"
read -p "Press Enter to continue."

WORK_DIR=${PWD}

$GS_HOME/bin/gs.sh container create \
  --zone connector \
  --vm-option -Dspring.profiles.active=connector \
  --vm-option -Dpipeline.config.location=$WORK_DIR/data-pipeline.yml \
  localhost

printf "\n"
echo "Ready for the next step"
