#!/bin/bash

#
# runall.sh
# - execute command(s) on ODS servers via ssh
# - run health checks (connectivity/application/configuration)
#
# By Alon Segal, Dec 2021
#

VERSION=2.6.6


function usage() {
    printf "\nrunall.sh v$VERSION\n"
    printf "\n%-10s\n%7s%-50s\n" "Usage:" " " "runall.sh [option] [command]"
    printf "\n%-10s\n" "Description:"
    printf "%7s%-50s\n" " " "Execute command(s) on ODS servers via ssh"
    printf "%7s%-50s\n" " " "Run health checks (connectivity/application/configuration)"
    printf "%7s%-50s\n" " " "Note: without options commands will run for all servers"
    case $1 in
        err0)
            printf "\nMissing required argument(s). "
            printf "See help [-h, --help] for usage.\n"
            ;;
        *)
            printf "\n%-10s\n" "Options:"
            printf "%2s%-13s%-50s\n" "" "-s" "Connect to ODS space servers"
            printf "%2s%-13s%-50s\n" "" "-m" "Connect to ODS management servers"
            printf "%2s%-13s%-50s\n" "" "-a" "Connect to ODS space and management servers"
            printf "%2s%-13s%-50s\n" "" "-c" "Connect to CDC servers"
            printf "%2s%-13s%-50s\n" "" "-d" "Connect to DI servers"
            printf "%2s%-13s%-50s\n" "" "-na" "Connect to northbound application servers"
            printf "%2s%-13s%-50s\n" "" "-nm" "Connect to northbound management servers"
            printf "%2s%-13s%-50s\n" "" "-n" "Connect to northbound application and management servers"
            printf "%2s%-13s%-50s\n" "" "-p" "Connect to pivot/admin server"
            printf "%2s%-13s%-50s\n" "" "-cp" "Connect to cockpit server"
            printf "%2s%-13s%-50s\n" "" "-A" "Connect to all servers (including pivot server)"
            printf "%2s%-13s%-50s\n" "" "-q" "Do not print error summary"
            printf "%2s%-5s%-8s%-50s\n" "" "-h, " "--help" "Display this help screen"
            printf "\n%-10s\n" "Commands:"
            printf "%2s%-13s%-50s\n" "" "-l" "List nodes"
            printf "%2s%-13s%-50s\n" "" "-v" "Display version"
            printf "%2s%-13s%-50s\n" "" "-hc" "Run health checks"
            printf "%2s%-13s%-50s\n" "" "-hw" "Display complete hardware information"
            printf "%2s%-30s%-50s\n" "" "-hw.method[=property]" "Invoke methods to display specific hardware information"
            printf "%2s%-30s%-50s\n" "" "" "collection of methods is supported (e.g: -hw.cpu-count -hw.mem.count)"
            printf "%5s%-27s%-50s\n" "" ".capacity=<VOLUME>" "Get capacity stats for the named volume"
            printf "%5s%-27s%-50s\n" "" ".cpu-count" "Get number of CPU cores"
            printf "%5s%-27s%-50s\n" "" ".cpu-load" "Get CPU load stats "
            printf "%5s%-27s%-50s\n" "" ".mem-count" "Get amount of RAM"
            printf "%5s%-27s%-50s\n" "" ".mem-load" "Get memory usage stats"
            printf "%2s%-30s%-50s\n" "" "copy -src PATHS -tgt PATH" "Copy data to nodes in a cluster (source collections supported)"
            printf "%2s%-13s%-50s\n" "" "<command>" "Execute shell command(s) on all hosts for the selected cluster"
            printf "\n"
    esac
}


function logit() {
    # print to screen or logfile
    # arg $1: --text (print the string input) / --service (print status according to value input)
    # arg $2: string to print / value
    # arg $3: -f (print to file), -s (print to screen), -fs (print to both)
    # arg $4: severity (incident level to register in log file)

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
                "Down") log_text="${yellow}${log_text}${reset}" ;;
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


