#!/bin/bash
[[ ! -x /dbagiga/utils/host-yaml.sh ]] && { echo -e "Can not find host-yaml.sh - exiting" ; exit 1 ; }
[[ $(which jq > /dev/null 2>&1 ; echo $?) -ne 0 ]] && { echo "need to install jq" ; exit 1 ; }

#_NUM_OF_TABLES=0      # Number of tables
_TABLES=""            # List of tables
_ALL_MANAGER_SERVERS=( $(/dbagiga/utils/host-yaml.sh -m) )       # List ODS managers
# The user and pass used in curl command
[[ -x /dbagiga/getUser.sh ]] && _CREDS=( $(/dbagiga/getUser.sh) ) || _CREDS=( user pass ) ; _USER=${_CREDS[0]} ; _PASS=${_CREDS[1]}

get_tables() {
  echo -e "\n$(date) ============== Number of records for all Objects DISK/RAM:\n"
  _TABLES=($(curl -u ${_USER}:${_PASS} -s http://${_ALL_MANAGER_SERVERS}:8090/v2/internal/spaces/utilization  | jq -r '.[]."objectTypes"|keys|.[]' ))
  [[ -z $_TABLES ]] && { echo -e "No TYPES are loaded\n" ; exit 1 ; }
  #_NUM_OF_TABLES=$(echo ${_TABLES[@]} |wc -w)
}

get_table_entries_count() {
  curl -u ${_USER}:${_PASS} -s http://${_ALL_MANAGER_SERVERS}:8090/v2/internal/spaces/utilization  | jq -c ".[] |.\"objectTypes\" | .\"${1}\" | .\"entries\" "
}

get_table_tieredEntries_count() {
  curl -u ${_USER}:${_PASS} -s http://${_ALL_MANAGER_SERVERS}:8090/v2/internal/spaces/utilization  | jq -c ".[] |.\"objectTypes\" | .\"${1}\" | .\"tieredEntries\""
}

show_tables() {
printf "%-30s %12s %12s\n" "TABLE" "DISK" "RAM"
printf "%-30s %12s %12s\n" "-----" "----" "---"
for t in ${_TABLES[@]} ; do
  printf "%-30s %12d %12d\n" "${t}" "$(get_table_entries_count ${t})" "$(get_table_tieredEntries_count ${t})"
done
echo
}

############## MAIN

get_tables
show_tables
