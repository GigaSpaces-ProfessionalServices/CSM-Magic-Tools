#!/bin/bash

#
# RUN GARBAGE COLLECTION ON SPACE GSCs
#

function usage() {
    local name=$(basename $0)
    printf "\n${name}\n"
    printf "\n%-10s\n%7s%-50s\n" "Usage:" " " "${name} command [option]"
    printf "\n%-10s\n" "Description:"
    printf "%7s%-50s\n" "" "Execute garbage collection on space node(s) for a selected zone"
    printf "\n%-10s\n" "Commands:"
    printf "%2s%-4s%-15s%-50s\n" "" "-z," "--zone=ZONE" "Specify the target zone for garbage collection"
    printf "\n%-10s\n" "Options:"
    printf "%2s%-4s%-15s%-50s\n" "" "-n," "--node=HOSTNAME" "Specify a target node. If not specified DEFAULT=ALL"
    printf "%2s%-4s%-15s%-50s\n" "" "-w," "--wait=SECONDS" "Amount of seconds to wait between each process execution. DEFAULT=5"
    printf "%2s%-4s%-15s%-50s\n" "" "-h," "--help" "Display this help screen"
    printf "\n"
}

function gc_run() {
    local n="$1"
    # check node is accessable via ssh
    if [[ $(ssh -q $n "exit" ; echo $?) -ne 0 ]]; then
            echo "ERROR: unable to connect to host: $n"
    else
        echo ">>> running garbage collection on host: ${n^^}"
        for pid in $(ssh $n "ps ax | grep -v grep | grep GSC | grep java | grep zones=${ZONE}" | awk '{print $1}'); do
            ssh $n "jcmd $pid GC.run"
            sleep $WAIT
        done
    fi
}

### GLOBALS ###
WAIT=5
ZONE_OK=false
NODE="ALL"
HOST_CONNECT_OK=false

### ARGUMENTS ###
while [[ $# -gt 0 ]]; do
    case $1 in
        -z|--zone=*)
            ZONE_OK=true
            if [[ $1 == '-z' ]]; then
                shift
                ZONE="$1"
            else
                ZONE="$(echo $1 | cut -d= -f2)"
            fi
            [[ $ZONE == "" ]] && usage && exit
            ;;
        -n|--node=*)
            if [[ $1 == '-n' ]]; then
                shift
                NODE="$1"
            else
                NODE="$(echo $1 | cut -d= -f2)"
            fi
            [[ $NODE == "" ]] && NODE="ALL"
            ;;
        -w|--wait=*)
            if [[ $1 == '-w' ]]; then
                shift
                WAIT="$1"
            else
                WAIT="$(echo $1 | cut -d= -f2)"
            fi
            [[ $WAIT == "" ]] && WAIT=5
            ;;
        -h|--help) usage && exit
    esac
    shift
done

### MAIN ###
if $ZONE_OK ; then
    if [[ $NODE == "ALL" ]]; then
        for n in $(./runall.sh -s -l | grep -v ===); do gc_run $n ; done
    else
        gc_run $NODE
    fi
else
    echo "ZONE is required!"
    usage
fi

exit