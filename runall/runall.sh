#!/bin/bash

#
# runall.sh (v2.0)
# - execute command(s) on ODS servers via ssh
# - run health checks (connctivity/application/configuration)
# 
# By Alon Segal, Dec 2021
#

function usage() {

	echo
	echo "Usage: runall.sh [option] [command]"
	echo
	echo -e "   -s\t\tConnect to ODS space servers only"
	echo -e "   -m\t\tConnect to ODS management servers only"
	echo -e "   -a\t\tConnect to ODS space + management servers"
	echo -e "   -c\t\tConnect only to DI servers (not including DI pivot)"
	echo -e "   -na\t\tConnect to northbound application servers"
	echo -e "   -nm\t\tConnect to northbound management servers"
	echo -e "   -n\t\tConnect to northbound application + management servers"
	echo -e "   -p\t\tConnect only to pivot/admin server"
	echo -e "   -l\t\tList all nodes by their respective clusters"
	echo -e "   -A\t\tConnect to all servers (including pivot server)"
	echo -e "   -hc\t\tRun health checks on all servers"
	echo -e "   -h, --help\tDisplay this help screen"
	echo
}

function get_targeted_servers() {
	# we populate SERVER_LIST and SERVICES according to the environment type
	# ENV_NAME and SERVICES only apply to a single cluster NOT an augmentation of clusters
	# ENV_NAME :: a name for the cluster for interactive output purposes
	# SERVER_LIST :: space delimited list of servers
	# SERVICES Syntax :: <Test [R]emotly/[L]ocaly>:<PORT-NUMBER>:<SERVICE-NAME> (e.g. R:443:HTTPS)
	#
	# NOTE! - [R]emote services (e.g. network connectivity) are tested before other services.
	
	case $1 in	
		-l)
			list_all_servers ; exit
			;;
		-s)
			ENV_NAME="ODS space servers"
			SERVER_LIST=""
			SERVICES="R:0:PING R:22:SSH L:8600:CONSUL-DNS L:8500:CONSUL-HTTP-API L:8301:CONSUL-SERF-LAN"
			;;
		-m)
			ENV_NAME="ODS management servers"
			SERVER_LIST=""
			SERVICES="R:0:PING R:22:SSH L:4174:GS-LRMI(LUS) L:8099:WEB-UI L:8090:OPS-MANAGER-UI"
			;;
		-a)
			SERVER_LIST=""
			;;
		-c)
			ENV_NAME="DI servers"
			SERVER_LIST=""
			SERVICES="R:0:PING R:22:SSH L:2050:CR8 L:27017:MONGODB L:9092:KAFKA \
			L:2181:ZOOKEEPER-CLIENT-PORT L:2888:ZOOKEEPER-PEERS L:3888:ZOOKEEPER-LEADER-ELECT"
			;;
		-na)
			ENV_NAME="northbound application servers"
			SERVER_LIST=""
			SERVICES="R:0:PING R:22:SSH L:8080:NGINX L:8443:NGINX L:8055:NGINX-LB L:8300:CONSUL-SERVER-RPC L:8301:CONSUL-SERF-LAN L:8302:CONSUL-SERF-WAN L:8600:CONSUL-DNS L:8500:CONSUL-HTTP-API"
			;;
		-nm)
			ENV_NAME="northbound management servers"
			SERVER_LIST=""
			SERVICES="R:0:PING R:22:SSH"
			;;
		-n)
			SERVER_LIST=""
			;;
		-p)
			ENV_NAME="Pivot server(s)"
			SERVER_LIST=""
			SERVICES="L:22:SSH L:8088:INFLUXDB L:3000:GRAFANA-SERVER L:9992:KAPACITOR"
			;;
		-A)
			SERVER_LIST=""
			;;
		*)
			echo "invalid option or bad syntax."
			usage
	esac
}

function logger() {
	# This function will display output and write to log file
	
	local log_text=$1
	local log_type=$2
	local log_file="$(dirname $0)/runall_health_check.log"
	local ts="[ $(date +"%a %D %H:%M:%S") ]"
	case $log_type in
		-a)	# log to both terminal and file
			echo -e "${log_text}"
			echo -e "${ts}${log_text}" >> $log_file
			;;
		-f)	# log to file only
			echo -e "${ts}${log_text}" >> $log_file
			;;
		*)	# output text to terminal only
			echo -e "${log_text}"
	esac
}

