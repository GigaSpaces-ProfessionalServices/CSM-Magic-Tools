#!/bin/bash

function do_env() {
export TERM=xterm
case $ENV_NAME in
  "TAUG") _TAU_ENV=DEV ;;
  "TAUS") _TAU_ENV=TEST ;;
  "TAUP") _TAU_ENV=PROD ;;
  *) echo "Wrong env" ; exit 1 ;;
esac
  _SPACE_SERVERS=$( runall -s -l | grep -v "==" )
  _LOG=""
  _ERROR_SWITCH=0
  _EMAIL_SUBJECT=""
  _ERROR_OUT=""
  _GS_ALERT_LOG=/gigalogs/jr-gs-alert.log
  _ERR_LIST=""
  if [[ "${_TAU_ENV}" == "PROD" ]] ; then
    _RECIPIENTS="josh.roden@gigaspaces.com shmulik.kaufman@gigaspaces.com veronikap@tauex.tau.ac.il"
  else
    _RECIPIENTS="josh.roden@gigaspaces.com"
  fi
}

function get_auth() {
# Get user/pass creds
_USER=$(awk -F= '/app.manager.security.username=/ {print $2}' ${ENV_CONFIG}/app.config)
if grep '^app.vault.use=true' ${ENV_CONFIG}/app.config > /dev/null ; then
  _VAULT_PASS=$(awk -F= '/app.manager.security.password.vault=/ {print $2}' ${ENV_CONFIG}/app.config)
  _PASS=$(java -Dapp.db.path=/dbagigawork/sqlite/ -jar /dbagigashare/current/gs/jars/gs-vault-1.0-SNAPSHOT-jar-with-dependencies.jar --get ${_VAULT_PASS})
else
  _PASS=$(awk -F= '/app.manager.security.password=/ {print $2}' ${ENV_CONFIG}/app.config)
fi
}

function email_and_logs() {
  logger -t GS-ALERTS "${_EMAIL_SUBJECT}: "${_ERROR_OUT[@]}""
  echo -e "==========================\n$(date '+%Y-%m-%d %H:%M:%S') GS-ALERTS ${_EMAIL_SUBJECT}:\n${_ERROR_OUT[@]}" >> $_GS_ALERT_LOG
  echo -e "${_ALERT_NAME}:\n"${_ERROR_OUT[@]}"" | mailx -s "${_EMAIL_SUBJECT}" -r kapacitor-alerts@tau.ac.il "${_RECIPIENTS}" >/dev/null 2>&1
}

function clear_alert() {
  [[ ! -f $_LOG ]] && { touch $_LOG ; return ; }      # Create log if not exist 
  if [[ ! -s $_LOG ]] ; then                          # Return if empty    
    return
  else
    email_and_logs
    > $_LOG
  fi
}

function send_alert() {
  # Send email alert and write logs
  if [[ ! -f $_LOG || ! -s $_LOG ]] ; then          # if no logfile or logfile is empty 
    email_and_logs
    date +%s > $_LOG
    return
  fi

  # Send email alert and write logs - ONLY ONCE A DAY
  echo -e "==========================\n$(date '+%Y-%m-%d %H:%M:%S') GS-ALERTS ${_EMAIL_SUBJECT}:\n${_ERROR_OUT[@]}" >> $_GS_ALERT_LOG
  local sec=$( echo "$(date +%s) - $(cat ${_LOG})" | bc )
  [[ $sec -lt 21600 ]] && return
  # After 1 day send another alert
  email_and_logs
  date +%s > $_LOG
}

function check_one_nb_service() {
  local result host_name=$1 svc=$2 exit_code
  result=$( timeout $_NB_TIMEOUT ssh $host_name "systemctl is-active ${svc}" )
  exit_code=$?
  #[[ "${host_name}" == "gstest-manager2.tau.ac.il" ]] && { exit_code=124 ; result=inactive ; } 
  if [[ $result != "active" ]] ; then
    if [[ $exit_code -eq 124 ]] ; then 
      result="timeout"
    elif [[ -z $result ]] ; then
      result="Output is empty and exit_code=${exit_code}"
    fi
    _ERROR_OUT+=$(echo -e "\n$host_name $s ${result}")
    _ERROR_SWITCH=1
  fi
}

# Check NB servers and services
function check_nb_services() {

  _NB_APP_SERVERS=$( runall -na -l | grep -v "==" )
  _NB_APP_SERVICES=( nginx.service consul.service consul-template.service telegraf.service northbound.target )
  _NB_AGENT_SERVICES=( consul.service telegraf.service northbound.target ) 
  _NB_TIMEOUT=10

  _ERROR_OUT=""
  _ERROR_SWITCH=0
  _ALERT_NAME="NB SERVICES"
  _LOG=/giga/utils/check_nb_services.log
  local h s result host_name exit_code

  # Check NB APP servers
  for h in ${_NB_APP_SERVERS[@]} ; do
    host_name=$( host ${h} | awk '{print $NF}' | sed 's/\.$//' )
    for s in ${_NB_APP_SERVICES[@]} ; do
      check_one_nb_service $host_name $s
    done
  done

  # Check NB AGENT servers
  for h in ${_SPACE_SERVERS[@]} ; do
    host_name=$( host ${h} | awk '{print $NF}' | sed 's/\.$//' )
    for s in ${_NB_AGENT_SERVICES[@]} ; do
      check_one_nb_service $host_name $s
    done
  done

  if [[ $_ERROR_SWITCH -eq 1 ]] ; then
    _EMAIL_SUBJECT="${_TAU_ENV} :: ${_ALERT_NAME} :: ALERT"
    send_alert
  else
    _EMAIL_SUBJECT="${_TAU_ENV} :: ${_ALERT_NAME} :: OK"
    clear_alert 
  fi
}

