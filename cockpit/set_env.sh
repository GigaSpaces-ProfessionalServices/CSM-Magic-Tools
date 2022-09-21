#!/bin/bash

#
# setting COCKPIT_HOME value in /etc/environment
#

ENV_FILE=/etc/environment
COCKPIT_HOME=$(realpath $(dirname $0))

# remove current setting
sed -i '/COCKPIT_HOME=/d' $ENV_FILE

# insert new setting
echo "export COCKPIT_HOME=${COCKPIT_HOME}" >> $ENV_FILE

echo "set COCKPIT_HOME to: '${COCKPIT_HOME}'"

# source it
source $ENV_FILE && exec bash
