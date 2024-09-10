#!/bin/bash

function do_env() {
case $ENV_NAME in
  "TAUG") _TAU_ENV=DEV ;;
  "TAUS") _TAU_ENV=TEST ;;
  "TAUP") _TAU_ENV=PROD ;;
  *) echo "Wrong env" ; exit 1 ;;
esac
  _NB_APP_SERVERS=$( runall -na -l | grep -v "==" )
  _SPACE_SERVERS=$( runall -s -l | grep -v "==" )
  _NB_APP_SERVICES=( nginx.service consul.service consul-template.service telegraf.service northbound.target )
  _NB_AGENT_SERVICES=( consul.service telegraf.service northbound.target ) 
  _NB_TIMEOUT=10
  _LOG=""
  _ERROR_SWITCH=0
  _EMAIL_SUBJECT=""
  _ERROR_OUT=""
}

function clear_alert() {
  [[ ! -f $_LOG ]] && { touch $_LOG ; return ; }
  if [[ ! -s $_LOG ]] ; then
    return
  else
    logger -t GS-ALERTS "${_EMAIL_SUBJECT}"
    echo "" | mailx -s "${_EMAIL_SUBJECT}" -r kapacitor-alerts@tau.ac.il josh.roden@gigaspaces.com >/dev/null 2>&1
    > $_LOG
  fi
}

function send_alert() {
  if [[ ! -f $_LOG || ! -s $_LOG ]] ; then          # if no logfile or logfile is empty 
    date +%s > $_LOG
    logger -t GS-ALERTS "NB failed services: "${_ERROR_OUT}""
    echo -e "NB failed services:\n\"${_ERROR_OUT}\"" | mailx -s "${_EMAIL_SUBJECT}" -r kapacitor-alerts@tau.ac.il josh.roden@gigaspaces.com >/dev/null 2>&1
    return
  fi
  # Send alert only once a day
  local sec=$( echo "$(date +%s) - $(cat ${_LOG})" | bc )
  [[ $sec -lt 86400 ]] && return
  # After 1 day send another alert
  logger -t GS-ALERTS "NB failed services: "${_ERROR_OUT}""
  echo -e "NB failed services:\n\"${_ERROR_OUT}\"" | mailx -s "${_EMAIL_SUBJECT}" -r kapacitor-alerts@tau.ac.il josh.roden@gigaspaces.com >/dev/null 2>&1
  date +%s > $_LOG
}

function check_one_nb_service() {
  local result host_name=$1 svc=$2 exit_code
  result=$( timeout $_NB_TIMEOUT ssh $host_name "systemctl is-active ${svc}" )
  exit_code=$?
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

function check_nb_services() {
  _ERROR_OUT=""
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
    _EMAIL_SUBJECT="${_TAU_ENV} :: NB SERVICES ALERT."
    send_alert
  else
    _EMAIL_SUBJECT="${_TAU_ENV} :: NB SERVICES OK."
    clear_alert 
  fi
}

############### MAIN ###############

do_env
check_nb_services
