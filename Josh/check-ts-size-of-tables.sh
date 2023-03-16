#!/bin/bash

_LOG="/dbagigalogs/chaos_monkey_instance.log"
_NUM_OF_TABLES=0      # No. of db2feeder tables
_TABLES=""            # List of tables
_ODS_MNG=( $(/dbagiga/utils/host-yaml.sh manager) )       # List ODS managers
# The user and pass used in curl command
[[ -x /dbagiga/getUser.sh ]] && _CREDS=( $(/dbagiga/getUser.sh) ) || _CREDS=( user pass ) ; _USER=${_CREDS[0]} ; _PASS=${_CREDS[1]}
# The cap size for db2feeder tables

get_tables() {
#  _TABLES=($(curl -u ${_USER}:${_PASS} -s http://${_ODS_MNG}:8090/v2/internal/spaces/utilization  | jq '.[]."tieredConfiguration"|keys|.[]'|grep 'JOTB[MP]\|BLL_DKTB' | tr -d '"' |grep -v SEGMENT))
  _TABLES=($(curl -u ${_USER}:${_PASS} -s http://${_ODS_MNG}:8090/v2/internal/spaces/utilization  | jq '.[]."objectTypes"|keys|.[]' | tr -d '"' ))
  _NUM_OF_TABLES=$(echo ${_TABLES[@]} |wc -w)
  echo -e "$(date) CAP: tables = ${_TABLES[@]}"
  echo -e "$(date) CAP: Number of tables=${_NUM_OF_TABLES}"
}

get_table_entries_count() {
  curl -u ${_USER}:${_PASS} -s http://${_ODS_MNG}:8090/v2/internal/spaces/utilization  | jq -c ".[] |."objectTypes"|.${1} | .entries"
}

get_table_tieredEntries_count() {
  curl -u ${_USER}:${_PASS} -s http://${_ODS_MNG}:8090/v2/internal/spaces/utilization  | jq -c ".[] |."objectTypes"|.${1} | .tieredEntries"
}

############## MAIN

echo -e "$(date) Number of records for all Objects DISK/RAM:" >> $_LOG
get_tables
for t in ${_TABLES[@]} ; do
  printf "Table: %-25s Disk/Ram: %12d %12d\n" "${t}" $(get_table_entries_count ${t}) $(get_table_tieredEntries_count ${t}) | tee -a $_LOG
done
