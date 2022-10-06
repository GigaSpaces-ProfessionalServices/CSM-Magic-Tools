#!/bin/bash

#
# setting COCKPIT_HOME value in /etc/environment
#

ENV_FILE=/etc/environment
COCKPIT_HOME=$(realpath $(dirname $0)/../)

# remove current setting and insert new
sed -i '/COCKPIT_HOME=/d' $ENV_FILE
echo "export COCKPIT_HOME=${COCKPIT_HOME}" >> $ENV_FILE

echo "COCKPIT_HOME has been set to: '${COCKPIT_HOME}'"

# source it
source $ENV_FILE && exec bash
