#!/bin/bash
do_env() {
source ~/.bash_profile
# Get user/pass creds
_USER=$(awk -F= '/app.manager.security.username=/ {print $2}' ${ENV_CONFIG}/app.config)
if grep '^app.vault.use=true' ${ENV_CONFIG}/app.config > /dev/null ; then
  _VAULT_PASS=$(awk -F= '/app.manager.security.password.vault=/ {print $2}' ${ENV_CONFIG}/app.config)
  _PASS=$(java -Dapp.db.path=/dbagigawork/sqlite/ -jar /dbagigashare/current/gs/jars/gs-vault-1.0-SNAPSHOT-jar-with-dependencies.jar --get ${_VAULT_PASS})
else
  _PASS=$(awk -F= '/app.manager.security.password=/ {print $2}' ${ENV_CONFIG}/app.config)
fi

_DI_HOST=$(runall -d -l | grep -v === | head -1)
_DISK_USAGE_PERCENT=75
_SLEEP_AFTER=1
_MODE="daily"
_VERBOSE=""
_MANAGERS=( $( runall -m -l | grep -v === ) )
_MGR="${_MANAGERS[0]}"                     # target of single-manager REST queries - override with -m
_SPACE_SERVERS=( $( runall -s -l | grep -v === ) )
_ALL_HOSTS=( $( runall -A -l | grep -iv "===\|x\.x" ) )
_SPACE_NAME=dih-tau-service
_PRINT=""
# NB
_ENV_KEY=""
_ENV_CERT=""
_ENV_CACERT=""
_SSL_DIR=""

}

usage() {
  cat << EOF

  USAGE:

   $(basename $0) [<options>]

  OPTIONS:

    -d                DAILY report checks - default (no arguments)
    -v                VERBOSE
    -m <manager>      Send single-manager REST queries to <manager>.
                      Default is the first host returned by "runall -m -l".
    -c <number>       Specify one CHECK by number
    -lc               LIST of single CHECKS
    -ls               LIST of service names
    -p                Print command to be executed (single queries)
    -h                Display HELP/USAGE

  NOTE:

    -c, -lc, -ls and -h stop the script, so they must come LAST.
    Put -d, -r, -v, -p, -q and -m before them - anything placed after them is
    rejected with an error instead of being silently ignored.

  EXAMPLES:

   $(basename $0) -c 6                       # QUERY services only
   $(basename $0) -v -c 11                   # CHECK today's Control-M log entries
   $(basename $0) -v -c 8                    # CHECK pipelines
   $(basename $0) -p -c 16 program           # PRINT command: single query for program_study_service
   $(basename $0) -m gsprod-manager3 -c 24   # CHECK index counts against manager3

EOF
exit
}

bad_args() {
  echo -e "\n ${1}\n Run '$(basename $0) -h' for usage.\n" >&2
  exit 1
}

# -c, -lc, -ls and -h all end in "exit", so do_menu never parses anything that
# follows them. Walk the argument list the same way do_menu does and refuse to
# run rather than silently ignore those arguments.
validate_args() {
  local -a args=( "${@}" )
  local -i i=0 argc=${#args[@]}
  local arg check_num

  while [[ $i -lt $argc ]] ; do
    arg="${args[i]}"
    case $arg in
      "-p"|"-d"|"-r"|"-v"|"-q")
        (( i++ ))
        ;;
      "-m")
        [[ $((i+1)) -lt $argc ]] || bad_args "Option -m requires a manager host name."
        [[ ${args[i+1]} == -* ]] && bad_args "Option -m requires a manager host name, got '${args[i+1]}'."
        (( i += 2 ))
        ;;
      "-c")
        [[ $((i+1)) -lt $argc ]] || bad_args "Option -c requires a check number."
        check_num="${args[i+1]}"
        (( i += 2 ))
        # Checks 3, 16 and 23 accept one optional argument of their own
        case ${check_num} in
          3|16) [[ $i -lt $argc && ${args[i]} != -* ]] && (( i++ )) ;;
          23)   [[ $i -lt $argc && ( ${args[i]} == "-l" || ${args[i]} == "-c" ) ]] && (( i++ )) ;;
        esac
        [[ $i -lt $argc ]] && bad_args "'-c ${check_num}' exits the script - these arguments would be ignored: ${args[*]:i}"
        return 0
        ;;
      "-lc"|"-ls"|"-h")
        (( i++ ))
        [[ $i -lt $argc ]] && bad_args "'${arg}' exits the script - these arguments would be ignored: ${args[*]:i}"
        return 0
        ;;
      *)
        bad_args "Option ${arg} not supported."
        ;;
    esac
  done
  return 0
}

do_title() {
  echo -e "\n===================="
  echo -e "==================== Start checking ${ENV_NAME} environment. $(date)"
  echo -e "====================\n"
}

do_menu() {
  #[[ $# -eq 0 ]] && usage
  while [[ $# -gt 0 ]] ; do
    case $1 in
      "-p")
        _PRINT="yes"
        ;;
      "-d")
        _MODE="daily"
        ;;
      "-r")
        _MODE="regular"
        ;;
      "-c")
        do_one_check "${@}"
        ;;
      "-lc")
        list_of_checks
        ;;
      "-ls")
        list_service_names ; exit
        ;;
      "-m")
        local mng found=""
        for mng in ${_MANAGERS[@]} ; do
          [[ "${mng}" == "${2}" || "${mng%%.*}" == "${2%%.*}" ]] && { found="${mng}" ; break ; }
        done
        [[ -z ${found} ]] && bad_args "Manager '${2}' is not one of: ${_MANAGERS[*]}"
        _MGR="${found}"
        shift
        ;;
      "-v")
        _VERBOSE="yes"
        ;;
      "-q")
        _QUIET="-q"
        ;;
      "-h")
        usage
        ;;
      *)
        echo -e "\n Option $1 not supported.\n" ; exit 1
        ;;
    esac
    shift
  done
}

