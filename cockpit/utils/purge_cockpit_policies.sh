#!/bin/bash

#
# purge cockpit policies (cockpit services and scripts)
#

SYS_HOME=/etc/systemd/system
COCKPIT_HOME=$(realpath $(dirname $0))

echo "WARNING!!!"
echo "THIS WILL DELETE ANY COCKPIT POLICIES ON THE SYSTEM AND MIGHT BREAK COCKPIT OPERATIONS"
echo "OPERATIONS INCLUDE:"
echo " - SYSTEMD TIMERS AND SERVICES DELETION"
echo " - POLICY MANGER AND WORKER FILES DELETION"
echo
echo "*** RELATED DATABASE RECORDS WILL HAVE TO BE REMOVED MANUALLY ***"

read -p "Do you wish to continue? [yes/no]   " ans
if [[ ${ans^^} == "YES" ]]; then
    for unit in $(systemctl list-timers | egrep -iow "cockpit_policy.*"); do
        p_name=${unit%.*}
        p_service=${p_name}.service
        # stop
        systemctl stop ${unit}
        systemctl stop ${p_service}
        # disable
        systemctl disable ${unit}
        systemctl disable ${p_service}
        # remove
        if [[ ${policy_name} != "" ]]; then
            rm -f ${SYS_HOME}/${policy_name}*
        fi
        # reload
        systemctl daemon-reload
    done

    # delete policy files
    if [[ -d ${COCKPIT_HOME}/policies ]]; then
        find ${COCKPIT_HOME}/policies -name "${policy_name}*" -exec rm -f {} \;
    fi
fi

exit