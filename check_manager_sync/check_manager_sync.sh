#!/bin/bash
# check_manager_sync.sh v1.0
# BUILD date 2022-12-04
env_settings() {
#source /root/.bash_profile
source /dbagiga/utils/check_manager_sync/check_manager_sync.conf
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
  cd $MANAGER_CONTROLLER ; ./testController.sh > $_TEMP_FILE 2>&1 ; _ADMINAPI_EXIT_CODE=$?
  [[ $_ADMINAPI_EXIT_CODE -ne 0 ]] && { echo "$(date) REST: Manager cluster not intact" >> tee -a $_REST_LOG $_LOG ; }
  _ACTIVE_MANAGER=$(sed -En 's/.*(#####[^#]*anager.*#####).*/\1/p' $_TEMP_FILE | tr -d '\n' )
}
# 2. Check manager cluster
odsx_check_manager_cluster() {
  echo -e "\n==================== Check if manager cluster is functioning correctly"
  [[ ! -d $MANAGER_CONTROLLER ]] && { echo "$(date) CHECK_MANAGER_SYNC: Skipping cluster check because ADMIN API does not exist" | tee -a $_REST_LOG $_LOG ; return 1 ; }
  check_manager_cluster
  if [[ $_ADMINAPI_EXIT_CODE -eq 0 ]] ; then
    echo "Cluster intact - ${_ACTIVE_MANAGER}"
  else
    echo "Cluster NOT intact"
  fi
}
# Used by 3
# Check BLLSERVICE STATE and query TN_MATI - EXPECTED RESULT: bllservice state=intact and tn_mati query response=0
check_query_and_state() {
  local bll_server=${1}
  [[ "${1}" == "${_ODSGS}" ]] && bll_http=https || bll_http=http
  _BLL_STATE=$(curl -u ${_USER}:${_PASS} -skX GET --header "Accept: application/json" "${bll_http}://${bll_server}:8090/v2/pus" 2>/dev/null | awk -F'"status":' '{print $2}'|awk -F',' '{print $1}' | tr -d '"' | tr "[a-z]" "[A-Z]")
  [[ ! "$(curl -u ${_USER}:${_PASS} -skI http://${1}:8090/v2/spaces/bllspace/query?typeName=JOTBMF01_TN_MATI|grep HTTP)" =~ "200 OK" ]] && { _BLL_QUERY_RESPONSE=1 ; return 1 ; }
  _BLL_QUERY=$(curl -u ${_USER}:${_PASS} -skX GET --header "Accept: application/json" "${bll_http}://${bll_server}:8090/v2/spaces/bllspace/query?typeName=JOTBMF01_TN_MATI&filter=JOMF01_SNIF%3C%3E0&maxResults=1")
  echo $_BLL_QUERY | grep -E '[0-9]*\|[0-9]*\|[0-9]*\|[0-9]*\|[0-9]*' >/dev/null 2>&1
  _BLL_QUERY_RESPONSE=$?
}
# 3. Check that bllservice is INTACT and TN_MATI can be queried
odsx_check_query_and_state() {
  echo -e "\n==================== Check that bllservice state is INTACT and TN_MATI can be queried"
  for h in ${_ALL_ODS_MNG[@]} $_ODSGS ; do
    check_query_and_state ${h}
    [[ $_BLL_QUERY_RESPONSE -ne 0 ]] && fail_succeed=failed || fail_succeed=succeeded
    echo "jotbmf01_tn_mati query thru ${h} ${fail_succeed}"
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
    echo "gs.sh query did not get stuck while querying managers"
  else
    echo "gs.sh query got stuck while querying manager ${_MANAGER_STUCK}"
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
