#!/bin/bash
#sed -n "/^$(date "+%Y-%m-%d")/p" /dbagigalogs/sanity/ods_sanity.log |grep -i 'CRITICAL\|error\|down\|fail\|inactive' | less
do_env() {
source ~/.bash_profile
[[ -x /dbagiga/getUser.sh ]] && _CREDS=( $(/dbagiga/getUser.sh) ) || _CREDS=( user pass ) ; _USER=${_CREDS[0]} ;  _PASS=${_CREDS[1]}
_ODSGS=( $(host-yaml.sh m) )
[[ "${1}" == "-q" ]] && _QUIET="-q"
}

service_hc() {
[[ "${_QUIET}" != "-q" ]] && { echo ; read -sn1 -p "Press any key to display services 611 613 614: " ; echo ; }
echo -e "\n==================== Display health check of services\n"
nbapp=( $(host-yaml.sh na) )
ip_port_DB_CENTRAL_branch_table_DKTB611=( $(ssh $nbapp "sed -n '/upstream DB_CENTRAL_branch_table_DKTB611/,/}/p' /etc/nginx/conf.d/microservices.conf" | grep server | awk '{print $2}'|tr -d ';') )
echo -e "Health check for DB_CENTRAL_branch_table_DKTB611: $(curl -su ${_USER}:${_PASS} http://${ip_port_DB_CENTRAL_branch_table_DKTB611}/v1/actuator/health)" 
ip_port_DB_Central_Index_Table_exchange_rates_DKTB613=( $(ssh $nbapp "sed -n '/upstream DB_Central_Index_Table_exchange_rates_DKTB613/,/}/p' /etc/nginx/conf.d/microservices.conf" | grep server | awk '{print $2}'|tr -d ';') )
echo -e "Health check for DB_Central_Index_Table_exchange_rates_DKTB613: $(curl -su ${_USER}:${_PASS} http://${ip_port_DB_Central_Index_Table_exchange_rates_DKTB613}/v1/actuator/health)" 
ip_port_DB_Central_ForeignCurrencyTable_DKTB614=( $(ssh $nbapp "sed -n '/upstream DB_Central_ForeignCurrencyTable_DKTB614/,/}/p' /etc/nginx/conf.d/microservices.conf" | grep server | awk '{print $2}'|tr -d ';') )
echo -e "Health check for DB_Central_ForeignCurrencyTable_DKTB614: $(curl -su ${_USER}:${_PASS} http://${ip_port_DB_Central_ForeignCurrencyTable_DKTB614}/v1/actuator/health)" 
}

query_mssql_tables() {
[[ "${_QUIET}" != "-q" ]] && { echo ; read -sn1 -p "Press any key to query central tables (mssql): " ;echo ; }
echo -e "\n==================== Display query of all tables\n"
_TABLES=( $(curl -u ${_USER}:${_PASS} -s http://${_ODSGS}:8090/v2/internal/spaces/utilization  | jq '.[]."objectTypes"|keys|.[]' | tr -d '"' |grep BLL) )

echo -e "\nList of tables:\n ${_TABLES[@]}"

for t in ${_TABLES[@]} ; do
  echo -e "\n\n==================== Query for Central Table: ${t}"
  mycurl=$(echo -e "curl -sku ${_USER}:${_PASS} \x27http://${_ODSGS}:8090/v2/spaces/bllspace/query?typeName=${t}&maxResults=1\x27")
  echo -e "${mycurl}"
  eval $mycurl
  sleep .7
done
}

do_entries() {
  [[ "${_QUIET}" != "-q" ]] && { echo ; read -sn1 -p "Press any key to continue" ;echo ; } || { echo ; echo ; }
  echo -e "\n==================== entries and tieredEntries of all tables\n"
  check-ts-size-of-tables.sh
}

# grep "$(date +%Y-%m-%d)" /dbagigalogs/sanity/ods_sanity.log | grep -i "fail\|error\|warning\|===========================" |grep -v 'Follower'
check_sanity() {
  [[ "${_QUIET}" != "-q" ]] && { echo ; read -sn1 -p "Press any key to check sanity" ;echo ; }
  echo -e "\n==================== Check sanity\n"
  local exclude_txt='Follower\|ms-digital'        # generic exclude
  local include_txt='fail\|error\|warning\|==========================='
  #exclude per environment in addition to above generic exclude
  case $ENV_NAME in
    'GRG' ) exclude_txt=( ${exclude_txt}'\|KAPACITOR' ) ;;
    'DEV' ) exclude_txt=( ${exclude_txt}'\|KAPACITOR' ) ;;
    'STG' ) exclude_txt=( ${exclude_txt} ) ;;
    'DR' ) exclude_txt=( ${exclude_txt}'\|ping\|network consistency' ) ;;
    'PRD' ) exclude_txt=( ${exclude_txt}'\|ping\|network consistency' ) ;;
  esac
  grep "$(date +%Y-%m-%d)" /dbagigalogs/sanity/ods_sanity.log | grep -i "${include_txt}" | grep -iv "${exclude_txt}"
  local num_lines=$(grep "$(date +%Y-%m-%d)" /dbagigalogs/sanity/ods_sanity.log |wc -l)
  echo -e "\nNo. of lines in sanity check: ${num_lines}\n"
  [[ $num_lines -lt 240 ]] && echo -e "WARNING: check number of lines in sanity check\n"
}

####################################### MAIN

do_env "$@"
pb-same-server
service_hc
query_mssql_tables
do_entries
check_sanity
