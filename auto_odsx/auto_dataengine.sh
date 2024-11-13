#!/bin/bash

# get status from sqlite
# curl <space_server>:<port>/table-feed/status
# IDLE, SUCCESS, IN_PROGRESS, ERROR, Connect failed
# Query feeder_name column:
# SELECT * FROM users WHERE UPPER(name) = UPPER('john');
# sqlite3 /gigawork/sqlite/mssqlFeeder.db <<<"select * from mssql_host_port where UPPER(feeder_name) LIKE '%PORTAL%';"
# file|feeder_name|host|port
# load_Portal_Calendary_View.sh|mssqlfeeder_portal_calendary_view|gsprod-space1|8302

_TIME_TO_WAIT=5
_DB_FILE=""
_LOGFILE=/gigalogs/dataengine_ctm.log
_TABLE=""
_TABLE_NAME=""
_TIMEOUT=1800
# Get user/pass creds
_USER=$(awk -F= '/app.manager.security.username=/ {print $2}' ${ENV_CONFIG}/app.config)
if grep '^app.vault.use=true' ${ENV_CONFIG}/app.config > /dev/null ; then
  _VAULT_PASS=$(awk -F= '/app.manager.security.password.vault=/ {print $2}' ${ENV_CONFIG}/app.config)
  _PASS=$(java -Dapp.db.path=/dbagigawork/sqlite/ -jar /dbagigashare/current/gs/jars/gs-vault-1.0-SNAPSHOT-jar-with-dependencies.jar --get ${_VAULT_PASS})
else
  _PASS=$(awk -F= '/app.manager.security.password=/ {print $2}' ${ENV_CONFIG}/app.config)
fi

usage() {
  cat << EOF

  DESCRIPTION: Synchronously start all feeders for CTM.

  USAGE: $(basename $0) [<option>] [<action>]

  OPTIONS:

  -f <feeder>      Feeder type
  -t <table>       Table name
  
  ACTIONS:

  start            Start feeder
  stop             Stop feeder

  NOTE: Regarding GILBOA FULL LOAD and GILBOA UPDATE, only provide feeder - see EXAMPLES below.

  EXAMPLES:
  $(basename $0) -f oracle -t ta_calendar start
  $(basename $0) -f gilboafull
  $(basename $0) -f gilboaupdate

EOF
exit
}