function get_targeted_servers() {
     local SERVER_GROUP=(
        "space"             # [0]
        "manager"           # [1]
        "dataIntegration"   # [2]
        "nb_applicative"    # [3]
        "nb_management"     # [4]
        "pivot"             # [5]
        "cockpit"           # [6]
    )
    case $1 in
        -l) list_all_servers ; exit ;;
        -s) local env_preffix="_s_"
            local srv_group=(${SERVER_GROUP[0]}) ;;
        -m) local env_preffix="_m_"
            local srv_group=(${SERVER_GROUP[1]}) ;;
        -a) local env_preffix="_a_"
            local srv_group=(${SERVER_GROUP[0]} ${SERVER_GROUP[1]}) ;;
        -c) local env_preffix="_c_"
            local srv_group=(${SERVER_GROUP[2]}) ;;
        -d) local env_preffix="_d_"
            local srv_group=(${SERVER_GROUP[2]}) ;;
        -na) local env_preffix="_na_"
            local srv_group=(${SERVER_GROUP[3]}) ;;
        -nm) local env_preffix="_nm_"
            local srv_group=(${SERVER_GROUP[4]}) ;;
        -n) local env_preffix="_n_"
            local srv_group=(${SERVER_GROUP[3]} ${SERVER_GROUP[4]}) ;;
        -p) local env_preffix="_p_"
            local srv_group=(${SERVER_GROUP[5]}) ;;
        -cp) local env_preffix="_cp_"
            local srv_group=(${SERVER_GROUP[6]}) ;;
        -A) local env_preffix="_A_"
            local srv_group=${SERVER_GROUP[@]} ;;
        *)
            echo "invalid option or bad syntax."
            usage ; exit
    esac
    ENV_NAME="$(cat $CONFIG_FILE | grep "${env_preffix}ENV_NAME" | cut -d'=' -f2)"
    # load services
    if [[ ${#env_preffix[@]} -eq 1 ]]; then
        SERVICES="$(cat $CONFIG_FILE | grep "${env_preffix[0]}SERVICES" | cut -d= -f2)"
    fi
    # build server list
    servers=""
    for s in ${srv_group[@]}; do
        servers+=" $(for h in $(get_cluster_hosts $s); do hlist+=" $h"; done ; echo $hlist)"
    done
    servers=$(echo $servers | xargs)
    SERVER_LIST=""
    for node in $servers; do
        grep -q $node <<< $SERVER_LIST && continue || SERVER_LIST+=" ${node}"
    done
    SERVER_LIST=$(echo $SERVER_LIST | xargs)
}


function text_align() {
    # align text presentation according to string length.
    # *** change 'row_width' value as necessary to control output alignment
    local str=$1
    local txt_type=$2
    local row_width=80
    case $txt_type in
        --title)
            local width=$(( $(( $row_width - ${#str} )) / 2 ))
            for i in $(seq 1 $width); do chars="${chars}$(echo -n "=")"; done
            echo "${chars} ${str} ${chars}"
            ;;
        --params)
            local width=$(($row_width - ${#str}))
            for i in $(seq 1 $width); do chars="${chars}$(echo -n " ")"; done
            echo "${str}${chars}"
            ;;
        *)
            local width=$(($row_width - ${#str}))
            for i in $(seq 1 $width); do chars="${chars}$(echo -n ".")"; done
            echo "${str}${chars} "
    esac
}


function list_all_servers() {
    # list servers by cluster
    local target_env=${1:-$ENV_TYPES}
    for type in $target_env; do
        unset SERVER_LIST
        get_targeted_servers $type
        logit --text "${bold}$(text_align "${ENV_NAME^^}" "--title")${nbold}\n" -s
        if [[ ${SERVER_LIST[@]} == "" ]]; then
            echo "None"
            continue
        fi
        for node in $SERVER_LIST; do echo "$node" | sed 's/ *//g' ; done
    done
}


function check_services() {
  # check service(s) status
  local host="$1"
  local port="$2"
  local service_name="$3"
  local retval=""
  case $port in
        0) ping -c 1 $host >/dev/null 2>&1 ; echo $? ;;
        22) ssh -q $host "exit" ; echo $? ;;
        *) retval=$(ssh $host "netstat -ntl | grep $port >/dev/null 2>&1" ; echo $?) ; echo $retval ;;
  esac
}


function get_kafka_status() {
    # using kafka's utility to check its cluster health
    local host=$1
    local kafka_home="/dbagiga/kafka_latest"
    if $(ssh $host "[[ -e "$kafka_home/bin/zookeeper-shell.sh" ]] && echo true || echo false") ; then
        local return_code=$(ssh $host "$kafka_home/bin/zookeeper-shell.sh localhost:2181 ls /brokers/ids" > /dev/null 2>&1 ; echo $?)
        if [[ $return_code -eq 0 ]]; then
            logit --text "$(text_align "[${host}]${R_SPC}Kafka-Zookeeper cluster connection")" -fs INFO
            logit --state "Passed" -fs
            # if kafka-zk connection is ok we check the kafka cluster
            local rstr="$(ssh $host "$kafka_home/bin/zookeeper-shell.sh localhost:2181 ls /brokers/ids 2>&1" | grep -v "ERROR" | tail -1)"
            if [[ $rstr == "[1, 2, 3]" ]]; then
                local status="Passed" ; local severity="INFO"
            else
                local status="Failed" ; local severity="ERROR"
                $ERR_REPORT && \
                CLUSTER_ERRORS[${#CLUSTER_ERRORS[@]}]="[${host}] [ERROR]${R_SPC}Kafka members not found!"
            fi
            logit --text "$(text_align "[${host}]${R_SPC}Kafka cluster status (members: $rstr)")" -fs $severity
            logit --state $status -fs
        else
            logit --text "$(text_align "[${host}]${R_SPC}Kafka-Zookeeper cluster connection")" -fs ERROR
            logit --state "Failed" -fs
            $ERR_REPORT && \
            CLUSTER_ERRORS[${#CLUSTER_ERRORS[@]}]="[${host}] [ERROR]${R_SPC}Kafka-Zookeeper connection test failed!"
        fi
    else
        logit --text "$(text_align "[${host}]${R_SPC}Kafka deployment was not found!")" -fs ERROR
    fi
}


function get_cpu_count(){
    # get number of cpu cores
    local host=$1
    num_cpu_cores=$(ssh $host "grep processor /proc/cpuinfo | wc -l")
    logit --text "$(text_align "[${host}]${R_SPC}[CPU] # of cores: $num_cpu_cores" "--params")\n" -fs INFO
}


function get_cpu_load() {
    # get cpu load for the past 1m, 5m and 15m
    local host=$1
    rstr="$(ssh $host "cat /proc/loadavg")"
    load="$(echo $rstr | awk '{print "1m:"$1 " 5m:"$2 " 15m:"$3}')"
    logit --text "$(text_align "[${host}]${R_SPC}[CPU] average load: ${load}" "--params")\n" -fs INFO
}


function get_mem_count() {
    # get total amount of memory
    rstr="$(ssh $host "grep -i 'MemTotal' /proc/meminfo | sed 's/ //g' | sed 's/kB//g' | sed 's/MemTotal\://g'")"
    total=$(($rstr/1000/1000))
    [[ $(expr $total % 2) -eq 1 ]] && total=$((total+1))
    logit --text "$(text_align "[${host}]${R_SPC}[RAM] Total amount of memory: \
    ${total}GB" "--params")\n" -fs INFO
}


function get_mem_load() {
    # get memory usage
    local host=$1
    local rstr="$(ssh $host "grep -i 'MemFree\|Buffers\|Cached\|SwapTotal\|SwapFree' /proc/meminfo | sed 's/ //g'")"
    logit --text "$(text_align "[${host}]${R_SPC}[RAM] usage:" "--params")\n" -fs INFO
    for s in $rstr; do
        local type=$(echo $s | cut -d: -f1)
        local value=$(echo $s | cut -d: -f2)
        logit --text "$(text_align "${R_SPC}${R_SPC}${R_SPC}${R_SPC}${type}: ${value}" "--params")\n" -fs INFO
    done
}


function get_volume_usage(){
    # get used capacity for root volume
    local target_vol=$1
    local host=$2
    # check if volume exists
    if [[ $(ssh $host "df -h $target_vol >/dev/null 2>&1" ; echo $?) -eq 0 ]]; then
        local rstr=$(ssh $host "df -h ${target_vol} | tail -1")
        local hdd_dev="$(echo $rstr | awk '{print $1}')"
        local hdd_cap=$(echo $rstr | awk '{print $5}' | sed 's/%//g')
        if [[ $hdd_cap -ge 90 ]]; then
            local set_color="${red}"
            local severity="CRITICAL"
            $ERR_REPORT && \
            CLUSTER_ERRORS[${#CLUSTER_ERRORS[@]}]="[${host}]${R_SPC}[STORAGE] '${target_vol}' capacity level: $severity"
        elif [[ $hdd_cap -ge 90 ]]; then
            local set_color="${lred}"
            local severity="ERROR"
            $ERR_REPORT && \
            CLUSTER_ERRORS[${#CLUSTER_ERRORS[@]}]="[${host}]${R_SPC}[STORAGE] '${target_vol}' capacity level: $severity"
        elif [[ $hdd_cap -ge 70 ]]; then
            local set_color="${yellow}"
            local severity="WARNING"
        else
            local set_color="${lgreen}"
            local severity="INFO"
        fi
        logit --text "$(text_align "[${host}]${R_SPC}[STORAGE] device: $hdd_dev" "--params")\n" -fs INFO
        logit --text "$(text_align "[${host}]${R_SPC}[STORAGE] '${target_vol}' capacity: ${set_color}${hdd_cap}%${reset} used" "--params")\n" -s
        logit --text "$(text_align "[${host}]${R_SPC}[STORAGE] '${target_vol}' capacity: ${hdd_cap}% used" "--params")\n" -f $severity
    else
        logit --text "$(text_align "[${host}]${R_SPC}[STORAGE] '${target_vol}' volume does not exist!" "--params")\n" -fs ERROR
        $ERR_REPORT && \
        CLUSTER_ERRORS[${#CLUSTER_ERRORS[@]}]="[${host}]${R_SPC}[STORAGE] '${target_vol}' volume does not exist!"
    fi
}


function show_hw_report() {
    # aggregate hardware related data
    local env_type=$1
    [[ $env_type == "-cp" ]] && return  # exclude cockpit from checks
    local default_methods=("-hw.cpu-count" "-hw.mem-count" "-hw.capacity='/'" "-hw.cpu-load" "-hw.mem-load")
    get_targeted_servers $env_type
    echo
    # if only '-hw' we use default methods as specified in default_methods array
    if [[ ${#hw_methods[@]} -eq 0 ]]; then
        for ((i=0; i<${#default_methods[@]}; i++)); do hw_methods[$i]=${default_methods[$i]} ; done
    fi
    logit --text "$(text_align "CLUSTER: ${ENV_NAME^^}" "--title")\n" -fs INFO
    for host in $SERVER_LIST; do
        for item in ${hw_methods[@]}; do
            local method=$(echo $item | cut -d. -f2 | cut -d= -f1)
            local target_vol=$(echo $item | cut -d. -f2 | cut -d= -f2)
            case $method in
                'capacity')     get_volume_usage $target_vol $host ;;
                'cpu-count')    get_cpu_count $host ;;
                'cpu-load')     get_cpu_load $host ;;
                'mem-count')    get_mem_count $host ;;
                'mem-load')     get_mem_load $host ;;
                *) echo "ERROR: invalid option '$method'. aborting! " ; exit
            esac
        done
        logit --text "\n" -fs
    done
}


function check_cr8_status() {
    # show cr8 streams status and name
    local host=$1
    local count=0
    # get cr8 service status
    local cr8_ctl="/home/dbsh/cr8/latest_cr8/utils/CR8_ctl.sh"
    # check if cr8 service is deployed
    if ! $(ssh $host "[[ -e $cr8_ctl ]] && echo true || echo false"); then
        logit --text "[${host}]${R_SPC}CR8 installation not found!\n" -fs INFO
        $ERR_REPORT && \
        CLUSTER_ERRORS[${#CLUSTER_ERRORS[@]}]="[${host}] [ERROR]${R_SPC}CR8 installation not found!"
    else
        local return_str=$(ssh $host "su dbsh -c \"${cr8_ctl} status | grep 'status_CR8' | cut -d: -f4\"")
        local is_running=$(echo "$return_str" | grep 'is running' > /dev/null 2>&1 ; echo $?)
        if [[ $is_running == 0 ]]; then
            #local cr8_pid=$(echo "$return_str" | grep -Po "(?<=\[).*?(?=\])")
            logit --text "$(text_align "[${host}]${R_SPC}CR8 service status")" -fs INFO
            logit --state "Active" -fs
        else
            logit --text "$(text_align "[${host}]${R_SPC}CR8 service status")" -fs ERROR
            logit --state "Inactive" -fs
        fi
        # get cr8 streams status
        local idx=0
        ssh $host "su dbsh -c \"${cr8_ctl} status | grep 'status_CR8_Stream'\"" | while read line; do
            streams[$idx]="$line"
            ((idx++))
        done
        if [[ ${#streams[@]} -eq 0 ]]; then
            logit --text "[${host}]${R_SPC}|--- No CR8 streams found!\n" -fs WARNING
        else
            # print status of streams
            logit --text "[${host}]${R_SPC}|--- Found ${#streams[@]} CR8 streams:\n" -fs INFO
            for s in ${streams[@]}; do
                s_name=$(echo "$s" | cut -d: -f1 | grep -Po "(?<=\[).*?(?=\])")
                is_running=$(echo "$s" | grep 'is running' > /dev/null 2>&1 ; echo $?)
                if [[ $is_running == 0 ]]; then
                    #s_pid=$(echo "$s" | cut -d: -f5 | grep -Po "(?<=\[).*?(?=\])")
                    logit --text "$(text_align "[${host}]${R_SPC}CR8 stream '${s_name}' status")" -fs INFO
                    logit --state "Active" -fs
                else
                    logit --text "$(text_align "[${host}]${R_SPC}CR8 stream '${s_name}' status")" -fs WARNING
                    logit --state "Inactive" -fs
                fi
            done
        fi
    fi
}


function check_dns_resolve() {
    # check if dns resolution is correct
    local the_host=$1
    local lookup_name=""
    local lookup_ip=""
    local lookup_name=$(nslookup -timeout=3 $the_host | grep -v "#53" | awk '/Address/ {print $2}')
    if [[ $lookup_name == "" ]]; then
        local lookup_ip=$(nslookup -timeout=3 $the_host | sed '/^$/d' | sed 's/.*name = //' | sed 's/\.$//g')
    fi
    if [[ $lookup_name == "" && $lookup_ip == "" ]]; then
        local err=1
    elif [[ $lookup_name != ""  ]]; then
        local rev_lookup=$(nslookup -timeout=3 $lookup_name | sed '/^$/d' | sed 's/.*name = //' | sed 's/\.$//g')
        if [[ $rev_lookup != "$the_host" ]]; then
            local rev_lookup=$(nslookup -timeout=3 $lookup_name | sed '/^$/d' | sed 's/.*name = //' | cut -d. -f1)
        fi
        if [[ $rev_lookup = "" ]]; then
            local err=1
        else
            [[ "${rev_lookup^^}" == "${the_host^^}" ]] && local err=0 || local err=1
        fi
    elif [[ $lookup_ip != ""  ]]; then
        local rev_lookup=$(nslookup -timeout=3 $lookup_ip | grep -v "#53" | awk '/Address/ {print $2}')
        if [[ $rev_lookup = "" ]]; then
            local err=1
        else
            [[ "${rev_lookup^^}" == "${the_host^^}" ]] && local err=0 || local err=1
        fi
    fi
    echo $err
}


function check_network_consistency() {
    # check if dns resolution is correct
    local host_name=$1
    local cycles=10
    for _ in $(seq $cycles); do
        percent_packet_loss=$(ping -c 1 $host_name | awk '/packet/ {print $6}' | sed 's/%//')
        [[ $percent_packet_loss != "0%" ]] && break
        sleep 1
    done
    echo $percent_packet_loss
}


function check_system_service(){
    # check systemd service exists and active
    local host=$1
    local service_name=$2
    local service_desc=$3
    local sysd_retval=$(ssh $host systemctl list-units | grep -o "$service_name" > /dev/null 2>&1 ; echo $?)
    local sysv_retval=$(ssh $host [[ -f /etc/init.d/${service_name} ]] > /dev/null 2>&1 ; echo $?)
    if [[ $sysd_retval -ne 0 ]] && [[ $sysv_retval -ne 0 ]]; then
        logit --text "[${host}]${R_SPC}${service_desc^^} service not deployed!\n" -fs ERROR
        $ERR_REPORT && \
        CLUSTER_ERRORS[${#CLUSTER_ERRORS[@]}]="[${host}] [ERROR]${R_SPC}${service_desc^^} service not deployed!"
    elif [[ $sysd_retval -eq 0 ]]; then
        retval=$(ssh $host "systemctl is-active $service_name")
        if [[ $retval == "active" ]]; then
            severity="INFO"
            status="Active"
        else
            severity="ERROR"
            status="Inactive"
            $ERR_REPORT && \
            CLUSTER_ERRORS[${#CLUSTER_ERRORS[@]}]="[${host}] [ERROR]${R_SPC}${service_desc^^} service status is: ${retval^}"
        fi
        logit --text "$(text_align "[${host}]${R_SPC}${service_desc^^} service status")" -fs $severity
        logit --state $status -fs
    elif [[ $sysv_retval -eq 0 ]]; then
        retval=$(ssh $host "service $service_name status | grep -o 'running'")
        if [[ $retval == "running" ]]; then
            severity="INFO"
            status="Active"
        else
            severity="ERROR"
            status="Inactive"
            $ERR_REPORT && \
            CLUSTER_ERRORS[${#CLUSTER_ERRORS[@]}]="[${host}] [ERROR]${R_SPC}${service_desc^^} service status is: ${retval^}"
        fi
        logit --text "$(text_align "[${host}]${R_SPC}${service_desc^^} service status")" -fs $severity
        logit --state $status -fs
    fi
}


function check_time() {
    # check time offsets on all servers
    h=$1
    result=$(ssh $h "timedatectl | grep 'synchronized' | cut -d: -f2 | sed 's/ *//g'")
    [[ $result == "yes" ]] && return 0 || return 1
}


function print_errors_report() {
    # display errors report and clean up temporary files
    printf "\n\n"
    printf '#%.0s' {1..31}
    echo -ne "  ERRORS SUMMARY  "
    printf '#%.0s' {1..32}
    printf "\n"
    printf '#%.0s' {1..81}
    printf "\n\n"
    if [[ ${#ERRORS[@]} -gt 0 ]]; then
        for ((i=0; i<${#ERRORS[@]}+1; i++)); do echo "${ERRORS[$i]}" ; done
    else
        echo -e "\nNo errors found!"
    fi
    unset ERRORS
}


function secure_copy() {
    local _source=$1
    local _target=$(echo $2 | sed 's;/*$;;g')
    for host in $SERVER_LIST; do
        scp -r "${_source}" "${host}:${_target}/"
    done
}


function run_health_checks() {
    # traverse the different environments
    # and run health checks according to related SERVICES
    local env_type=$1
    [[ $env_type == "-cp" ]] && return  # exclude cockpit from checks
    local retval=0
    local is_number='^[0-9]+$'
    get_targeted_servers $env_type
    logit --text "$(text_align "CLUSTER: ${ENV_NAME^^}" "--title")\n" -fs "INFO"
    $ERR_REPORT && CLUSTER_ERRORS[0]="$(text_align "CLUSTER: ${ENV_NAME^^}" "--title")"
    # dividing services to [R]emote and [L]ocal
    S_R=""; for R in $SERVICES; do [[ ${R:0:1} == "R" ]] && S_R="${S_R} ${R}"; done
    S_L=""; for L in $SERVICES; do [[ ${L:0:1} == "L" ]] && S_L="${S_L} ${L}"; done
    # setting flags for zookeeper testing
    if [[ ${S_L} == *"ZOOKEEPER"* ]]; then
        local zk_test=true
        local zk_leader_found=false
        local zk_follower_found=false
    else
        local zk_test=false
    fi
    for host in $SERVER_LIST; do
        local net_fail=false
        logit --text "[${host}]${H_SPC}Basic network connectivity\n" -fs "INFO"
        # check dns
        local retval=$(check_dns_resolve $host)
        if [[ $retval == 0 ]]; then
            logit --text "$(text_align "[${host}]${R_SPC}DNS resolution")" -fs INFO
            logit --state "Passed" -fs
        else
            logit --text "$(text_align "[${host}]${R_SPC}DNS resolution")" -fs ERROR
            logit --state "Failed" -fs
            $ERR_REPORT && \
            CLUSTER_ERRORS[${#CLUSTER_ERRORS[@]}]="[${host}] [ERROR]${R_SPC}DNS resolution"
        fi
        # check remote services
        for svc in $S_R; do
            local net_fail=false
            local port=$(echo $svc | cut -d: -f2)
            local service_name=$(echo $svc | cut -d: -f3)
            local retval=$(check_services $host $port $service_name)
            [[ $retval == 0 ]] && local severity="INFO" || local severity="ERROR"
            logit --text "$(text_align "[${host}]${R_SPC}$service_name on port: $port")" -fs $severity
            case $port in
                0)
                    if [[ $retval == 0 ]]; then
                        logit --state "Passed" -fs
                        # if ping OK we verify network packet loss
                        local retval=$(check_network_consistency $host)
                        if [[ $retval == 0 ]]; then
                            logit --text "$(text_align "[${host}]${R_SPC}network consistency (% packet loss): ")" -fs INFO
                            logit --state "Passed" -fs
                        else
                            logit --text "$(text_align "[${host}]${R_SPC}network consistency (% packet loss): ")" -fs ERROR
                            logit --state "Failed" -fs
                            $ERR_REPORT && \
                            CLUSTER_ERRORS[${#CLUSTER_ERRORS[@]}]="[${host}] [ERROR]${R_SPC}Failed network consistency test"
                        fi
                    else
                        logit --state "Failed" -fs
                        $ERR_REPORT && \
                        CLUSTER_ERRORS[${#CLUSTER_ERRORS[@]}]="[${host}] [ERROR]${R_SPC}No PING to host"
                        # if ping failed we print packet loss FAIL
                        logit --text "$(text_align "[${host}]${R_SPC}network consistency (% packet loss): ")" -fs ERROR
                        logit --state "Failed" -fs
                        $ERR_REPORT && \
                        CLUSTER_ERRORS[${#CLUSTER_ERRORS[@]}]="[${host}] \
                        [ERROR]${R_SPC}Failed network consistency test"
                    fi
                    ;;
                22)
                    if [[ $retval == 0 ]]; then
                        logit --state "Passed" -fs
                    else
                        logit --state "Failed" -fs
                        $ERR_REPORT && \
                        CLUSTER_ERRORS[${#CLUSTER_ERRORS[@]}]="[${host}] \
                        [ERROR]${R_SPC}Unable to establish SSH connection"
                        local net_fail=true
                        break
                    fi
                    ;;
            esac
        done
        # if remote checks are bad we go to next host
        if $net_fail; then
            logit --text "\n" -s
            continue
        fi
        # check NTP
        logit --text "$(text_align "[${host}]${R_SPC}Time synchronization")" -fs INFO
        if [[ $(check_time $host) -eq 0 ]]; then
            logit --state "Passed" -fs
        else
            logit --state "Failed" -fs
        fi
        # check NFS mount
        local retval=$(ssh $host "findmnt /dbagigashare > /dev/null" ; echo $?)
        if [[ $retval == 0 ]]; then
            logit --text "$(text_align "[${host}]${R_SPC}NFS mount check")" -fs INFO
            logit --state "Passed" -fs
        else
            logit --text "$(text_align "[${host}]${R_SPC}NFS mount check")" -fs ERROR
            logit --state "Failed" -fs
            $ERR_REPORT && \
            CLUSTER_ERRORS[${#CLUSTER_ERRORS[@]}]="[${host}] [ERROR]${R_SPC}NFS mount check"
        fi
        logit --text "[${host}]${H_SPC}Role related services\n" -fs "INFO"
        for svc in $S_L; do
            local port=$(echo $svc | cut -d: -f2)
            local service_name=$(echo $svc | cut -d: -f3)
            if ! [[ $port =~ $is_number ]] ; then
                check_system_service $host $port $service_name
                continue
            else
                local retval=$(check_services $host $port $service_name)
            fi
            if [[ $retval == 0 ]]; then
                logit --text "$(text_align "[${host}]${R_SPC}$service_name on port: $port")" -fs INFO
                # checking if zookeeper related ports
                if [[ $port == 2888 ]]; then
                    logit --state "Leader" -fs
                    zk_leader_found=true
                else
                    logit --state "Up" -fs
                fi
            else
                logit --text "$(text_align "[${host}]${R_SPC}$service_name on port: $port")" -fs ERROR
                if [[ $port == 2888 ]]; then
                    logit --state "Follower" -fs
                    zk_follower_found=true
                elif [[ $port == 2181 ]] || [[ $port == 3888 ]]; then
                    logit --state "Down" -fs
                else
                    logit --state "Down" -fs
                    $ERR_REPORT && \
                    CLUSTER_ERRORS[${#CLUSTER_ERRORS[@]}]="[${host}] [ERROR]${R_SPC}$service_name on port: $port"
                fi
            fi
            case $port in
                2050) check_cr8_status $host ;;
            esac
        done
    # add type specific checks  here
        case $type in
            -c) get_kafka_status $host ;;
        esac
        logit --text "\n" -s
    done
    # if no zookeeper leader was found we print an error
    if $zk_test && $zk_follower_found && ! $zk_leader_found; then
        local cluster_name=$(echo ${ENV_NAME^^} | sed -e 's/SERVER.*//' -e 's/ $//')
        $ERR_REPORT && \
        CLUSTER_ERRORS[${#CLUSTER_ERRORS[@]}]="[ZOOKEEPER]     [ERROR]${R_SPC}${cluster_name} cluster has no leader!"
    fi
    # check runtime errors per cluster
    if [[ ${#CLUSTER_ERRORS[@]} -gt 1 ]]; then
        for k in ${!CLUSTER_ERRORS[@]}; do
            if [[ ${#ERRORS[@]} -eq 0 ]]; then
                ERRORS[0]="${CLUSTER_ERRORS[$k]}"
            else
                ERRORS[$[${#ERRORS[@]}+1]]="${CLUSTER_ERRORS[$k]}"
            fi
        done
        unset CLUSTER_ERRORS
        ERRORS[$[${#ERRORS[@]}+1]]=""
    fi
}


### MAIN ###
#

### global veriables ###
GS_ROOT="/dbagiga"
LOGS_DIR="/dbagigalogs"
UTILS_DIR="${GS_ROOT}/utils"
LOG_FILE="${LOGS_DIR}/sanity/sanity.log"
CONFIG_FILE="${UTILS_DIR}/runall/runall.conf"
RTID=$RANDOM
TMP_DIR="/tmp/runall_err.${RTID}"
declare -A ERRORS

### text style ###
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
### text spacers ###
H_SPC="    "      # header spacer
R_SPC="        "  # row spacer

### cluster keys ###
ENV_TYPES="-s -m -c -d -na -nm -p -cp"

# check if host.yaml exists
if [[ ! -e ${ENV_CONFIG}/host.yaml ]]; then
    echo "ERROR: '${ENV_CONFIG}/host.yaml' could not be found!"
    exit
fi

# abort if conf file is missing
if [[ ! -e $CONFIG_FILE ]]; then
    echo "[WARNING] missing '$CONFIG_FILE'" ; exit
else
    # remove carriage return characters from config file
    sed -i 's/\r$//g' $CONFIG_FILE
fi

# parse arguments
list_servers=false
hardware_check=false
health_check=false
is_scp=false
ERR_REPORT=true

while [[ $# -gt 0 ]]; do
    case $1 in
        -s|-m|-a|-c|-d|-na|-nm|-n|-p|-cp|-A) type="$1" ;;
        -l) list_servers=true ;;
        -v) printf "runall.sh v${VERSION}\n" ; exit ;;
        copy)   scp_sources=()
                shift
                if [[ $1 == '-src' ]]; then
                    while true; do
                        shift
                        if [[ $1 == '-tgt' ]]; then
                            shift
                            scp_target="$1"
                            break
                        fi
                        scp_sources+=("$1")
                    done
                fi
                [[ -z $scp_sources ]] && usage err0 && exit
                is_scp=true
                ;;
        -hc) health_check=true ;;
        -hw*)
            hardware_check=true
            this_method=$(echo $1 | cut -d. -f2 | cut -d= -f1)
            if [[ ${#hw_methods[@]} -eq 0 ]]; then
                if [[ ${this_method} != '-hw' ]] && [[ ${this_method} != "" ]]; then
                    hw_methods[0]=$1
                fi
            else
                next_element=$[${#hw_methods[@]} + 1]
                hw_methods[${next_element}]=$1
            fi
            ;;
        -q) ERR_REPORT=false ;;
        -h|--help) usage ; exit ;;
        *)
            if [[ ! -z $type ]]; then
                get_targeted_servers $type
                allcmd="$1"
                for node in $SERVER_LIST; do
                    echo
                    logit --text "$(text_align "Executing command(s) on :: $node" "--title")\n" -s
                    ssh "${node}" "${allcmd}" ; echo
                done
            else
                usage err0 ; exit
            fi
    esac
    shift
done

if $list_servers; then
    [[ -z $type ]] && list_all_servers || list_all_servers $type
    exit
fi

if $is_scp; then
    if [[ -z $type ]]; then
        echo "ERROR: missing option for cluster type"
        exit 1
    fi
    for src in ${scp_sources[@]}; do
        if [[ ! -d $src && ! -f $src ]]; then
            echo "ERROR: invalid source path '$src'"
            exit
        fi
    done
    if ! [[ "$scp_target" == *"/"* ]]; then
        echo "ERROR: invlid target path"
        exit
    fi
    get_targeted_servers $type
    for src in ${scp_sources[@]}; do
        secure_copy $src $scp_target
    done
fi

if $health_check; then
    START_TIME=$(date +"%Y-%m-%d %H:%M:%S")
    if [[ ! -z $type ]] ; then
        case $type in
            -a) ENV_TYPES="-s -m" ;;
            -n) ENV_TYPES="-na -nm" ;;
            *)  ENV_TYPES=$type
        esac
    fi
    for type in $ENV_TYPES; do run_health_checks $type; done
    END_TIME=$(date +"%Y-%m-%d %H:%M:%S")
  # display errors report
  $ERR_REPORT && print_errors_report
    echo
    exit
fi
if $hardware_check; then
    if [[ -z $type ]]; then
        for type in $ENV_TYPES; do show_hw_report $type; done
    else
        show_hw_report $type
    fi
    exit
fi

exit
