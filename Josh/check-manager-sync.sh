#!/bin/bash
# check_manager_sync.sh v1.0
# BUILD date 2022-12-04

[[ ! -x /dbagiga/utils/host-yaml.sh ]] && { echo -e "Can not find host-yaml.sh - exiting" ; exit 1 ; }

# Environment settings
env_settings() {
source ~/.bash_profile
_LOG="/var/log/chaos_monkey.log"
_REST_LOG="/dbagigalogs/check_manager_sync.log"
_ACTIVE_MANAGER=""
_MANAGER_STUCK=""
_ADMINAPI_EXIT_CODE=1
_TEMP_FILE=/tmp/rest_temp_file
_TEMP_FILE2=/tmp/bll_query$$
_MANAGER_CONTROLLER="/dbagiga/DEPLOYMENT/MANAGER_CONTROLLER"
case $ENV_NAME in
  "GRG" ) _ODSGS="odsgs-mng-garage.hq.il.tleumi" ;;
  "DEV" ) _ODSGS="odsgs-mng-dev.hq.il.tleumi" ;;
  "STG" ) _ODSGS="odsgs-mng-stg.hq.il.bleumi" ;;
  "DR"  ) _ODSGS="odsgs-mng-tlv-prd.hq.il.leumi" ;;
  "PRD" ) _ODSGS="odsgs-mng-lod-prd.hq.il.leumi" ;;
esac
# set user/pass for curl and gs.sh
[[ -x /dbagiga/getUser.sh ]] && _CREDS=($(/dbagiga/getUser.sh)) || _CREDS=( user pass ) ; _USER=${_CREDS[0]} ; _PASS=${_CREDS[1]}
# Get managers and loadbalancer 
[[ -z $_ODSGS ]] && { _ODSGS=$( /dbagiga/utils/host-yaml.sh manager | tail -1 ) ; http="http" ; } || { HTTP=https ; }
_ALL_ODS_MNG=( $(curl -u ${_USER}:${_PASS} -sk "${HTTP}://${_ODSGS}:8090/v2/info" |jq '.managers | .[]' | tr -d '"') )
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
  for h in ${_ALL_ODS_MNG[@]} ; do
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
odsx_check_manager_cluster() {
  [[ "${ENV_NAME}" != "GRG" && "${ENV_NAME}" != "DEV" ]] && { echo -e "\n==================== Can not check manager cluster on secured env" ; return 1 ; }
  echo -e "\n==================== Check if manager cluster is functioning correctly"
  #check_manager_cluster
  if check_manager_cluster ; then 
    echo "Manager cluster is INTACT - ${_ACTIVE_MANAGER}"
  else
    echo "Manager cluster NOT intact"
  fi
}

# Used by 3
# Check BLLSERVICE STATE and query JOTBMF01_TN_MATI - EXPECTED RESULT: bllservice state=intact and tn_mati query response=0
check_query_and_state() {
  local bll_server=${1}
  [[ "${1}" == "${_ODSGS}" ]] && HTTP=https || HTTP=http    # if endpoint then use "https"
  _BLL_STATE=$(curl -u ${_USER}:${_PASS} -skX GET --header "Accept: application/json" "${HTTP}://${bll_server}:8090/v2/pus" |jq '.[] | select(.name == "bllservice" ) | .status')
  ( curl -u ${_USER}:${_PASS} -skX GET --header "Accept: application/json" "${HTTP}://${bll_server}:8090/v2/spaces/bllspace/query?typeName=JOTBMF01_TN_MATI&filter=JOMF01_SNIF%3C%3E0&maxResults=1" >$_TEMP_FILE2 2>&1 ) & 
  local bll_query_pid=$!
  for i in {1..5} ; do      # if JOTBMF01_TN_MATI curl gets stuck > 5s the kill it
    if ps -fp $bll_query_pid >/dev/null 2>&1 ; then  sleep 1 ; else break ; fi
    [[ $i -eq 5 ]] && { echo "_BLL_QUERY got stuck" ; kill -9 $bll_query_pid >/dev/null 2>&1 ; return 1 ; }
  done
  _BLL_QUERY=$( cat $_TEMP_FILE2 ) ; rm -f $_TEMP_FILE2
  echo -e "id field = \"$(echo $_BLL_QUERY | grep -oE '[0-9]*\|[0-9]*\|[0-9]*\|[0-9]*\|[0-9]*\|[0-9]{4}-[0-9]{2}-[0-9]{2}')\""
  if echo $_BLL_QUERY | grep -oE '[0-9]*\|[0-9]*\|[0-9]*\|[0-9]*\|[0-9]*\|[0-9]{4}-[0-9]{2}-[0-9]{2}' >/dev/null 2>&1 ; then local result=SUCCEEDED ; else local result=FAILED ; fi
  echo "JOTBMF01_TN_MATI query $result to retrieve a data record."
  return 0
}