# parameter: 1=feeder, 2=table, 3=action
db_feeder() {
  echo -e "\n==================== $(date) Start synchronous process for $1 feeder." >> $_LOGFILE
  local feeder=$1 table=$2 action=$3 
  local space_server_name=$(sqlite3 /gigawork/sqlite/oracleFeeder.db <<<"select host from oracle_host_port where LOWER(feeder_name) LIKE '%${table}%';")
  local port=$(sqlite3 /gigawork/sqlite/oracleFeeder.db <<<"select port from oracle_host_port where LOWER(feeder_name) LIKE '%${table}%';")
  local space_server_ip=$(host $space_server_name |awk '{print $NF}')
  local current_query_status=$(curl -s -u ${_USER}:${_PASS} "http://${space_server_ip}:${port}/table-feed/status" | tr -d '"')
  echo feeder=$feeder table=$table action=$action space_server_name=$space_server_name port=$port space_server_ip=$space_server_ip current_query_status=$current_query_status >> $_LOGFILE

  ## do start action and check for "OK"
  echo -e "cd /dbagiga/gs-odsx ; ./odsx.py dataengine ${feeder}-feeder ${action} ${feeder}feeder_${table}" >> $_LOGFILE
  local output=$( cd /dbagiga/gs-odsx ; ./odsx.py dataengine ${feeder}-feeder ${action} ${feeder}feeder_${table} 2>/dev/null )
  echo "Start action sent to table ${table}" >> $_LOGFILE
  [[ "$(echo $output | grep -o '"OK"')" != '"OK"' ]] && return 1

  ## Display status
  local new_query_status=$(curl -s -u ${_USER}:${_PASS} "http://${space_server_ip}:${port}/table-feed/status" | tr -d '"')
  echo current_query_status="${current_query_status}" new_query_status="${new_query_status}" >> $_LOGFILE
  echo "show output:" >> $_LOGFILE ; echo $output >> $_LOGFILE
  echo "show exit code:" >> $_LOGFILE
  echo $output | grep -o '"OK"' >> $_LOGFILE

  ## Get time and status
  echo "Waiting $_TIME_TO_WAIT seconds before starting IN_PROGRESS loop." >> $_LOGFILE ; sleep $_TIME_TO_WAIT
  local start_time=$(date +%s -d now)
  local timeout_time=$(( start_time + _TIMEOUT ))
  new_query_status=$(curl -s -u ${_USER}:${_PASS} "http://${space_server_ip}:${port}/table-feed/status" | tr -d '"')
  echo "new_query_status=$new_query_status timeout=$_TIMEOUT start_time=${start_time} timeout_time=${timeout_time}" >> $_LOGFILE

  ## loop until query status not IN_PROGRESS or timeout
  while [[ "${new_query_status}" == "IN_PROGRESS" &&  $(date +%s -d now) -lt $timeout_time ]] ; do
    echo "start time=$start_time current time=$(date +%s -d now) timeout time=$timeout_time time left=$(( timeout_time - $(date +%s -d now) ))" >> $_LOGFILE
    sleep $_TIME_TO_WAIT
    new_query_status=$(curl -s -u ${_USER}:${_PASS} "http://${space_server_ip}:${port}/table-feed/status" | tr -d '"')
  done

  echo "exiting IN_PROGRESS loop, new_query_status=${new_query_status}" >> $_LOGFILE
  [[ "${new_query_status}" != "SUCCESS" ]] && return 1 || return 0
}

do_db_feeder() {
  while [[ $# -gt 0 ]] ; do
    case $1 in
      "-f") shift ; feeder="${1,,}" 
        ;;
      "-t") shift ; table="${1,,}" 
        ;;
      "start") action=start 
        ;;
      *) echo -e "\nOption/Action $1 not supported.\n" ; usage 
        ;;
    esac
    shift
  done
  db_feeder $feeder $table $action
  db_feeder_exit_code=$?
  echo Finished db_feeder db_feeder_exit_code=$db_feeder_exit_code >> $_LOGFILE
  exit $db_feeder_exit_code
}

