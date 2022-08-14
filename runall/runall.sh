#!/bin/bash

#
# runall.sh
# - execute command(s) on ODS servers via ssh
# - run health checks (connectivity/application/configuration)
#
# By Alon Segal, Dec 2021
#

VERSION=2.5.2

function usage() {
    printf "\nrunall.sh v$VERSION\n"
    printf "\n%-10s\n%7s%-50s\n" "Usage:" " " "runall.sh [option] [command]"
    printf "\n%-10s\n" "Description:"
    printf "%7s%-50s\n" " " "Execute command(s) on ODS servers via ssh"
    printf "%7s%-50s\n" " " "Run health checks (connectivity/application/configuration)"
    printf "%7s%-50s\n" " " "Note: without options commands will run for all servers"
    case $1 in
        err0)
            printf "At least one argument is needed."
            printf "See help [-h, --help] for options.\n"
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
            printf "%5s%-30s%-50s\n" "" ".capacity=<VOLUME>" "Get capacity stats for the named volume"
            printf "%5s%-30s%-50s\n" "" ".cpu-count" "Get number of CPU cores"
            printf "%5s%-30s%-50s\n" "" ".cpu-load" "Get CPU load stats "
            printf "%5s%-30s%-50s\n" "" ".mem-count" "Get amount of RAM"
            printf "%5s%-30s%-50s\n" "" ".mem-load" "Get memory usage stats"
            printf "%2s%-13s%-50s\n" "" "<command>" "Execute shell command(s) on all hosts for the selected cluster"
            printf "\n"
    esac
}

