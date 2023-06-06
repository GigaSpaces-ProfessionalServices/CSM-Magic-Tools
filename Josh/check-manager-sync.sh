#!/bin/bash
# check_manager_sync.sh v1.0
# BUILD date 2022-12-04

[[ ! -x /dbagiga/utils/host-yaml.sh ]] && { echo -e "Can not find host-yaml.sh - exiting" ; exit 1 ; }

# Environment settings
env_settings() {
source ~/.bash_profile
_LOG="/var/log/chaos_monkey.log"
_REST_LOG="/gigalogs/check_manager_sync.log"
_ACTIVE_MANAGER=""
_MANAGER_STUCK=""
_ADMINAPI_EXIT_CODE=1
_TEMP_FILE=/tmp/rest_temp_file
_TEMP_FILE2=/tmp/bll_query$$
_MANAGER_CONTROLLER="/dbagiga/DEPLOYMENT/MANAGER_CONTROLLER"
if [[ "${ENV_NAME}" == "TAUG" || "${ENV_NAME}" == "TAUS" || "${ENV_NAME}" == "TAUP" ]] ; then
  _SERVICE_NAME=dih-tau-service
  _SPACE_NAME=dih-tau-space
else
  _SERVICE_NAME=bllservice
  _SPACE_NAME=bllspace
fi
case $ENV_NAME in
  "GRG" ) _ODSGS="odsgs-mng-garage.hq.il.tleumi" ;;
  "DEV" ) _ODSGS="odsgs-mng-dev.hq.il.tleumi" ;;
  "STG" ) _ODSGS="odsgs-mng-stg.hq.il.bleumi" ;;
  "DR"  ) _ODSGS="odsgs-mng-tlv-prd.hq.il.leumi" ;;
  "PRD" ) _ODSGS="odsgs-mng-lod-prd.hq.il.leumi" ;;
  *) _ODSGS=""
esac
# set user/pass for curl and gs.sh
[[ -x /dbagiga/getUser.sh ]] && _CREDS=($(/dbagiga/getUser.sh)) || _CREDS=( user pass ) ; _USER=${_CREDS[0]} ; _PASS=${_CREDS[1]}
# Get managers and loadbalancer 
[[ -z $_ODSGS ]] && http="http" || HTTP=https
_ALL_MANAGER_SERVERS=( $(host-yaml.sh -m) )
# Remove temp files before quitting
graceful_shutdown() {
  rm -rf $_TEMP_FILE
  rm -rf $_TEMP_FILE2
  exit
}
trap graceful_shutdown SIGINT SIGQUIT SIGTERM
}

# 1. Preliminary manager ping check
check_if_managers_up() {
  local manager_down=0
  for h in ${_ALL_MANAGER_SERVERS[@]} ; do
    if [[ ! "`ping -c1 -w2 $h 2>/dev/null`" ]] ;then
      echo -e "$(date) REST: Machine $h is down" >> tee -a $_REST_LOG
      manager_down=1
    fi
  done
  [[ $manager_down -eq 1 ]] && { echo -e "At least one manager failed ping test." ; return 1 ; }
  return 0
}

# Used for check 2
check_manager_cluster() {
  cd $_MANAGER_CONTROLLER ; ./testController.sh > $_TEMP_FILE 2>&1 ; admin_exit_code=$?
  [[ $admin_exit_code -ne 0 ]] && { echo "$(date) REST: Manager cluster not intact" >> tee -a $_REST_LOG $_LOG ; return 1 ; }
  _ACTIVE_MANAGER=$(sed -En 's/.*(#####[^#]*anager.*#####).*/\1/p' $_TEMP_FILE | tr -d '\n' )
  rm -f $_TEMP_FILE
  return 0
}

# 2. Check manager cluster
check_manager_cluster() {
  [[ "${ENV_NAME}" != "GRG" && "${ENV_NAME}" != "DEV" ]] && { echo -e "\n==================== Can not check manager cluster on this env" ; return 1 ; }
  echo -e "\n==================== Check if manager cluster is functioning correctly"
  #check_manager_cluster
  if check_manager_cluster ; then 
    echo "Manager cluster is INTACT - ${_ACTIVE_MANAGER}"
  else
    echo "Manager cluster NOT intact"
  fi
}

# Used by 3
# Check BLLSERVICE STATE and query JOTBMF01_TN_MATI - EXPECTED RESULT: ${_SERVICE_NAME} state=intact and tn_mati query response=0
check_state() {
  local server=${1}
  [[ "${1}" == "${_ODSGS}" ]] && HTTP=https || HTTP=http    # if endpoint exists then use "https"
  local service_state=$(curl -u ${_USER}:${_PASS} -skX GET --header "Accept: application/json" "${HTTP}://${server}:8090/v2/pus" |jq -r ".[] | select(.name == \"${_SERVICE_NAME}\" ) | .status")
  echo $service_state
}

