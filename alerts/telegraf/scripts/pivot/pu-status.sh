#!/bin/bash


function get_cluster_hosts {
    local cluster_name=$1
    local prefix=$2
    local s='[[:space:]]*' w='[a-zA-Z0-9_]*' fs=$(echo @|tr @ '\034')
    sed -ne "s|^\($s\):|\1|" \
        -e "s|^\($s\)\($w\)$s:$s[\"']\(.*\)[\"']$s\$|\1$fs\2$fs\3|p" \
        -e "s|^\($s\)\($w\)$s:$s\(.*\)$s\$|\1$fs\2$fs\3|p" \
        ${ENV_CONFIG}/host.yaml |
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


function get_auth() {
    sec_flag=$(cat /gigashare/env_config/app.config | grep "app.setup.profile" | cut -d= -f2)
    if [[ $sec_flag != "" ]]; then
        AUTH_USER=$(cat /gigashare/env_config/app.config | grep "app.manager.security.username" | cut -d= -f2)
        AUTH_PASS=$(cat /gigashare/env_config/app.config | grep "app.manager.security.password" | cut -d= -f2)
    fi
}

function is_manager_rest_ok() {
    local the_manager=$1
    local port_ok=false
    local rest_ok=false

    # check port
    nc -z $the_manager 8090 && port_ok=true
    
    # check rest
    local rest="http://${the_manager}:8090/v2/index.html"
    local status_code=$(curl -u "$AUTH_USER:$AUTH_PASS" \
    --write-out '%{http_code}' --silent --output /dev/null "$rest")
    [[ $status_code -eq 200 ]] && rest_ok=true
    
    ($port_ok && $rest_ok) && return 0 || return 1
}


#
# # # MAIN # # #
#

ENV_CONFIG="/gigashare/env_config"
AUTH_USER=""
AUTH_PASS=""

# check host.yaml exists
if [[ ! -e ${ENV_CONFIG}/host.yaml ]]; then
    echo "[ERROR] host.yaml not found. aborting!"
    exit
fi

# get credentials if env is secured
get_auth

# get manager host
for m in $(get_cluster_hosts "manager"); do
    if is_manager_rest_ok $m ; then
        MANAGER=$m
        break
    fi
done
if [[ -z $MANAGER ]]; then
    echo "[ERROR] no avaialable managers found!"
    exit
fi

BASE_URL="http://${MANAGER}:8090/v2"
SHOB_COOKIE=/tmp/.shob_cookie

if [[ $AUTH_USER != "" ]] ; then
    # cache login
    [[ -e $SHOB_COOKIE ]] && rm -f $SHOB_COOKIE
    curl --user $AUTH_USER:$AUTH_PASS --cookie-jar $SHOB_COOKIE $BASE_URL
    if [[ ! -e $SHOB_COOKIE ]]; then
        echo "error: could not build auth cache file"
        exit 1
    fi
fi

# check if connecion to space available
if [[ $(curl --cookie $SHOB_COOKIE -sk "${BASE_URL}/spaces") == "Connect failed" ]]; then
    echo "Connection failed."
    exit
fi

# check if connecion returns data
if [[ $(curl --cookie $SHOB_COOKIE -sk "${BASE_URL}/spaces") == "" ]]; then
    echo "No data is currently available."
    exit
fi

# parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        stateful) TYPE="stateful" ; break ;;
        stateless) TYPE="stateless" ; break ;;
    esac
done

# initialize data array
pu_info=()

if [[ -z $TYPE ]] ; then
    while read pu_name; do
        json_data=$(curl --cookie $SHOB_COOKIE -sk $BASE_URL/pus/$pu_name)
        pu_status=$(echo $json_data | jq '.status' | sed 's/"//g')
        pu_unit_type=$(echo $json_data | jq '.processingUnitType' | sed 's/"//g')
        pu_info+=("puStatus,pu_name=$pu_name,pu_unit_type=$pu_unit_type status=\"${pu_status}\"") 
    done < <(curl --cookie $SHOB_COOKIE -sk $BASE_URL/pus | jq -r '.[].name')
else
    while read pu_name; do
        json_data=$(curl --cookie $SHOB_COOKIE -sk $BASE_URL/pus/$pu_name)
        pu_status=$(echo $json_data | jq '.status' | sed 's/"//g')
        pu_unit_type=$(echo $json_data | jq '.processingUnitType' | sed 's/"//g')
        pu_info+=("puStatus,pu_name=$pu_name,pu_unit_type=$pu_unit_type status=\"${pu_status}\"") 
    done < <(curl --cookie $SHOB_COOKIE -sk $BASE_URL/pus | jq -r --arg ptype "$TYPE" '.[] | select(.processingUnitType == $ptype) | .name')
fi



# output influx data
for i in "${pu_info[@]}"; do echo "$i" ; done

# delete session cookie
rm -f $SHOB_COOKIE

exit