# Alert if replicationMode of instance IDs != SYNC
function check_replicationmode() {
  _ERROR_OUT=()
  _ERROR_SWITCH=0
  _ALERT_NAME="SPACE INSTANCE ID REPLICATIONMODE"
  _LOG=/giga/utils/check_replicationmode.log

  local inst_id mode

  # Get Space PRIMARY instance ID's
  get_auth
  _MANAGERS=( $( runall -m -l | grep -v === ) )
  instance_ids=$(timeout 10 curl -s -u ${_USER}:${_PASS} "http://${_MANAGERS[0]}:8090/v2/spaces/dih-tau-space/instances" | jq -r '.[] | select(.mode =="PRIMARY").id')
  if [[ $? -ne 0 ]] ; then 
    _ERROR_OUT=( "Failed to get instance IDs" )
    echo "$(date '+%Y-%m-%d %H:%M:%S') GS-ALERTS "${_ERROR_OUT[@]}"" >> $_GS_ALERT_LOG
    _ERROR_SWITCH=1 
  else
    for inst_id in $instance_ids ; do
      mode=$(timeout 10 curl -s -u ${_USER}:${_PASS} "http://${_MANAGERS[0]}:8090/v2/spaces/dih-tau-space/instances/${inst_id}/statistics/replication" | jq -r '.channels | to_entries[] | select(.value.replicationMode == "BACKUP_SPACE") | .value.operatingMode')
      if [[ $? -ne 0 ]] ; then 
        _ERROR_OUT=( ${_ERROR_OUT[@]} $(echo -e "\nFailed to get replicationMode of instance ID ${inst_id}.") )
        echo "$(date '+%Y-%m-%d %H:%M:%S') GS-ALERTS "${_ERROR_OUT[@]}"" >> $_GS_ALERT_LOG
        _ERROR_SWITCH=1 
      elif [[ "${mode}" != "SYNC" ]] ; then
      #elif [[ "${mode}" != "SYNC" || "${inst_id}" == "dih-tau-space~5_1" ]] ; then
      #elif [[ "${mode}" != "SYNC" || "${inst_id}" == "dih-tau-space~5_1" || "${inst_id}" == "dih-tau-space~10_1" ]] ; then
        _ERROR_OUT=( ${_ERROR_OUT[@]} $(echo -e "\n${inst_id} replicationMode=${mode}") )
        echo "$(date '+%Y-%m-%d %H:%M:%S') GS-ALERTS "${_ERROR_OUT[@]}"" >> $_GS_ALERT_LOG
        _ERROR_SWITCH=1
      fi
      #echo -e "\n${inst_id} replicationMode=${mode}"
    done
  fi

  # Process if error occurred
  if [[ $_ERROR_SWITCH -eq 1 ]] ; then
    _EMAIL_SUBJECT="${_TAU_ENV} :: ${_ALERT_NAME} :: ALERT"
    send_alert
  else
    _EMAIL_SUBJECT="${_TAU_ENV} :: ${_ALERT_NAME} :: OK"
    clear_alert 
  fi
}

# Alert if replicationMode of instance IDs != SYNC
function check_pl_restarting() {

  _DIH1_SERVER=$( runall -d -l | grep -v === | head -1 )

  _ERROR_OUT=""
  _ERROR_SWITCH=0
  _ALERT_NAME="PIPELINE STATE RESTARTING"
  _LOG=/giga/utils/check_pipeline_state_running.log

  _ERROR_OUT=$( ssh ${_DIH1_SERVER} 'su - gsods -c /dbagiga/scripts/statusPipelines.sh' | grep '^PL.*RESTARTING' )
  #_ERROR_OUT=$( cat /tmp/jrpltest )
  if [[ -n $_ERROR_OUT ]] ; then
    echo "$(date '+%Y-%m-%d %H:%M:%S') GS-ALERTS "${_ERROR_OUT}"" >> $_GS_ALERT_LOG
    _ERROR_SWITCH=1
  fi

  # Process if error occurred
  if [[ $_ERROR_SWITCH -eq 1 ]] ; then
    _EMAIL_SUBJECT="${_TAU_ENV} :: ${_ALERT_NAME} :: ALERT"
    send_alert
  else
    _EMAIL_SUBJECT="${_TAU_ENV} :: ${_ALERT_NAME} :: OK"
    clear_alert
  fi
 
}