gilboafull_feeder() {
  echo -e "\n==================== $(date) Start synchronous process for gilboafull feeder." >> $_LOGFILE
  local table="${_TABLE,,}"
  local table_name="${_TABLE_NAME}"
  local space_server_name=$(sqlite3 /gigawork/sqlite/$_DB_FILE <<<"select host from mssql_host_port where LOWER(feeder_name) LIKE '%${table}%';")
  local port=$(sqlite3 /gigawork/sqlite/$_DB_FILE <<<"select port from mssql_host_port where LOWER(feeder_name) LIKE '%${table}%';")
  local space_server_ip=$(host $space_server_name |awk '{print $NF}')
  local current_query_status=$(curl -s -u ${_USER}:${_PASS} "http://${space_server_ip}:${port}/table-feed/status" | tr -d '"')
  echo feeder=$feeder table=$table action=$action space_server_name=$space_server_name port=$port space_server_ip=$space_server_ip current_query_status=$current_query_status >> $_LOGFILE
  #[[ "${current_query_status}" != "IDLE" && "${current_query_status}" != "SUCCESS" ]] && return 1

  ## do start action and check for "OK"
  #echo -e "curl -XPOST \"http://${space_server_ip}:${port}/table-feed/start?table-name=${table_name}&base-column=v_timestamp&clear-before-start=true\"" >> $_LOGFILE
  #local output=$(curl -XPOST "http://${space_server_ip}:${port}/table-feed/start?table-name=${table_name}&base-column=v_timestamp&clear-before-start=true" 2>/dev/null )
  local output=$( /gigashare/current/mssql/scripts/load_Portal_Calendary_View.sh $space_server_ip $port 2>/dev/null )
  echo "Start action sent to table ${table}" >> $_LOGFILE
  [[ "$(echo $output | grep -o '"OK"')" != '"OK"' ]] && return 1

  ## Display status
  local new_query_status=$(curl -s -u ${_USER}:${_PASS} "http://${space_server_ip}:${port}/table-feed/status" | tr -d '"')
  echo current_query_status="${current_query_status}" new_query_status="${new_query_status}" >> $_LOGFILE
  echo "show output:" >> $_LOGFILE ; echo $output >> $_LOGFILE
  echo "show exit code:" >> $_LOGFILE
  echo $output | grep -o '"OK"' >> $_LOGFILE

  ## Get time and status
  echo "Waiting $_TIME_TO_WAIT seconds before starting IN_PROGRESS loop." >> $_LOGFILE ; sleep $_TIME_TO_WAIT
  local start_time=$(date +%s -d now)
  local timeout_time=$(( start_time + _TIMEOUT ))
  new_query_status=$(curl -s -u ${_USER}:${_PASS} "http://${space_server_ip}:${port}/table-feed/status" | tr -d '"')
  echo "new_query_status=$new_query_status timeout=$_TIMEOUT start_time=${start_time} timeout_time=${timeout_time}" >> $_LOGFILE

  ## loop until query status not IN_PROGRESS or timeout
  while [[ "${new_query_status}" == "IN_PROGRESS" &&  $(date +%s -d now) -lt $timeout_time ]] ; do
    echo "start time=$start_time current time=$(date +%s -d now) timeout time=$timeout_time time left=$(( timeout_time - $(date +%s -d now) ))" >> $_LOGFILE
    sleep $_TIME_TO_WAIT
    new_query_status=$(curl -s -u ${_USER}:${_PASS} "http://${space_server_ip}:${port}/table-feed/status" | tr -d '"')
  done

  echo "exiting IN_PROGRESS loop, new_query_status=${new_query_status}" >> $_LOGFILE
  if [[ "${new_query_status}" != "SUCCESS" ]] ; then return 1 ; else return 0 ; fi
}

do_gilboafull_feeder() {
  #  /giga/utils/auto_odsx/auto_gilboafeederfullloadstart
  gilboafull_feeder
  gilboafull_feeder_exit_code=$?
  echo -e "Finished gilboafull_feeder gilboafull_feeder_exit_code=${gilboafull_feeder_exit_code}" >> $_LOGFILE
  exit $gilboafull_feeder_exit_code
}

