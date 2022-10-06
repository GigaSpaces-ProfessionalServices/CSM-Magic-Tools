#!/bin/bash

#
# purge cockpit database, services and scripts
#

SYS_HOME=/etc/systemd/system
COCKPIT_HOME=$(realpath $(dirname $0)/../)
CONFIG=$(realpath ${COCKPIT_HOME}/config/config.yaml)

clear
echo ; echo "WARNING!!!"
echo "THIS WILL DELETE ANY COCKPIT DATA ON THE SYSTEM AND MIGHT BREAK COCKPIT OPERATIONS"
echo "THIS INCLUDES:"
echo " - SYSTEMD TIMERS AND SERVICES"
echo " - POLICY MANGER AND WORKER FILES DELETION"
echo " - COCKPIT DATABASE"
echo

read -p "Do you wish to continue? [yes/no]   " ans
if [[ ${ans^^} == "YES" ]]; then
    timers=$(systemctl list-timers | egrep -iow "cockpit_policy.*timer")
    if [[ $timers != "" ]]; then
        for unit in $timers; do
            policy_name=${unit%.*}
            p_service=${policy_name}.service
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
    else
        echo "no cockpit services are enabled!"
    fi
    
    # delete jobs
    if [[ -d ${COCKPIT_HOME}/jobs ]]; then
        rm -rf ${COCKPIT_HOME}/jobs
        echo "deleted jobs directory"
    fi
    # delete policies
    if [[ -d ${COCKPIT_HOME}/policies ]]; then
        rm -rf ${COCKPIT_HOME}/policies
        echo "deleted policies directory"
    fi

    # delete cockpit database
    CPDB=$(cat $CONFIG | grep "db_home:" | cut -d: -f2 | sed 's/ *//')
    if [[ -d $CPDB ]]; then
        rm -rf $CPDB
        echo "deleted cockpit database home (${CPDB})"
    fi

    # delete any stale systemd services
    find ${SYS_HOME} -type f -name "cockpit_policy*" | while read file; do
        rm -f $file
        echo "deleted stale cockpit service: ${file}"
    done
fi

exit