function list_all_servers() {
	# list all servers by cluster
	
	for type in $ENV_TYPES; do
		get_targeted_servers $type
		echo ; logger "${_BT}$(text_align "${ENV_NAME^^}" "--title")${_NT}" -a
		for node in $SERVER_LIST; do
			echo " $node"
		done
	done
	echo
}

function text_align() {
	# align text presentation according to string length.
	# *** change 'row_width' value as neccessary to control output alignment *** 
	
	local s=$1
	local row_width=70
	case $2 in
		--title)
				local w=$(( $(( $row_width - ${#s} )) / 2 ))
				for i in $(seq 1 $w); do chars="${chars}$(echo -n "#")"; done
				echo "${chars} ${s} ${chars}"
				;;
		--params)
				local w=$(($row_width - ${#s}))
				for i in $(seq 1 $w); do chars="${chars}$(echo -n " ")"; done
				echo "${s}${chars}\t"
				;;
		*)
				local w=$(($row_width - ${#s}))
				for i in $(seq 1 $w); do chars="${chars}$(echo -n ".")"; done
				echo "${s}${chars}\t"
	esac
}

function check_services() {
	# check service(s) status
	
	local host="$1"
	local port="$2"
	local service_name="$3"
	local status=""
	local retval=""
	case $port in
		0) ping -c 1 $host >/dev/null 2>&1 ; echo $? ;;
		22) timeout 1 bash -c "echo > /dev/tcp/$host/$port" > /dev/null 2>&1 ; echo $? ;;
		*) retval=$(ssh $host "netstat -ntl | grep $port >/dev/null 2>&1" ; echo $?) ; echo $retval ;;
	esac
}

function status_print() {
	# print the status according to exit code from check_services()
	
	local retval=$1
	[[ $retval -eq 0 ]] && status="${_GC}[ OK ]${_NC}" || status="${_RC}[ FAIL ]${_NC}"
	echo $status
}

function get_kafka_status() {
	# using kafka's utility to check its cluster health
	
	local host=$1
	local kafka_home="/opt/Kafka/kafka_2.13-2.8.1"	
	rstr="$(ssh $host "$kafka_home/bin/zookeeper-shell.sh localhost:2181 ls /brokers/ids" | tail -1)"
	[[ $rstr == "[1, 2, 3]" ]] && status="${_GC}[ OK ]${_NC}" || status="${_RC}[ FAIL ]${_NC}"
	logger "$(text_align "kafka cluster status (members: $rstr)")${status}" -a
}

function get_system_profile() {
	# enumerate number of cpu cores
	
	local host=$1
	# get number of cpu cores
	num_cpu_cores=$(ssh $host "grep processor /proc/cpuinfo | wc -l")
	logger "$(text_align "   CPU :: # of cores: $num_cpu_cores" "--params")" -a
	
	# get total amount of memory
	rstr="$(ssh $host "grep -i 'MemTotal' /proc/meminfo | sed 's/ //g' | sed 's/kB//g' | sed 's/MemTotal\://g'")"
	logger "$(text_align "   RAM :: Total amount of memory: $(($rstr/1000/1000))GB" "--params")" -a
	
	# get used capacity for root volume
	rstr=$(ssh $host "df -h / | tail -1")
	hdd_dev="$(echo $rstr | awk '{print $1}')"
	hdd_root_cap=$(echo $rstr | awk '{print $5}' | sed 's/%//g')
	[[ $hdd_root_cap -gt 75 ]] && hdd_root_cap_colored="${_RC}${hdd_root_cap}%${_NC}" \
	|| hdd_root_cap_colored="${_GC}${hdd_root_cap}%${_NC}" 
	logger "$(text_align "   STORAGE :: root volume device: $hdd_dev" "--params")" -a
	logger "$(text_align "   STORAGE :: root volume capacity: $hdd_root_cap_colored used" "--params")" -a
}

function get_mem_usage() {
	# get memory usage
	
	local host=$1
	rstr="$(ssh $host "grep -i 'MemFree\|Buffers\|Cached\|SwapTotal\|SwapFree' /proc/meminfo | sed 's/ //g'")"
	logger "$(text_align "Average memory usage" "--params")" -a
	for s in $rstr; do logger "$(text_align "   ${s}" "--params")" -a; done
}

function get_cpu_load() {
	# get cpu load for the past 1m, 5m and 15m
	 
	local host=$1 
	rstr="$(ssh $host "cat /proc/loadavg")"
	load="$(echo $rstr | awk '{print "1m:"$1 " 5m:"$2 " 15m:"$3}')"
	logger "$(text_align "Average CPU load" "--params")" -a
	logger "$(text_align "   ${load}" "--params")" -a
}

function run_health_checks() {
	# traverse the different environments
	# and run health checks according to related SERVICES
	
	for type in $ENV_TYPES; do
		get_targeted_servers $type
		local env_title=""
		echo ; logger "${_BT}$(text_align "Checking ${ENV_NAME^^}" "--title")${_NT}" -a
		# we devide services to [R]emote and [L]ocal
		S_R=""; for R in $SERVICES; do [[ ${R:0:1} == "R" ]] && S_R="${S_R} ${R}"; done
		S_L=""; for L in $SERVICES; do [[ ${L:0:1} == "L" ]] && S_L="${S_L} ${L}"; done
		for host in $SERVER_LIST; do
			logger "${_BT}>>> $host${_NT}" -a 
			logger "--- Basic network connectivity ---" -a
			for svc in $S_R; do
				port=$(echo $svc | cut -d: -f2)
				service_name=$(echo $svc | cut -d: -f3)
				retval=$(check_services $host $port $service_name)
				case $port in
					0)
						logger "$(text_align "   network connectivity ($service_name)")$(status_print $retval)" -a
						if [[ $retval -ne 0 ]]; then
							logger "${_RC} *** HOST UNREACHABLE. SKIPPING... ***${_NC}" -a ; logger "" -a
							break
						fi
						;;
					22)
						logger "$(text_align "   service $service_name on port: $port")$(status_print $retval)" -a
						if [[ $retval -ne 0 ]]; then
							logger "${_RC} *** UNABLE TO CONNECT TO HOST VIA SSH. SKIPPING... ***${_NC}" -a ; logger "" -a
							break
						fi
						;;
				esac
			done
			[[ $retval -ne 0 ]] && continue
			# show host configuration profile
			logger "" -a ; logger "--- System profile ---" -a
			get_system_profile $host
			# show resources status
			logger "" -a ; logger "--- Generic system load status ---" -a
			get_cpu_load $host
			get_mem_usage $host
			logger "" -a ; logger "--- Role related services ---" -a
			for svc in $S_L; do
				port=$(echo $svc | cut -d: -f2)
				service_name=$(echo $svc | cut -d: -f3)
				retval=$(check_services $host $port $service_name)
				logger "$(text_align "   service $service_name on port: $port")$(status_print $retval)" -a
			done
			case $type in	# add type specific checks (functions) here 
				-c) 
					get_kafka_status $host
					;;
			esac
			logger "" -a
		done
		echo
	done
}

#
#
### MAIN ###
#
#

# keys/switches of the clusters in the environment
# as specified in function 'get_targeted_servers()'
ENV_TYPES="-s -m -c -na -nm -p"

# text styling globals
_BT=$(tput bold)
_NT=$(tput sgr0)
_RC='\033[0;31m'
_GC='\033[1;32m'
_NC='\033[0m'

# abort if no arguments supplied
if [[ $# -eq 0 ]]; then
	echo "At least one argument is needed."
	echo "See help [-h, --help] for options."
	echo ; exit
fi

# set 'allcmd' to the 2nd parameter if exists
allcmd="${2:-}"
if [[ $1 == "-hc" ]]; then
	run_health_checks
else
	get_targeted_servers $1
	for node in $SERVER_LIST; do
		echo
		logger "########## Gathering info on node ### $node ##########"
		ssh "${node}" "${allcmd}"
	done
fi
exit
