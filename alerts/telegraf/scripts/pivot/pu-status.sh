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
    sec_flag=$(cat $ENV_CONFIG/app.config | grep "app.setup.profile" | cut -d= -f2)
    if [[ $sec_flag != "" ]]; then
        declare -g AUTH_USER=$(cat $ENV_CONFIG/app.config | grep "app.manager.security.username" | cut -d= -f2)
        declare -g AUTH_PASS=$(cat $ENV_CONFIG/app.config | grep "app.manager.security.password" | cut -d= -f2)
    else
		declare -g AUTH_USER=""
		declare -g AUTH_PASS=""
	fi
}

function is_manager_rest_ok() {
    local the_manager=$1
    local port_ok=false
    local rest_ok=false

    # check port
    nc -z $m 8090 && port_ok=true
    
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

# set verbosity
[[ $1 == '-v' ]] && verbose=true || verbose=false

# check host.yaml exists
if [[ ! -e ${ENV_CONFIG}/host.yaml ]]; then
    echo "[ERROR] host.yaml not found. aborting!"
    exit
fi

# get security credentials
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

# check if connecion to space available
if [[ $(curl -u "$AUTH_USER:$AUTH_PASS" -ks "${BASE_URL}/spaces") == "Connect failed" ]]; then
    echo "Connection failed."
    exit
fi

# check if connecion returns data
if [[ $(curl -u "$AUTH_USER:$AUTH_PASS" -ks "${BASE_URL}/spaces") == "" ]]; then
    echo "No data is currently available."
    exit
fi


# initialize data array
pu_info=()

# get pus data
while read pu_name; do
    json_data=$(curl -sk http://${MANAGER}:8090/v2/pus/$pu_name)
    pu_status=$(echo $json_data | jq '.status' | sed 's/"//g')
    pu_unit_type=$(echo $json_data | jq '.processingUnitType' | sed 's/"//g')
    pu_info+=("puStatus,pu_name=$pu_name,pu_unit_type=$pu_unit_type status=\"${pu_status}\"") 
done < <(curl -sk http://${MANAGER}:8090/v2/pus | jq -r '.[].name')

# output influx data
for i in "${pu_info[@]}"; do echo "$i" ; done

exit