function check_count_primary_backup() {

  _ERROR_OUT=()
  _ERROR_SWITCH=0
  _ALERT_NAME="SPACE INSTANCE P/B COUNT"
  _LOG=/giga/utils/check_count_primary_backup.log

  local inst_id mode

  # Get Space PRIMARY instance ID's
  get_auth
  _MANAGERS=( $( runall -m -l | grep -v === ) )
  #instance_backup=$(timeout 10 curl -s -u ${_USER}:${_PASS} "http://${_MANAGERS[0]}:8090/v2/spaces/dih-tau-space/instances" | jq -r '.[] | select(.mode =="BACKUP").id')
  instance_primary=$(timeout 10 curl -s -u ${_USER}:${_PASS} "http://${_MANAGERS[0]}:8090/v2/spaces/dih-tau-space/instances" | jq -r '.[] | select(.mode =="PRIMARY").id')

  # Check: 1. If the curl request times out or 2. If the jq command fails to parse the response.
  if [[ $? -ne 0 ]] ; then 
    _ERROR_OUT=( "Failed to get instance IDs" )
    echo "$(date '+%Y-%m-%d %H:%M:%S') GS-ALERTS "${_ERROR_OUT[@]}"" >> $_GS_ALERT_LOG
    _ERROR_SWITCH=1 
  else
    for inst_id in $instance_ids ; do
      mode=$(timeout 10 curl -s -u ${_USER}:${_PASS} "http://${_MANAGERS[0]}:8090/v2/spaces/dih-tau-space/instances/${inst_id}/statistics/replication" | jq -r '.channels | to_entries[] | select(.value.replicationMode == "BACKUP_SPACE") | .value.operatingMode')
      if [[ $? -ne 0 ]] ; then 
        _ERROR_OUT=( ${_ERROR_OUT[@]} $(echo -e "\nFailed to get replicationMode of instance ID ${inst_id}.") )
        echo "$(date '+%Y-%m-%d %H:%M:%S') GS-ALERTS "${_ERROR_OUT[@]}"" >> $_GS_ALERT_LOG
        _ERROR_SWITCH=1 
      elif [[ "${mode}" != "SYNC" ]] ; then
      #elif [[ "${mode}" != "SYNC" || "${inst_id}" == "dih-tau-space~5_1" ]] ; then
      #elif [[ "${mode}" != "SYNC" || "${inst_id}" == "dih-tau-space~5_1" || "${inst_id}" == "dih-tau-space~10_1" ]] ; then
        _ERROR_OUT=( ${_ERROR_OUT[@]} $(echo -e "\n${inst_id} replicationMode=${mode}") )
        echo "$(date '+%Y-%m-%d %H:%M:%S') GS-ALERTS "${_ERROR_OUT[@]}"" >> $_GS_ALERT_LOG
        _ERROR_SWITCH=1
      fi
      #echo -e "\n${inst_id} replicationMode=${mode}"
    done
  fi

  # Process if error occurred
  if [[ $_ERROR_SWITCH -eq 1 ]] ; then
    _EMAIL_SUBJECT="${_TAU_ENV} :: ${_ALERT_NAME} :: ALERT"
    send_alert
  else
    _EMAIL_SUBJECT="${_TAU_ENV} :: ${_ALERT_NAME} :: OK"
    clear_alert 
  fi
}


function check_pl_running_count() {

  _ERROR_OUT=()
  _ERROR_SWITCH=0
  _ALERT_NAME="PL JOBS RUNNING COUNT"
  _LOG=/giga/utils/check_pl_running_count.log
  _ERR_LIST=""
  
  local dih1 api_base_url running_count job_ids pl_name 

  dih1=$(runall -d -l | grep -v == | head -1)
  api_base_url="http://${dih1}:8081/jobs"

  # Get the list of job IDs
  job_ids=$(curl -s "${api_base_url}" | jq -r '.jobs[] | .id')
  #job_ids=$(curl -s "${api_base_url}" | jq -r '.jobs[] | select(.status == "RUNNING") | .id')

  # Loop through each job ID and fetch its RUNNING count
  for job_id in $job_ids ; do
    running_count=$(curl -s "${api_base_url}/$job_id" | jq '.["status-counts"].RUNNING')
    if [[ $running_count -ne 3 ]] ; then
      pl_name=$( curl -s "${api_base_url}/$job_id" | jq -r '.name' | cut -d"|" -f1 )
      _ERR_LIST="${_ERR_LIST}PL name: ${pl_name}, Job ID: $job_id, RUNNING: ${running_count}"$'\n'
      _ERROR_SWITCH=1
    fi
  done

  # Process if error occurred
  if [[ $_ERROR_SWITCH -eq 1 ]] ; then
    _ERROR_OUT=( "${_ERR_LIST}" )
    _EMAIL_SUBJECT="${_TAU_ENV} :: ${_ALERT_NAME} :: ALERT"
    send_alert
  else
    _EMAIL_SUBJECT="${_TAU_ENV} :: ${_ALERT_NAME} :: OK"
    clear_alert 
  fi
}

# TBD
function send_clear_alert() {
echo
}


############### MAIN ###############

source ~/.bashrc
do_env
#check_nb_services
#check_replicationmode
#check_pl_restarting
#check_count_primary_backup
check_pl_running_count