gilboaupdate_feeder() {
  echo -e "\n==================== $(date) Start synchronous process for gilboaupdate feeder." >> $_LOGFILE
  local table="${_TABLE,,}"
  local table_name="${_TABLE_NAME}"
  local space_server_name=$(sqlite3 /gigawork/sqlite/$_DB_FILE <<<"select host from gilboa_host_port where LOWER(feeder_name) LIKE '%${table}%';")
  local port=$(sqlite3 /gigawork/sqlite/$_DB_FILE <<<"select port from gilboa_host_port where LOWER(feeder_name) LIKE '%${table}%';")
  local space_server_ip=$(host $space_server_name |awk '{print $NF}')
  local current_query_status=$(curl -s -u ${_USER}:${_PASS} "http://${space_server_ip}:${port}/table-feed/status" | tr -d '"')
  echo feeder=$feeder table=$table action=$action space_server_name=$space_server_name port=$port space_server_ip=$space_server_ip current_query_status=$current_query_status >> $_LOGFILE
  #[[ "${current_query_status}" != "IDLE" && "${current_query_status}" != "SUCCESS" ]] && return 1

  ## do start action and check for "OK"
  echo -e "curl -XPOST \"http://${space_server_ip}:${port}/table-feed/start?table-name=${table_name}&base-column=v_timestamp\"" >> $_LOGFILE
  local output=$(curl -XPOST "http://${space_server_ip}:${port}/table-feed/start?table-name=${table_name}&base-column=v_timestamp" 2>/dev/null)
  echo "Start action sent to table ${table}" >> $_LOGFILE
  [[ "$(echo $output | grep -o '"OK"')" != '"OK"' ]] && return 1

  ## Display status
  local new_query_status=$(curl -s -u ${_USER}:${_PASS} "http://${space_server_ip}:${port}/table-feed/status" | tr -d '"')
  echo current_query_status="${current_query_status}" new_query_status="${new_query_status}" >> $_LOGFILE
  echo "show output:" >> $_LOGFILE ; echo $output >> $_LOGFILE
  echo "show exit code:" >> $_LOGFILE
  echo $output | grep -o '"OK"' >> $_LOGFILE

  ## Get time and status
  echo "Waiting $_TIME_TO_WAIT seconds before starting IN_PROGRESS loop." >> $_LOGFILE ; sleep $_TIME_TO_WAIT
  local start_time=$(date +%s -d now)
  local timeout_time=$(( start_time + _TIMEOUT ))
  new_query_status=$(curl -s -u ${_USER}:${_PASS} "http://${space_server_ip}:${port}/table-feed/status" | tr -d '"')
  echo "new_query_status=$new_query_status timeout=$_TIMEOUT start_time=${start_time} timeout_time=${timeout_time}" >> $_LOGFILE

  ## loop until query status not IN_PROGRESS or timeout
  while [[ "${new_query_status}" == "IN_PROGRESS" &&  $(date +%s -d now) -lt $timeout_time ]] ; do
    echo "start time=$start_time current time=$(date +%s -d now) timeout time=$timeout_time time left=$(( timeout_time - $(date +%s -d now) ))" >> $_LOGFILE
    sleep $_TIME_TO_WAIT
    new_query_status=$(curl -s -u ${_USER}:${_PASS} "http://${space_server_ip}:${port}/table-feed/status" | tr -d '"')
  done

  echo "exiting IN_PROGRESS loop, new_query_status=${new_query_status}" >> $_LOGFILE
  [[ "${new_query_status}" != "SUCCESS" ]] && return 1 || return 0
}

do_gilboaupdate_feeder() {
  #  /giga/utils/auto_odsx/auto_gilboafeederupdatestart
  gilboaupdate_feeder
  gilboaupdate_feeder_exit_code=$?
  echo -e "Finished gilboaupdate_feeder gilboaupdate_feeder_exit_code=${gilboaupdate_feeder_exit_code}" >> $_LOGFILE
  exit $gilboaupdate_feeder_exit_code
}

################# MAIN #################

[[ $# -eq 0 ]] && usage 

case "${2,,}" in 
  "gilboafull")
    _DB_FILE="mssqlFeeder.db"
    _TABLE="Portal_Calendary_View"
    _TABLE_NAME="dbo.Portal_Calendary_View"
    do_gilboafull_feeder "${@}"
    echo _DB_FILE=$_DB_FILE >> $_LOGFILE
    ;;
  "gilboaupdate")
    _DB_FILE="gilboaFeeder.db"
    _TABLE="Portal_Calendary_Changes_View"
    _TABLE_NAME="dbo.Portal_Calendary_View"
    do_gilboaupdate_feeder "${@}"
    echo _DB_FILE=$_DB_FILE >> $_LOGFILE
    ;;
  *)
    _DB_FILE="${2,,}Feeder.db"
    do_db_feeder "${@}"
    echo _DB_FILE=$_DB_FILE >> $_LOGFILE
    ;;
esac
