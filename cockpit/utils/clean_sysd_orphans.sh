#!/bin/bash

#
# cleaning orphaned systemd cockpit services / scripts
#

SYS=/etc/systemd/system
COCKPIT_HOME=$(realpath $(dirname $0))

for unit in $(systemctl list-timers | awk '{print $13}' | grep cockpit); do
    policy_name={unit*.?}
done



# # remove current setting
# sed -i '/COCKPIT_HOME=/d' $ENV_FILE

# # insert new setting
# echo "export COCKPIT_HOME=${COCKPIT_HOME}" >> $ENV_FILE

# echo "set COCKPIT_HOME to: '${COCKPIT_HOME}'"

# # source it
# source $ENV_FILE && exec bash