list_of_checks() {
  cat << EOF

  1   check_ping
  2   check_ssh
  3   check_disk_usage_all [<percent>]            # Specify percent - default is 70
  4   show_primary_backup
  5   service_hc
  6   service_query
  7   check_feeders
  8   check_pipelines
  9   check_sanity_errors
  10  check_all_sanity_of_today
  11  check_ctm
  12  list_types
  13  run_pipelines_bg ; show_pipelines_bg
  14  check_notifiers
  15  list_service_names
  16  query_one_service <service name>            # $(basename $0) -c 16 program_study 
  17  check_gigashare
  18  person_schedule_query_2
  19  check_nbapp_services
  20  check_sync_of_managers
  21  check_spacedeck_on_managers
  22  check_nbagent_services
  23  check_indexes_for_dv_max                     # "-l" insert into log, "-c" show count
  24  check_indexes_count                          # Show tablename and index count
  25  check_indexes_count_less_than_2              # Show tables with index count less than 2 except SHOB_GA
  26  check_mng_connection                         # Show check manager connection files if exist (tesst -f)


EOF
exit
}

do_one_check() {
  case $2 in
    "1")  echo ; check_ping ;;
    "2")  echo ; check_ssh ;;
    "3")  shift 2 ; check_disk_usage_all "${1}" ;;
    "4")  show_primary_backup ;;
    "5")  service_hc ;;
    "6")  service_query ;;
    "7")  check_feeders ;;
    "8")  check_pipelines ;;
    "9")  check_sanity_errors ;;
    "10") check_all_sanity_of_today ;;
    "11") check_ctm ;;
    "12") list_types ;;
    "13") run_pipelines_bg ; show_pipelines_bg ;;
    "14") check_notifiers ;;
    "15") list_service_names ;;
    "16") query_one_service $3 ;;
    "17") check_gigashare ;;
    "18") person_schedule_query_2 ;;
    "19") check_nbapp_services ;;
    "20") check_sync_of_managers ;;
    "21") check_spacedeck_on_managers ;;
    "22") check_nbagent_services ;;
    "23") check_indexes_for_dv_max $3 ;;
    "24") check_indexes_count ;;
    "25") check_indexes_count_less_than_2 ;;
    "26") check_mng_connection ;;
    *)    echo -e "\nChoice unknown" ;;
  esac
  echo
  exit
}

list_service_names() {
  awk -F'8443/' 'NF>1{print $2}' /giga/microservices/curls | sed 's/\/.*$//' | sort -u | sed '=' | sed 'N;s/\n/\t/' 
}