# 3. Check that service is INTACT
check_service_state() {
  echo -e "\n==================== Check service state for all managers."
  for h in ${_ALL_MANAGER_SERVERS[@]} $_ODSGS ; do
    local service_state=$( check_state $h )
    echo SERVER=${h} SERVICE=${_SERVICE_NAME} STATE=${service_state}
  done
}

# Used by 4
# Check if gs.sh gets stuck when querying one of the managers
check_gs_query() {
  local mng_stuck=0
  # Do gs.sh query on all manaagers
  for h in ${_ALL_MANAGER_SERVERS[@]}; do
    echo "Checking manager ${h}..."
    /dbagiga/gigaspaces-smart-ods/bin/gs.sh --server=$h --username=$_USER --password=$_PASS pu list  >> $_REST_LOG 2>&1 & 
    local gs_pid=$!
    # Wait for gs.sh query to finish up to 30s before failing
    for ((i=1 ; i<30 ; i++ )) ; do
      sleep 1
      gs_proc=$(ps -fp $gs_pid | grep -v grep | grep 'gs.sh')
      [[ -z "$gs_proc" && $i -lt 29 ]] && break
      [[ -n "$gs_proc" && $i -ge 29 ]] && { mng_stuck=1 ; echo -e "$(date) REST: Killing ${gs_proc} gs.sh query stuck for 30s." >> $_REST_LOG ; kill -9 $gs_pid ; wait $gs_pid 2>/dev/null ; break ; }
    done
    [[ $mng_stuck -eq 1 ]] && { _MANAGER_STUCK="${h}" ; echo "$(date) REST: gs.sh query stuck when querying manager=${_MANAGER_STUCK}" >> $_REST_LOG ; break ; }        # Break on first gs.sh query failure
  done
}


# 4. Check if gs.sh gets stuck
check_gs_query_of_managers() {
  echo -e "\n==================== Check if gs.sh gets stuck while querying managers"
  check_gs_query
  if [[ -z ${_MANAGER_STUCK} ]] ; then
    echo "SUCCEEDED - gs.sh query did not get stuck while querying managers"
  else
    echo "FAILED - gs.sh query got stuck while querying manager ${_MANAGER_STUCK}"
  fi
}

# 5. Check if all managers have same _SPACE_NAME (bllspace/dih-tau-space) instance count.
check_all_mng_inst_count() {
  echo -e "\n==================== Check if all managers have same bllspace instance count."
  # Get count from 1st manager to compare with the others
  local first_inst_count=$(curl -u ${_USER}:${_PASS} -s "http://${_ALL_MANAGER_SERVERS[0]}:8090/v2/spaces" | jq -r ".[] | select(.name==\"${_SPACE_NAME}\") | .instancesIds[]" | wc -l)
  echo -e "Instance count for ${_ALL_MANAGER_SERVERS[0]} = ${first_inst_count}"
  # Continue check from 2nd manager "i=1"
  for ((i=1 ; i < ${#_ALL_MANAGER_SERVERS[@]} ; i++ )) ; do
    local inst_count=$(curl -u ${_USER}:${_PASS} -s "http://${_ALL_MANAGER_SERVERS[${i}]}:8090/v2/spaces" | jq -r ".[] | select(.name==\"${_SPACE_NAME}\") | .instancesIds[]" | wc -l)
    echo -e "Instance count for ${_ALL_MANAGER_SERVERS[${i}]} = ${inst_count}"
  done
}

# 6. Check start time of GSM process for all managers
check_GSM_proc_start_time() {
  echo -e "\n==================== Check start time of GSM process for all managers"
  echo -e "Managers' GSM process ID and start time:"
  local mng_pid
  for h in ${_ALL_MANAGER_SERVERS[@]} ; do
    mng_pid=$(ssh $h "pgrep -f 'com.gigaspaces.start.SystemBoot services=MANAGER\[LH,ZK,GSM,REST\]'")
    printf "%-15s %12s %s\n" "$h" "$mng_pid" "$(ssh $h "ps -o lstart= -p ${mng_pid}")"
  done
}

#=============== MAIN =================

env_settings
check_if_managers_up             # 1. Preliminary check
check_manager_cluster            # 2. Check if manager cluster is INTACT
check_service_state                      # 3. Check that ${_SERVICE_NAME} is INTACT
check_gs_query_of_managers       # 4. Check if gs.sh gets stuck
check_all_mng_inst_count         # 5. Check if all managers see same amount of GSC's
check_GSM_proc_start_time        # 6. Check start time of GSM process for all managers
echo