function get_targeted_servers() {
    case $1 in
        -l) list_all_servers ; exit ;;
        -s) local env_grep="_s_" ; local svc_grep=("_s_") ;;
        -m) local env_grep="_m_" ; local svc_grep=("_m_") ;;
        -a) local env_grep="_a_" ; local svc_grep=("_s_" "_m_") ;;
        -c) local env_grep="_c_" ; local svc_grep=("_c_") ;;
        -d) local env_grep="_d_" ; local svc_grep=("_d_") ;;
        -na) local env_grep="_na_" ; local svc_grep=("_na_") ;;
        -nm) local env_grep="_nm_" ; local svc_grep=("_nm_") ;;
        -n) local env_grep="_n_" ; local svc_grep=("_na_" "_nm_") ;;
        -p) local env_grep="_p_" ; local svc_grep=("_p_") ;;
        -A) local env_grep="_A_" ; local svc_grep=("_s_" "_m_" "_c_" "_d_" "_na_" "_nm_" "_p_") ;;
        *)
            echo "invalid option or bad syntax."
            usage ; exit
    esac
    ENV_NAME="$(cat $CONFIG_FILE | grep "${env_grep}ENV_NAME" | cut -d'=' -f2)"
    [[ ${#svc_grep[@]} -eq 1 ]] && SERVICES="$(cat $CONFIG_FILE | grep "${svc_grep[0]}SERVICES" | cut -d'=' -f2)"
    SERVER_LIST=""
    for s in ${svc_grep[@]}; do
        SERVER_LIST+=" $(cat $CONFIG_FILE | grep "${s}SERVER_LIST" | cut -d'=' -f2)"
    done
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
        get_targeted_servers $type
        logit --text "${bold}$(text_align "${ENV_NAME^^}" "--title")${nbold}\n" -s
		servers=""
		for node in $SERVER_LIST; do
			grep -q $node <<< $servers && continue || servers+="${node} "
        done
		for n in $servers; do echo "$n" | sed 's/ *//g' ; done
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
    # local kafka_home="/opt/Kafka/latest_kafka"
    local kafka_home="/opt/Kafka/kafka_2.13-2.8.1"
    if $(ssh $host "[[ -e "$kafka_home/bin/zookeeper-shell.sh" ]] && echo true || echo false") ; then
        local return_code=$(ssh $host "$kafka_home/bin/zookeeper-shell.sh localhost:2181 ls /brokers/ids" > /dev/null 2>&1 ; echo $?)
        if [[ $return_code -eq 0 ]]; then
            logit --text "$(text_align "[${host}]${R_SPC}Kafka-Zookeeper cluster connection")" -fs INFO
            logit --state "Passed" -fs
            # if kafka-zk connection is ok we check the kafka cluster
            local rstr="$(ssh $host "$kafka_home/bin/zookeeper-shell.sh localhost:2181 ls /brokers/ids" | tail -1)"
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
    logit --text "$(text_align "[${host}]${R_SPC}[RAM] Total amount of memory: \
    $(($rstr/1000/1000))GB" "--params")\n" -fs INFO
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
        elif [[ $hdd_cap -ge 75 ]]; then
            local set_color="${lred}"
            local severity="ERROR"
            $ERR_REPORT && \
            CLUSTER_ERRORS[${#CLUSTER_ERRORS[@]}]="[${host}]${R_SPC}[STORAGE] '${target_vol}' capacity level: $severity"
        elif [[ $hdd_cap -ge 50 ]]; then
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
    local host_name=$1
    local host_ip=$(nslookup -timeout=3 $host_name | grep -v "#53" | awk '/Address/ {print $2}')
    if [[ $host_ip != "" ]]; then
        local lookup=$(nslookup -timeout=3 $host_ip | sed '/^$/d' | sed 's/.*name = //' | cut -d. -f1)
        if [[ $lookup = "" ]]; then
            local err=1
        else
            [[ "${lookup^^}" == "${host_name^^}" ]] && local err=0 || local err=1
        fi
    else
        local err=1
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

function check_systemd_service(){
    # check systemd service exists and active
    local host=$1
    local service_name=$2
    local service_desc=$3
    local retval=$(ssh $host systemctl list-units | grep -o "$service_name" > /dev/null 2>&1 ; echo $?)
    if [[ $retval -ne 0 ]]; then
        logit --text "[${host}]${R_SPC}${service_desc^^} service not deployed!\n" -fs ERROR
        $ERR_REPORT && \
        CLUSTER_ERRORS[${#CLUSTER_ERRORS[@]}]="[${host}] [ERROR]${R_SPC}${service_desc^^} service not deployed!"
    else
        retval=$(ssh $host "systemctl is-active $service_name")
        if [[ $retval == "active" ]]; then
            severity="INFO"
        else
            severity="ERROR"
            $ERR_REPORT && \
            CLUSTER_ERRORS[${#CLUSTER_ERRORS[@]}]="[${host}] [ERROR]${R_SPC}${service_desc^^} service status is: ${retval^}"
        fi
        logit --text "$(text_align "[${host}]${R_SPC}${service_desc^^} service status")" -fs $severity
        logit --state ${retval^} -fs
    fi
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

function run_health_checks() {
    # traverse the different environments
    # and run health checks according to related SERVICES
    local env_type=$1
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
                        CLUSTER_ERRORS[${#CLUSTER_ERRORS[@]}]="[${host}] [ERROR]${R_SPC}Failed network consistency test"
                    fi
                    ;;
                22)
                    if [[ $retval == 0 ]]; then
                        logit --state "Passed" -fs
                    else
                        logit --state "Failed" -fs
                        $ERR_REPORT && \
                        CLUSTER_ERRORS[${#CLUSTER_ERRORS[@]}]="[${host}] [ERROR]${R_SPC}Unable to establish SSH connection"
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
                check_systemd_service $host $port $service_name
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

#
### MAIN ###
#

# import global parameters and functions
INCLUDE="/dbagiga/utils/common/include.sh"
if [[ ! -f $INCLUDE ]]; then
    echo "unable to find required '${INCLUDE}' file. operation aborted."
    exit 1
else
    #sed -i 's/\r$//g' $INCLUDE
    source $INCLUDE
fi

LOG_FILE="/var/log/ods_sanity.log"
CONFIG_FILE="/etc/runall/runall.conf"
RTID=$RANDOM
TMP_DIR="/tmp/runall_err.${RTID}"
declare -A ERRORS

# keys/switches of the clusters in the environment
# as specified in function 'get_targeted_servers()'
ENV_TYPES="-s -m -c -d -na -nm -p"

# text styling globals
H_SPC="    "      # header spacer
R_SPC="        "  # row spacer

# abort if conf file is missing
if [[ ! -e $CONFIG_FILE ]]; then
    echo "missing configuration file runall.conf" ; exit
else
    # remove carriage return characters from config file
    sed -i 's/\r$//g' $CONFIG_FILE
fi

# parse arguments
list_servers=false
hardware_check=false
health_check=false
ERR_REPORT=true
while [[ $# -gt 0 ]]; do
    case $1 in
        -s|-m|-a|-c|-d|-na|-nm|-n|-p|-A) type="$1" ;;
        -l) list_servers=true ;;
        -v) printf "runall.sh v${VERSION}\n" ; exit ;;
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
                    logit --text "$(text_align "Gathering info on node :: $node" "--title")\n" -s
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