# 3. Check that bllservice is INTACT and TN_MATI can be queried
odsx_check_query_and_state() {
  echo -e "\n==================== Check that bllservice state is INTACT and TN_MATI can be queried"
  for h in ${_ALL_ODS_MNG[@]} $_ODSGS ; do
    if check_query_and_state ${h} ; then local result=succeeded ; else local result=failed ; fi
    echo "jotbmf01_tn_mati query thru ${h} ${result}"
    echo "bllservice state=${_BLL_STATE}"
  done
}

# Used by 4
# Check if gs.sh gets stuck when querying one of the managers
check_gs_query_of_managers() {
  local mng_stuck=0
  # Do gs.sh query on all manaagers
  for h in ${_ALL_ODS_MNG[@]}; do
    echo "Checking manager ${h}..."
    /dbagiga/gigaspaces-smart-ods/bin/gs.sh --server=$h --username=$_USER --password=$_PASS pu list  >> $_REST_LOG 2>&1 & 
    local gs_pid=$!
    # Wait for gs.sh query to finish up to 30s before failing
    for ((i=1 ; i<30 ; i++ )) ; do                                    # Wait for gs.sh query to finish up to 30s before failing
      sleep 1
      gs_proc=$(ps -fp $gs_pid | grep -v grep | grep 'gs.sh')
      [[ -z "$gs_proc" && $i -lt 29 ]] && break
      [[ -n "$gs_proc" && $i -ge 29 ]] && { mng_stuck=1 ; echo -e "$(date) REST: Killing ${gs_proc} gs.sh query stuck for 30s." >> $_REST_LOG ; kill -9 $gs_pid ; wait $gs_pid 2>/dev/null ; break ; }
    done
    [[ $mng_stuck -eq 1 ]] && { _MANAGER_STUCK="${h}" ; echo "$(date) REST: gs.sh query stuck when querying manager=${_MANAGER_STUCK}" >> $_REST_LOG ; break ; }        # Break on first gs.sh query failure
  done
}


# 4. Check if gs.sh gets stuck
odsx_check_gs_query_of_managers() {
  echo -e "\n==================== Check if gs.sh gets stuck while querying managers"
  check_gs_query_of_managers
  if [[ -z ${_MANAGER_STUCK} ]] ; then
    echo "SUCCEEDED - gs.sh query did not get stuck while querying managers"
  else
    echo "FAILED - gs.sh query got stuck while querying manager ${_MANAGER_STUCK}"
  fi
}

# Used by 5
# Check if all managers have same bllspace instance count.
check_all_mng_inst_count() {
    # Get count from 1st manager to compare with the others
    local first_inst_count=$(curl -u ${_USER}:${_PASS} -skX GET --header "Accept: application/json" "http://${_ALL_ODS_MNG[0]}:8090/v2/spaces/bllspace" |jq ."instancesIds"[]|wc -l)
    echo -e "Instance count for ${_ALL_ODS_MNG[0]} = ${first_inst_count}"
    # Continue check from 2nd manager "i=1"
    for ((i=1 ; i < ${#_ALL_ODS_MNG[@]} ; i++ )) ; do
      local inst_count=$(curl -u ${_USER}:${_PASS} -skX GET --header "Accept: application/json" "http://${_ALL_ODS_MNG[${i}]}:8090/v2/spaces/bllspace" |jq ."instancesIds"[]|wc -l)
      echo -e "Instance count for ${_ALL_ODS_MNG[${i}]} = ${inst_count}"
    done
}

# 5. Check if all managers see same amount of GSC's
odsx_check_all_mng_inst_count() {
  echo -e "\n==================== Check if all managers have same bllspace instance count."
  check_all_mng_inst_count
}

# Used by 6
# Check start time of GSM process for all managers
check_GSM_proc_start_time() {
  echo -e "Managers' GSM process ID and start time:"
  local mng_pid
  for h in ${_ALL_ODS_MNG[@]} ; do
    mng_pid=$(ssh $h "pgrep -f 'com.gigaspaces.start.SystemBoot services=MANAGER\[LH,ZK,GSM,REST\]'")
    printf "%-15s %12s %s\n" "$h" "$mng_pid" "$(ssh $h "ps -o lstart= -p ${mng_pid}")"
  done
}

# 6. Check start time of GSM process for all managers
odsx_check_GSM_proc_start_time() {
  echo -e "\n==================== Check start time of GSM process for all managers"
  check_GSM_proc_start_time
}

#=============== MAIN =================

env_settings
check_if_managers_up                  # 1. Preliminary check
odsx_check_manager_cluster            # 2. Check if manager cluster is INTACT
odsx_check_query_and_state            # 3. Check that bllservice is INTACT and TN_MATI can be queried
odsx_check_gs_query_of_managers       # 4. Check if gs.sh gets stuck
odsx_check_all_mng_inst_count         # 5. Check if all managers see same amount of GSC's
odsx_check_GSM_proc_start_time        # 6. Check start time of GSM process for all managers
echo
