#!/bin/bash

# global vars
GS_ROOT="/dbagiga"
UTILS_DIR="${GS_ROOT}/utils"
LOGS_DIR="${UTILS_DIR}/logs"
LOG_FILE="${LOGS_DIR}/$(echo $(basename ${0%.*})).log"

### text styling globals ###

bold=$(tput bold)        # bold text
nbold=$(tput sgr0)       # not bold text
red='\033[0;31m'        # red text
lred='\033[1;31m'       # light red text
green='\033[0;32m'      # green text
lgreen='\033[1;32m'     # light green text
orange='\033[0;33m'     # orange text
yellow='\033[1;33m'     # yellow text
blue='\033[0;34m'       # blue text
lblue='\033[1;34m'      # light blue text
purple='\033[0;35m'     # purple text
lpurple='\033[1;35m'    # light purple text
cyan='\033[0;36m'       # cyan text
lcyan='\033[1;36m'      # light cyan text
reset='\033[0m'         # no colour


function logit() {
    # print to screen or logfile
    # login accepts 4 arguments:
    # $1: --text (print the nstring input) / --service (print status according to value input)
    # $2: string to print / value
    # $3: -f (print to file), -s (print to screen), -fs (print to both)
    # $4: severity (incident level to register in log file)

    local log_type=$1
    local log_text=$2
    local print_to=$3
    local severity=$4
    local ts="$(date +"%Y-%m-%d %H:%M:%S")"
    local loggin_ok=true

    # check login prereqisites
    if [[ -z $LOG_FILE ]]; then
        loggin_ok=false
        printf "\nRequired LOG_FILE variable is not set!\nLoggin is disabled.\n\n"
    fi
    if [[ ! -d $LOGS_DIR ]]; then
        logging_ok=false
        printf "\nRequired logs directory '$LOGS_DIR' doesn't exist.\nLoggin is disabled.\n\n"
    fi

    # exec logging
    case $log_type in
        '--text')
            local txt_to_file="$(printf "%s %-8s %s" "${ts}" "$severity" "${log_text}")"
            local txt_to_screen="${log_text}"
            ;;
        '--state')
            local txt_to_file="${log_text}\n"
            case $log_text in
                "Passed") log_text="${lgreen}${log_text}${reset}" ;;
                "Failed") log_text="${lred}${log_text}${reset}" ;;
                "Up") log_text="${lgreen}${log_text}${reset}" ;;
                "Down") log_text="${lred}${log_text}${reset}" ;;
                "Active") log_text="${lgreen}${log_text}${reset}" ;;
                "Inactive") log_text="${lred}${log_text}${reset}" ;;
                "Leader") log_text="${lblue}${log_text}${reset}" ;;
                "Follower") log_text="${lblue}${log_text}${reset}" ;;
            esac
            local txt_to_screen="${log_text}\n"
    esac
    case $print_to in
        '-f') $logging_ok && echo -ne "$txt_to_file" >> $LOG_FILE ;;
        '-s') echo -ne "$txt_to_screen" ;;
        '-fs'|'-sf')
            $logging_ok && echo -ne "$txt_to_file" >> $LOG_FILE
            echo -ne "$txt_to_screen" ;;
        *)  echo -ne "$txt_to_screen" ;;
    esac
}


function get_cluster_hosts {
    local cluster_name=$1
    local prefix=$2
    local s='[[:space:]]*' w='[a-zA-Z0-9_]*' fs=$(echo @|tr @ '\034')
    sed -ne "s|^\($s\):|\1|" \
        -e "s|^\($s\)\($w\)$s:$s[\"']\(.*\)[\"']$s\$|\1$fs\2$fs\3|p" \
        -e "s|^\($s\)\($w\)$s:$s\(.*\)$s\$|\1$fs\2$fs\3|p" \
        ${ODSXARTIFACTS}/odsx/host.yaml |
    awk -F$fs '{
        indent = length($1)/2;
        vname[indent] = $2;
        for (i in vname) {if (i > indent) {delete vname[i]}}
        if (length($3) > 0) {
            vn=""; for (i=0; i<indent; i++) {vn=(vn)(vname[i])("_")}
            printf("%s%s%s=\"%s\"\n", "'$prefix'",vn, $2, $3);
        }
    }' | while read line; do
        [[ "$line" =~ .*"${cluster_name}_host".* ]] && \
        echo $line | sed 's/ *//g' | sed 's/"//g' | cut -d= -f2
    done
}