query_one_service() {

  get_certs
  # Query service in /giga/microservices/curls
  microservice=$(grep $1 /giga/microservices/curls | tail -1)
  local service_string=${microservice##*8443/}
  local service_name=${service_string%%/*}

  # Only print command
  if [[ -n "${_PRINT}" ]] ; then
    echo -e "curl --max-time 10 -s --key $_ENV_KEY --cert $_ENV_CERT --cacert $_ENV_CACERT \"https://${_END_POINT}:8443/${service_string}\"" 
    return
  fi

  # Non verbose
  if [[ -z "${_VERBOSE}" ]] ; then 
    local result=$( { time curl --max-time 10 -s --key $_ENV_KEY --cert $_ENV_CERT --cacert $_ENV_CACERT "https://${_END_POINT}:8443/${service_string}" | jq '.res | if length == 0 then "===== empty response" else "data returned" end' ; } 2>&1 )
    printf '%3d %-45s%-25s%s\n' "$((++srv_num))" "${service_name}" "$(echo -e "${result}" | grep -v '^real' | grep -v '^user' | grep -v '^sys')" "$(echo "${result}" | grep ^real | awk '{print $2}')"
    return 
  fi

  # Verbose
  printf '=%.0s' {1..50} ; echo " $((++srv_num)) ${service_name}"
  time curl --max-time 10 -s --key $_ENV_KEY --cert $_ENV_CERT --cacert $_ENV_CACERT "https://${_END_POINT}:8443/${service_string}" | sed '$a\' ; echo
}

# ssl_dir="/gigashare/env_config/nb/applicative/ssl-2025/client" env_prefix="test" _END_POINT="dih-test.tau.ac.il" ; cd $ssl_dir ; env_key="${env_prefix}-client.key" env_cert="${env_prefix}-client.cer" env_cacert="tau-msca-2025.crt"
# Query health
# ssl_dir="/gigashare/env_config/nb/applicative/ssl-2025/client" env_prefix="test" _END_POINT="dih-test.tau.ac.il"
# env_key="${env_prefix}-client.key" env_cert="${env_prefix}-client.cer" env_cacert="tau-msca-2025.crt"
# cd $ssl_dir ; service_name=person_tziun_kurs_service 
# printf "%3d %-35s%s\n" "$(( ++srv_num ))" "${service_name}:" "$(curl -s --key $env_key --cert $env_cert --cacert $env_cacert "https://${_END_POINT}:8443/${service_name}/v1/actuator/health")"
service_hc() {
  #[[ "${_QUIET}" != "-q" ]] && { echo ; read -sn1 -p "Press any key to display health check for services." ; echo ; }
  local srv_num=0
  local registered_services=$(ssh $(runall -s -l |grep -v == |head -1) 'consul catalog services' | grep -vw 'consul')
  local defined_services=$(awk -F'8443/' 'NF>1{print $2}' /giga/microservices/curls | sed 's/\/.*$//' | sort -u | wc -l)
  echo -e "\n==================== Display health check of $(echo $registered_services | wc -w)/$defined_services services\n"
  get_certs
  for service_name in $registered_services ; do
    printf "%3d %-45s%s\n" "$(( ++srv_num ))" "${service_name}:" "$(curl -s --key $_ENV_KEY --cert $_ENV_CERT --cacert $_ENV_CACERT "https://${_END_POINT}:8443/${service_name}/v1/actuator/health")"
  done
  #sleep $_SLEEP_AFTER
}

function get_certs() {

  case ${ENV_NAME} in
    "TAUG") _SSL_DIR="${ENV_CONFIG}/nb/applicative/ssl-2025/client" _END_POINT="dih-nb-dev.tau.ac.il" ;; 
    "TAUS") _SSL_DIR="${ENV_CONFIG}/nb/applicative/ssl-2025/client" _END_POINT="dih-test.tau.ac.il" ;;
    "TAUP") _SSL_DIR="${ENV_CONFIG}/nb/applicative/ssl-2025/client" _END_POINT="dih.tau.ac.il" ;;
    *) echo -e "\nEnv not supported.\n" ; exit 1 ;;
  esac
  _ENV_KEY=$( ls -1tr ${_SSL_DIR}/*.key | tail -1 )
  _ENV_CERT=$( ls -1tr ${_SSL_DIR}/*.cer | tail -1 )
  _ENV_CACERT=$( ls -1tr ${_SSL_DIR}/*.crt | tail -1 )
  cd $_SSL_DIR || { echo -e "\nDirectory $_SSL_DIR does not exist.\n" ; exit 1 ; }

}

# DEV: _SSL_DIR="/giga/josh/ssl/dev" env_prefix="dev" end_point="dih-nb-dev.tau.ac.il" ; cd $_SSL_DIR ; env_key="${env_prefix}-client.key" env_cert="${env_prefix}-client.cer" env_cacert="tau-msca-2025.crt"
# STAGE: _SSL_DIR="/gigashare/env_config/nb/applicative/ssl-2025/client" env_prefix="test" end_point="dih-test.tau.ac.il" ; cd $_SSL_DIR ; env_key="${env_prefix}-client.key" env_cert="${env_prefix}-client.cer" env_cacert="tau-msca-2025.crt"
# PROD: _SSL_DIR="/giga/josh/ssl/prd" env_prefix="prod" end_point="dih.tau.ac.il" ; cd $_SSL_DIR ; env_key="${env_prefix}-client.key" env_cert="${env_prefix}-client.cer" env_cacert="tau-msca-2025.crt"
# service=person_schedule ; microservice=$(grep ${service} /giga/microservices/curls) ; service_string=${microservice##*8443/} ;
# time curl --max-time 10 -s --key $env_key --cert $env_cert --cacert $env_cacert "https://${end_point}:8443/${service_string}" | sed '$a\' ; echo

service_query() {
  local srv_num=0             # enumerate service queries
  local services_count=$(curl -s  -u ${_USER}:${_PASS} http://${_MGR}:8090/v2/pus | jq -r '.[].name' | grep '_service$' | grep -v notifier | wc -l)
  echo -e "\n==================== Display data query for all ${services_count} deployed services.\n"
  get_certs

  # Define local variables for below code
  local real_service_name             # Only PU's ending with "_service" except notifiers
  local service_name                  # Versioned (v2, v3) text removed from service for querying purposes
  local microservices_full_string     # holds /giga/microservices/curls full query string
  local service_string                # holds only query string in /giga/microservices/curls e.g. "/get_sem_dates_service/v1/u1?limit=1"
  local deploy_state                  # shows: intact, not intact
  # Check state of every service returned from REST call and query every intact service found in /giga/microservices/curls
  while read real_service_name ; do 
    service_name=$(echo $real_service_name | sed 's/_v[0-9]\+//')
    microservices_full_string=$(grep "/${service_name}/" /giga/microservices/curls)
    deploy_state=$( curl -s -u ${_USER}:${_PASS} http://${_MGR}:8090/v2/pus |jq -r ".[] | select(.name == \"${real_service_name}\") | .status" )
    [[ -z $microservices_full_string ]] && { printf '%3d %-45s%s\n' "$((++srv_num))" "${real_service_name}" "${deploy_state}" ; continue ; }
    service_string=${microservices_full_string##*8443/}
    if [[ -z "${_VERBOSE}" ]] ; then
      local result=$( { time curl --max-time 10 -s --key $_ENV_KEY --cert $_ENV_CERT --cacert $_ENV_CACERT "https://${_END_POINT}:8443/${service_string}" | jq '.res | if length == 0 then "===== empty response" else "data returned" end' ; } 2>&1 )
      printf '%3d %-45s%-25s%-10s%-12s\n' "$((++srv_num))" "${real_service_name}" "$(echo -e "${result}" | grep -v '^real' | grep -v '^user' | grep -v '^sys')" "$(echo "${result}" | grep ^real | awk '{print $2}')" "${deploy_state}"
      continue
    fi
    # Verbose
    printf '=%.0s' {1..50} ; echo " $((++srv_num)) ${real_service_name}"
    time curl --max-time 10 -s --key $_ENV_KEY --cert $_ENV_CERT --cacert $_ENV_CACERT "https://${_END_POINT}:8443/${service_string}" | sed '$a\' ; echo 
  done < <( curl -s  -u ${_USER}:${_PASS} http://${_MGR}:8090/v2/pus | jq -r '.[].name' | grep '_service$' | grep -v notifier )
}

do_entries() {
  #[[ "${_QUIET}" != "-q" ]] && { echo ; read -sn1 -p "Press any key to continue" ;echo ; } || { echo ; echo ; }
  echo -e "\n==================== entries and tieredEntries of all tables\n"
  check-ts-size-of-tables.sh
}

check_sanity() {
  #[[ "${_QUIET}" != "-q" ]] && { echo ; read -sn1 -p "Press any key to check sanity" ;echo ; }
  sleep 1
  echo -e "\n==================== Check sanity\n"
  local exclude_txt='CYBER-ARK-VAULT'        # generic exclude
  local include_txt='fail\|error\|warn\|down\|==========================='
  #exclude per environment in addition to above generic exclude
  case $ENV_NAME in
  # 'DEV' ) exclude_txt=( ${exclude_txt}'\|KAPACITOR' ) ;;
    'TAUG' ) exclude_txt=${exclude_txt}'\|SHOB\|IIDR'
      grep "$(date +%Y-%m-%d)" /dbagigalogs/sanity/sanity.log | grep -i "${include_txt}" | grep -iv "${exclude_txt}" ;;
    'TAUS' ) exclude_txt=${exclude_txt}
      grep "$(date +%Y-%m-%d)" /dbagigalogs/sanity/sanity.log | grep -i "${include_txt}" | grep -iv "${exclude_txt}" ;;
    'TAUP' ) exclude_txt=${exclude_txt}
      grep "$(date +%Y-%m-%d)" /dbagigalogs/sanity/sanity.log | grep -i "${include_txt}" | grep -iv "${exclude_txt}" ;;
  esac
  local num_lines=$(grep "$(date +%Y-%m-%d)" /dbagigalogs/sanity/sanity.log |wc -l)
  echo -e "\nNo. of lines in sanity check: ${num_lines}\n"
  [[ $num_lines -lt 80 ]] && { echo -e "WARNING: check number of lines in sanity check\n" ; sleep 2 ; }
}

run_sanity_bg() {
  remote_file_sanity=/tmp/remote_file_sanity$$
  sanity_pid=$(ssh -f root@localhost "nohup bash -l -c '/giga/utils/sanity/sanity.py dih-tau-space -c 1 > $remote_file_sanity ; echo \$! >&2' > /dev/null 2>&1 & echo \$!")
}

# start sanity Sun Dec 24 21:17:39 IST 2023
# finished bg sanity Sun Dec 24 21:19:22 IST 2023
# root@gstest-pivot:/giga/yuval/TAU# echo "$(date -d 'Sun Dec 24 21:19:22 IST 2023' +%s) - $(date -d 'Sun Dec 24 21:17:39 IST 2023' +%s)" |bc -l
# 103
show_sanity_bg() {
  echo -e "\n==================== Check sanity.\n"
  while ssh root@localhost "kill -0 $sanity_pid 2>/dev/null" ; do
    #echo "Waiting for sanity to finish - $(ssh root@localhost "ps -fp ${sanity_pid}")"
    sleep 5
  done
  sleep 1
  ssh root@localhost "cat ${remote_file_sanity}"
  rm -f ${remote_file_sanity}
}

check_all_sanity_of_today() {
  echo ;  read -t 3 -p "Press ENTER or wait 3 seconds to display sanity lines from today:" ; echo -e "\n"
  #[[ "${_QUIET}" != "-q" ]] && { echo ; read -sn1 -p "Press any key to check all rows of today's sanity" ;echo ; }
  echo -e "\n==================== Check all rows of today's sanity\n"
  grep $(date "+%Y-%m-%d") /gigalogs/sanity/sanity.log
}


check_disk_usage() {
  perc=$2
  ssh "$1" 'bash -s' "${perc}" <<-'ENDSSH'
    perc=$1
    # Run df -h to get disk usage information, then use awk to process the output
    df -h | awk -v threshold="${perc}" '(NR > 1) && ($5+0 > threshold) {printf "%-20s %-40s %-10s %-10s\n", "'$(hostname)'", $6, $5, $2}'
ENDSSH
}

check_disk_usage_all() {
  if [[ -n $1 ]] ; then
    _DISK_USAGE_PERCENT=$1
  fi
  local usage_threshold=$_DISK_USAGE_PERCENT
  echo -e "\n==================== Show DISK USAGE on all servers' partitions above ${_DISK_USAGE_PERCENT}%\n"
  # Print the header of the table
  printf "%-20s %-40s %-10s %-10s\n" "Hostname" "Partition" "Usage (%)" "Total Size"
  for h in $(runall -A -l | grep -iv "====\|x\.x") ; do
    check_disk_usage $h "${usage_threshold}" | grep -v 'docker\|container'
  done
}

#check_disk_usage_all() {
#  #[[ "${_QUIET}" != "-q" ]] && { echo ; read -sn1 -p "Press any key to Show DISK USAGE" ;echo ; }
#  echo -e "\n==================== Show DISK USAGE on all servers' partitions above ${_DISK_USAGE_PERCENT}%\n"
#  runhosts.sh -df $_DISK_USAGE_PERCENT
#  sleep $_SLEEP_AFTER
#}

finish_checking_env() {
  echo -e "===================="
  echo -e "==================== Finished checking ${ENV_NAME} environment. $(date)"
  echo -e "====================\n"
}

list_types() {
  #[[ "${_QUIET}" != "-q" ]] && { echo ; read -sn1 -p "Press any key to list all registered types" ;echo ; }
  echo -e "\n==================== List of all registered types.\n"
  local num_of_types=$(curl -u ${_USER}:${_PASS} -s "http://${_MGR}:8090/v2/internal/spaces/utilization" | jq -r ".[].objectTypes | keys| .[]" | wc -l)
  echo -e "Total number of registered types: ${num_of_types}\n"
  [[ -n $_VERBOSE ]] && curl -u ${_USER}:${_PASS} -s "http://${_MGR}:8090/v2/internal/spaces/utilization" | jq -r ".[].objectTypes | keys| .[]"
  sleep $_SLEEP_AFTER
}

show_primary_backup() {
  #[[ "${_QUIET}" != "-q" ]] && { echo ; read -sn1 -p "Press any key to show no. of PARTITIONS, and PRIMARY/BACKUP GSC's" ; echo ; }
  echo -e "\n==================== Displaying no. of PARTITIONS, and PRIMARY/BACKUP GSC's.\n"
  pb-same-server | sed '/Service name/,$!d'
}

check_ping() {
  echo -e "\n==================== Checking PING to all servers:\n"
  local ping_result="Success"
  echo -en "Ping result: "
  for h in $(runall -A -l | grep -iv '===\|x\.x') ; do
    ping -c1 -w2 $h > /dev/null 2>&1 
    [[ $? -ne 0 ]] && { echo "No ping to host: ${h}" ; ping_result="failed" ; }
  done
  echo -e "${ping_result}."
}

check_ssh() {
  echo -e "\n==================== Checking SSH execution on all servers:\n"
  local ssh_result="Success"  
  echo -en "SSH execution result: "
  for h in $(runall -A -l | grep -iv '===\|x\.x') ; do 
    timeout 5 ssh $h uptime  > /dev/null 2>&1 ; exit_code=$? 
    [[ $exit_code -ne 0 ]] && { echo "ssh execution failed on ${h}, exit_code=$exit_code" ; ssh_result="failed" ; }
  done
  echo -e "${ssh_result}."
}

check_mng_connection() {
  local selected_servers="gsprod-manager1 gsprod-manager2 gsprod-manager3 gsprod-space3 gsprod-space6 gsprod-space10"
  [[ "${ENV_NAME}" != "TAUP" ]] && return 1
  echo -e "\n==================== Checking connectivity between servers:\n"
  local hst
  for hst in $selected_servers ; do
    ssh $hst 'test -f /gigalogs/check-ping-mng.log && { printf "%-17s %s\n" "$(hostname)" "$(ls -ld /gigalogs/check-ping-mng.log)" ; }'
    ssh $hst 'test -f /gigalogs/check-port4174-mng.log && { printf "%-17s %s\n" "$(hostname)" "$(ls -ld /gigalogs/check-port4174-mng.log)" ; }'
    ssh $hst 'test -f /gigalogs/check-zk-mng-errors.log && { printf "%-17s %s\n" "$(hostname)" "$(ls -ld /gigalogs/check-zk-mng-errors.log)" ; }'
  done
  echo -e "\nFIND ERROR: Client session timed out, have not heard from server in ~20000ms - for $(date "+%a %b %d")"
  for hst in ${_SPACE_SERVERS[@]} ${_MANAGERS[@]} ; do
    server_name=$( host ${hst} | awk '{print $NF}' | sed 's/\.$//' )
    echo -e "\n=====${server_name}"
    ssh $server_name 'grep "Client session timed out, have not heard from server in [2-9][0-9]\{4,\}ms" /gigalogs/$(date "+%Y-%m-%d")*.*log*' | wc -l
  done
  [[ -z $_VERBOSE ]] && return 0  
  # VERBOSE
  for hst in $selected_servers ; do
    echo -e "\n=====${hst}"
    ssh $hst 'test -f /gigalogs/check-port-ping-mng.log && sed -E "/$(date "+%a %b %d")/,\$!d" /gigalogs/check-port-ping-mng.log' | grep -v '^$'
    ssh $hst 'test -f /gigalogs/check-port4174-mng.log && sed -E "/$(date "+%a %b %d")/,\$!d" /gigalogs/check-port4174-mng.log' | grep -v '^$'
    ssh $hst 'test -f /gigalogs/check-zk-mng-errors.log && sed -E "/$(date "+%a %b %d")/,\$!d" /gigalogs/check-zk-mng-errors.log' | grep -v '^$'
  done
  for hst in ${_SPACE_SERVERS[@]} ${_MANAGERS[@]} ; do
    server_name=$( host ${hst} | awk '{print $NF}' | sed 's/\.$//' )
    echo -e "\n=====${server_name}"
    ssh $server_name 'grep "Client session timed out, have not heard from server in [2-9][0-9]\{4,\}ms" /gigalogs/$(date "+%Y-%m-%d")*.*log*'
  done
}

check_feeders() {
  echo -e "\n==================== Display feeders without status IN_PROGRESS/SUCCESS/IDLE\n"
  echo -e "Checking Gilboa full feeder"
  auto_gilboafeederfullloadlist | grep -v -- '-----\|Status\|Resources\|DataEngine' | grep -v 'IN_PROGRESS\|SUCCESS\|IDLE'
  echo -e "Checking Gilboa update feeder"
  auto_gilboafeederupdatestatus | grep -v -- '-----\|Status\|Resources\|DataEngine' | grep -v 'IN_PROGRESS\|SUCCESS\|IDLE'
  echo -e "Checking Oracle feeders"
  auto_oraclefeederlist | grep -v -- '-----\|Status\|Resources\|DataEngine' | grep -v 'IN_PROGRESS\|SUCCESS\|IDLE'
  echo -e "Checking Oracle ERP feeders"
  auto_oracleerpfeederlist | grep -v -- '-----\|Status\|Resources\|DataEngine' | grep -v 'IN_PROGRESS\|SUCCESS\|IDLE'
  echo -e "Checking MySQL feeders"
  auto_mysqlfeederlist | grep -v -- '-----\|Status\|Resources\|DataEngine' | grep -v 'IN_PROGRESS\|SUCCESS\|IDLE'
}

run_pipelines_bg() {
  remote_file_pipelines=/tmp/remote_file_pipelines$$
  pipelines_pid=$(ssh -f root@${_DI_HOST} "nohup bash -l -c '/dbagiga/scripts/statusPipelines.sh > $remote_file_pipelines ; echo \$! >&2' > /dev/null 2>&1 & echo \$!")
}

show_pipelines_bg() {
  echo -e "\n==================== Check pipelines.\n"
  echo -ne "Status of pipelines: "
  while ssh root@${_DI_HOST} "kill -0 $pipelines_pid 2>/dev/null" ; do
    #echo "Waiting for sanity to finish - $(ssh root@localhost "ps -fp ${sanity_pid}")"
    sleep 1
  done
  plState=$(ssh root@${_DI_HOST} "cat ${remote_file_pipelines}" | awk -F'/' '/\// {print $1}')
  numOfPl=$(ssh root@${_DI_HOST} "cat ${remote_file_pipelines}" | sed -nE 's#^[0-9]+/([0-9]+) .*$#\1#p')
  [[ $plState -eq $numOfPl ]] && echo "Success" || echo "Failure" ; echo
  [[ -n $_VERBOSE ]] && { ssh root@${_DI_HOST} "cat ${remote_file_pipelines}" ; echo ; }
  ssh root@${_DI_HOST} "rm -f ${remote_file_pipelines}"
}

check_pipelines() {
  echo -e "\n==================== Checking status of pipelines\n"
  [[ $_VERBOSE == "yes" ]] && { ssh ${_DI_HOST} 'su - gsods -c /dbagiga/scripts/statusPipelines.sh' ; return 0 ; }
  echo -ne "Status of pipelines: "
  local pipeline_exitcode=$( ssh ${_DI_HOST} 'su - gsods -c /dbagiga/scripts/statusPipelines.sh >/dev/null 2>&1 ; echo $? ')
  [[ $pipeline_exitcode -eq 0 ]] && echo "Success" || echo "Failed"
}

check_ctm() {
  [[ "${ENV_NAME}" == "TAUG" ]] && { echo -e "\n==================== No CTM on DEV\n" ; return 1 ; } 
  echo -e "\n==================== Checking status of today's CTM jobs\n"

  # Get ctm log for today's date - 4 lines for each feeder e.g.
  # ==================== Mon Mar 11 08:35:00 IST 2024 Start synchronous process for gilboaupdate feeder.
  # portal_calendary_changes_view
  # show exit code:
  # "OK"
  
  # Get 4 lines for each feeder operation
  # '/'"$(date |cut -c1-10)"'/,$!d' - Get rows from today's date till EOF e.g. "Sun Mar 24"
  # "s/^Start action sent to table(.*$)/\1/p" - Edit line to only show the table name e.g. "kr_cheder"  <-- line 2
  # '/show exit code:/{p;n;p;}' - Shows match and the following line (which is "OK")                    <-- line 3 & 4
  # /$(date |cut -c1-10)/p" - Display lines with today's date                                           <-- line 1
  local ctm_of_today=$(sed -E '/'"$(date |cut -c1-10)"'/,$!d' /gigalogs/dataengine_ctm.log | sed -nE "s/^Start action sent to table(.*$)/\1/p ; /show exit code:/{p;n;p;} ; /^[fF]inished/p ; /$(date |cut -c1-10)/p")

  local line_num=1 line line_modulu4 ctm_result=Success
  while read line ; do
    line_modulu4=$(( line_num % 5 ))
    case $line_modulu4 in
      "1")  if ! echo $line | grep '====.*feeder\.$' >/dev/null 2>&1 ; then ctm_result=Failed ; echo "Error on line ${line_num}" ; break ; fi ;;
      "2")  if ! echo $line | grep '_' >/dev/null 2>&1 ; then ctm_result=Failed ; echo "Error on line ${line_num}" ; break ; fi ;;
      "3")  if ! echo $line | grep 'show exit code:' >/dev/null 2>&1 ; then ctm_result=Failed ; echo "Error on line ${line_num}" ; break ; fi ;;
      "4")  if ! echo $line | grep '"OK"' >/dev/null 2>&1 ; then ctm_result=Failed ; echo "Error on line ${line_num}" ; break ; fi ;;
      "0")  if ! echo $line | grep 'feeder_exit_code=0' >/dev/null 2>&1 ; then ctm_result=Failed ; echo "Error on line ${line_num}" ; break ; fi ;;
    esac
    (( line_num++ ))
  done < <(echo -e "${ctm_of_today}")

  echo -e "\nStatus of CTM jobs: ${ctm_result}"
  [[ -z $_VERBOSE ]] && return 0
  echo ; read -t 3 -p "Press ENTER or wait 3 seconds to see full CTM log:" ; echo -e "\n"
  echo -e "${ctm_of_today}"
}

check_sanity_errors() {
  echo -e "\n==================== Checking sanity lines containing:  fail|error|warn|down\n"
  local include_txt='fail\|error\|warn\|down\|==========================='
  grep "$(date +%Y-%m-%d)" /dbagigalogs/sanity/sanity.log | grep -i "${include_txt}" | grep -v 'ZOOKEEPER-LEADER'
}

check_notifiers() {
  [[ "${ENV_NAME}" == "TAUG" ]] && return
  echo -e "\n==================== Checking notifiers.\n"
  local notifier_output result
  echo -n "Notifiers result: "
  notifier_output=$(auto_notifiers.sh -l)
  result=$(echo "${notifier_output}" |grep notifier |grep intact | wc -l)
  if [[ $result -eq 2 ]] ; then
    echo "Success"
  else
    echo "Failure"
    echo -e "${notifier_output}"
  fi
  #curl -u ${_USER}:${_PASS} -s http://${_MGR}:8090/v2/pus | jq -r '.[] | select(.name | contains("notifier")).status'
}

check_gigashare() {
  #[[ "${_QUIET}" != "-q" ]] && { echo ; read -sn1 -p "Press any key to check gigashare mount on all hosts." ; echo ; }
  echo -e "\n==================== Check gigashare mount on all hosts\n"
  echo -n "gigashare check: "
  local check_gigashare=$(for h in ${_ALL_HOSTS[@]} ; do ssh $h 'echo "$(hostname) $(df -h |grep gigashare)"' ; done | grep -v gigashare)
  [[ -z $check_gigashare ]] && echo Success || { echo -e Failure ; echo -e "${check_gigashare}" ; }
  [[ -n "${_VERBOSE}" ]]  && { echo ; for h in ${_ALL_HOSTS[@]} ; do ssh $h 'echo "$(hostname) $(df -h |grep gigashare)"' ; done ; }
}

check_nbapp_services() {
  echo -e "\n==================== Check all NB APP services on all NB APP hosts\n"
  echo -n "NB APP services check: "
  local result="$(runall -na 'for s in nginx.service consul.service consul-template.service telegraf.service northbound.target ; do printf "%-45s%s\n" "$(hostname) ${s}:" "$(systemctl is-active ${s})" | grep -vw active ; done' | grep -v '===\|^ *$')"
  [[ -z $result ]] && echo Success || { echo -e Failure ; echo "${result}" ; }
}

check_nbagent_services() {
  echo -e "\n==================== Check all NB AGENT services on all NB AGENT hosts\n"
  echo -n "NB AGENT services check: "
  local result="$(runall -s 'for s in consul.service telegraf.service northbound.target ; do printf "%-45s%s\n" "$(hostname) ${s}:" "$(systemctl is-active ${s})" | grep -vw active ; done' | grep -v '===\|^ *$')"
  [[ -z $result ]] && echo Success || { echo -e Failure ; echo "${result}" ; }
}

check_indexes_for_dv_max() {
  local IDX=$( curl -su ${_USER}:${_PASS} http://${_MGR}:8090/v2/spaces/dih-tau-space/objectsTypeInfo | jq -r '.objectTypesMetadata[] | select(.indexes[] | select(.name == "T_IDKUN" and .method == "EQUAL_AND_ORDERED")).objectName' )
  local IDX_COUNT="Number of Types that have T_IDKUN EQUAL_AND_ORDERED index: $(echo -e "${IDX}" | wc -l)"
  [[ $1 == "-c" ]] && { echo -e "\n${IDX_COUNT}" ; return 0 ; }
  [[ $1 == "-l" ]] && { echo -e "$(date)\n${IDX_COUNT}\n${IDX}" >> /gigalogs/check-indexes.log ; return 0 ; }
  [[ -n $1 ]] && { echo "Option not supported" ; return 1 ; } 
  echo -e "\n${IDX}"
}

check_indexes_count() {
  local IDX=$( curl -su ${_USER}:${_PASS} http://${_MGR}:8090/v2/spaces/dih-tau-space/objectsTypeInfo| jq -r '.objectTypesMetadata[] | "objectName: \(.objectName)\nindexCount: \(.indexes | length)"' )
  echo -e "\n${IDX}"
}

check_indexes_count_less_than_2() {
  echo -e "\n==================== Display tables with less than 2 indexes.\n"
  echo -n "Index check: "
  local IDX=$(curl -su ${_USER}:${_PASS} http://${_MGR}:8090/v2/spaces/dih-tau-space/objectsTypeInfo \
  | jq -r '
    .objectTypesMetadata[]
    # Only proceed if the current element is indeed an object
    | select(type == "object")

    # Make sure we have an array in .indexes
    | select(.indexes? | type == "array")

    # Now we can safely count its length
    | select(.indexes | length <= 1)

    # Exclude objectName == "SHOB_GA"
    | select(.objectName? != "SHOB_GA")

    # Print desired fields
    | "objectName: \(.objectName)\nindexCount: \(.indexes | length)"
  ')
  [[ -z $IDX ]] && echo Success || { echo  "Failure" ; echo -e "\n${IDX}" ; }
}

check_sync_of_managers() {
  echo -e "\n==================== Check managers' SYNC: Compare PU and CONTAINER counts between managers.\n"
  echo -ne "manager sync: "
  # In TEST env e.g.: space_pus=32, non_empty_instances=102
  local mng sync_failed=0 empty_instance_flag=0
  local -A space_pus non_empty_instances empty_instances                # Associative arrays
  for mng in ${_MANAGERS[@]} ; do 
    if [[ -n $_PRINT ]] ; then
      echo "curl -su "${_USER}:${_PASS}" http://${mng}:8090/v2/pus/$_SPACE_NAME | jq -r '.instances[]' | wc -l"
      echo "curl -su "${_USER}:${_PASS}" http://${mng}:8090/v2/containers | jq -r '.[].instances?[]' | wc -l"
      echo "curl -su "${_USER}:${_PASS}" http://${mng}:8090/v2/containers | jq -r '.[] | select(.instances | length == 0) | .zones[]'|wc -l"
    fi
    space_pus["${mng}"]="$(curl -su "${_USER}:${_PASS}" http://${mng}:8090/v2/pus/$_SPACE_NAME | jq -r '.instances[]' | wc -l)"
    non_empty_instances["${mng}"]="$(curl -su "${_USER}:${_PASS}" http://${mng}:8090/v2/containers | jq -r '.[].instances?[]' | wc -l)"
    #curl -su "${_USER}:${_PASS}" http://${mng}:8090/v2/containers | jq -r '.[].instances?[]' | sort > /tmp/check-env-$mng
    empty_instances["${mng}"]="$(curl -su "${_USER}:${_PASS}" http://${mng}:8090/v2/containers | jq -r '.[] | select(.instances | length == 0) | .zones[]'|wc -l)"
    [[ ${empty_instances["${mng}"]} -ne 0 ]] && empty_instance_flag=1
    [[ space_pus["${_MANAGERS[0]}"] -ne space_pus["${mng}"] ]] && sync_failed=1
    [[ non_empty_instances["${_MANAGERS[0]}"] -ne non_empty_instances["${mng}"] ]] && sync_failed=1
  done
  [[ $sync_failed -eq 0 ]] && echo Success || { echo failure ; declare -p space_pus non_empty_instances ; }
  [[ $empty_instance_flag -ne 0 ]] && declare -p empty_instances
  [[ -n $_VERBOSE ]] && declare -p space_pus non_empty_instances empty_instances
}

check_spacedeck_on_managers() {
  echo -e "\n==================== Check on which managers SPACEDECK is running.\n"
  for mng in ${_MANAGERS[@]} ; do
    local host_name=$(host $mng | awk '{print $NF}' | sed 's/\.$//')
    echo -en "spacedeck ${host_name%%\.*}: "
    curl -sLI http://${mng}:4200 |grep ' 200 OK' >/dev/null 2>&1
    [[ $? -eq 0 ]] && echo -e "UP" || echo -e "DOWN"
  done
}

person_schedule_query_2() {

  get_certs
  local service_string='person_schedule_service/v1/u1?idno=97545&from_date=2023-06-30&to_date=2023-07-30&limit=1'
  local service_name=person_schedule_service
  # Non verbose
  [[ -z "${_VERBOSE}" ]] && { printf '%-40s%s\n' "${service_name}" "$(curl --max-time 10 -s --key $_ENV_KEY --cert $_ENV_CERT --cacert $_ENV_CACERT "https://${_END_POINT}:8443/${service_string}" | jq '.res | if length == 0 then "=========== empty response" else "data returned" end')" ; return ; }
  # Verbose
  printf '=%.0s' {1..50} ; echo "${service_name}"
  time curl --max-time 10 -s --key $_ENV_KEY --cert $_ENV_CERT --cacert $_ENV_CACERT "https://${_END_POINT}:8443/${service_string}" | sed '$a\' ; echo
}

do_daily() {
  run_sanity_bg
  [[ "${ENV_NAME}" != "TAUG" ]] && run_pipelines_bg
  check_ping
  check_nbapp_services
  check_nbagent_services
  check_indexes_count_less_than_2
  check_spacedeck_on_managers
  check_sync_of_managers
  check_notifiers
  show_primary_backup
  service_hc
  service_query
# check_pipelines        # 7.3s
  check_ssh              # 7s
  #check_mng_connection   # 7s
  check_feeders               # gfull 6.4s, gupd 6.6, ora 6.9
  check_disk_usage_all   # 5s
  check_ctm
  [[ "${ENV_NAME}" != "TAUG" ]] && show_pipelines_bg
  check_gigashare
  check_sanity_errors
  show_sanity_bg              # 103s = 1.43s
  #check_all_sanity_of_today
}

####################################### MAIN

validate_args "${@}"
[[ $1 == "-h" ]] && usage
do_env "${@}"
[[ $# -gt 0 ]] && do_menu "${@}"
do_title
case $_MODE in
  "daily") do_daily ;;
  "regular") echo "\nTBD\n" ;; #do_regular
esac
finish_checking_env
