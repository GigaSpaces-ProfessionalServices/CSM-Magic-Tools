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

function is_accessible() {
    local the_host=$1
    local the_port=$2

    # check port
    nc -z $the_host $the_port && return 0 || return 1
}


function is_rest_ok() {
    local the_host=$1
    local the_uri=$2
    
    # check rest
    local status_code=$(curl -u "$AUTH_USER:$AUTH_PASS" \
    --write-out '%{http_code}' --silent --output /dev/null "$the_uri")
    [[ $status_code -eq 200 ]] && return 0 || return 1
}


#
# # # MAIN # # #
#

ENV_CONFIG="/gigashare/env_config"
AUTH_USER=""
AUTH_PASS=""
MEASUREMENT="di_iidr-Status"

# initialize influx data array
influx_data=()

# # get credentials if env is secured
# get_auth

# # get manager host
# p=8090
# for h in $(get_cluster_hosts "manager"); do
#     if is_accessible $h $p ; then
#         uri="http://${h}:${p}/v2/index.html"
#         if is_rest_ok $h $uri; then
#             MANAGER=$h
#             break
#         fi
#     fi
# done
# if [[ -z $MANAGER ]]; then
#     echo "[ERROR] no avaialable managers found!"
#     exit
# fi

# BASE_URL="http://${MANAGER}:8090/v2"
# CURL_AUTH_COOKIE=/tmp/.curl_auth_cookie_${RANDOM}

# if [[ $AUTH_USER != "" ]] ; then
#     # cache login
#     [[ -e $CURL_AUTH_COOKIE ]] && rm -f $CURL_AUTH_COOKIE
#     curl --user $AUTH_USER:$AUTH_PASS --cookie-jar $CURL_AUTH_COOKIE $BASE_URL
#     if [[ ! -e $CURL_AUTH_COOKIE ]]; then
#         echo "error: could not build auth cache file"
#         exit 1
#     fi
# fi

# get DI MDM target
key="dataIntegration"
p=6081
for h in $(get_cluster_hosts "$key"); do
    if is_accessible $h $p ; then
        di_mdm=$h
        break
    fi
done
if [[ -z $di_mdm ]]; then
    influx_data+=("$MEASUREMENT,host_type=$key,rest_host=${h},service=di_mdm_state,port=$p status=500")
else
    uri="http://${di_mdm}:${p}/api/v1/about"
    di_mdm_state=$(curl -sX GET $uri -H 'accept: */*' | grep DI-MDM | wc -l)
    influx_data+=("$MEASUREMENT,host_type=$key,rest_host=${di_mdm},service=di_mdm_state,port=$p status=$di_mdm_state")
fi

# get DI manager target
key="dataIntegration"
p=6080
for h in $(get_cluster_hosts "$key"); do
    if is_accessible $h $p ; then
        di_manager=$h
        break
    fi
done
if [[ -z $di_manager ]]; then
    influx_data+=("$MEASUREMENT,host_type=$key,rest_host=${h},service=di_mdm_state,port=$p status=500")
else
    uri="http://${di_manager}:${p}/api/v1/about"
    di_manager_state=$(curl -sX GET $uri -H 'accept: */*' | grep DI-Manager | wc -l)
    influx_data+=("$MEASUREMENT,host_type=$key,rest_host=${di_manager},service=di_manager_state,port=$p status=$di_manager_state")
fi

# get DI subscriptions target
key="cdc"
p=6082
for h in $(get_cluster_hosts "$key"); do
    if is_accessible $h $p ; then
        subs_manager=$h
        break
    fi
done
if [[ -z $subs_manager ]]; then
    influx_data+=("$MEASUREMENT,host_type=$key,rest_host=${h},service=di_mdm_state,port=$p status=500")
else
    uri="http://${subs_manager}:${p}/api/v1/about"
    di_sub_manager_state=$(curl -sX GET $uri -H 'accept: */*' | grep DI-Subscription-Manager | wc -l)
    influx_data+=("$MEASUREMENT,host_type=$key,rest_host=${subs_manager},service=di_sub_manager_state,port=$p status=$di_sub_manager_state")
fi

# get Flink target
key="dataIntegration"
p=8081
for h in $(get_cluster_hosts "$key"); do
    if is_accessible $h $p ; then
        di_flink=$h
        break
    fi
done
if [[ -z $di_flink ]]; then
    influx_data+=("$MEASUREMENT,host_type=$key,rest_host=${h},service=di_mdm_state,port=$p status=500")
else
    uri="http://${di_flink}:${p}/"
    di_flink_state=$(curl -vs "$uri" 2>&1 | grep "HTTP/1.1 200 OK" | wc -l)
    influx_data+=("$MEASUREMENT,host_type=$key,rest_host=${di_flink},service=di_flink_state,port=$p status=$di_flink_state")
fi

# output influx data
for i in "${influx_data[@]}"; do echo "$i" ; done

# # delete session cookie
# rm -f $CURL_AUTH_COOKIE

exit

