#!/bin/bash

_GS_HOME="/dbagiga/gigaspaces-smart-ods"
_ALL_SPACE_SERVERS=( $(host-yaml.sh -s) )
_ALL_MANAGER_SERVERS=( $(host-yaml.sh -m) )
_MANAGER1=${_ALL_MANAGER_SERVERS}
_ORACLE_FEEDER_PATH="/giga/yuval/TAU/deployment/ORACLE_FEEDER"
# feeder name directories e.g. 10_SL_TOAR (only dir names matching types/feeders)
_ALL_ORACLE_FEEDERS=( $(ls -1 $_ORACLE_FEEDER_PATH | sed -En '/[0-9]+_.*/p') )
_ORACLE_HOST=$(awk -F= '/oracle\.host/ {print $2}' /root/oracle_creds.txt)
_ORACLE_USER=$(awk -F= '/oracle\.user/ {print $2}' /root/oracle_creds.txt)
_ORACLE_PASSWORD=$(awk -F= '/oracle\.password/ {print $2}' /root/oracle_creds.txt)


# TBD - Determine which host to deploy to (one with smallest no. of feeders deployed)
#_SPACE1=${_ALL_SPACE_SERVERS}
#_DEPLOY_HOST_NAME=$( [[ $_SPACE1 =~ [0-9]+\.[0-9] ]] && dig -x $_SPACE1 +short |cut -d. -f1 || echo $_SPACE1 )
#_NUM_OF_DEPLOYED_FEEDERS=0

# Create associative arrays:
# _FEEDERS_ARRAY - TBD
# _FEEDERS_ARRAY_DIR_NAME - e.g. key/value array entry - [1]="1_KR_CHEDER" (this is dir name)
unset       _FEEDERS_ARRAY_DIR_NAME
declare -A  _FEEDERS_ARRAY_DIR_NAME

# TBD
# Find host with least amount of containers
#find_deploy_host() {
#  # Set to no. of deployed feeders of a space server for comparing loop (the current value of _DEPLOY_HOST_NAME)
#  _NUM_OF_DEPLOYED_FEEDERS=$( curl -s http://${_MANAGER1}:8090/v2/containers | jq '.[] |.instances[],.id' |grep -A1 _feeder |grep $_DEPLOY_HOST_NAME | wc -l)
#  for h in ${_ALL_SPACE_SERVERS[@]} ; do
#    [[ $h =~ [0-9]+\.[0-9] ]] && host_name=$( dig -x $h +short |cut -d. -f1 ) || host_name=$h
#    local tmp_num_of_deployed_feeders=$( curl -s http://${_MANAGER1}:8090/v2/containers | jq '.[] |.instances[],.id' |grep -A1 _feeder |grep $host_name | wc -l)
#    echo host=$host_name feeder_count=$tmp_num_of_deployed_feeders
#    [[ $tmp_num_of_deployed_feeders -lt $_NUM_OF_DEPLOYED_FEEDERS ]] && { _DEPLOY_HOST_NAME=$host_name ; _NUM_OF_DEPLOYED_FEEDERS=$tmp_num_of_deployed_feeders ; } || echo "$tmp_num_of_deployed_feeders <= $_NUM_OF_DEPLOYED_FEEDERS "
#  done
#  echo _NUM_OF_DEPLOYED_FEEDERS=$_NUM_OF_DEPLOYED_FEEDERS _DEPLOY_HOST_NAME=$_DEPLOY_HOST_NAME
#}

f_copyjar() {
  cd /giga/yuval/TAU/deployment/ORACLE_FEEDER/
  for f in $(ls -d1 *_*_*) ; do 
    echo -e "Copying for ${f}" 
    \cp OracleFeeder-1.0-SNAPSHOT.jar "${f}/${f%%_*}_feeder.jar"
  done
}

find_non_deployed_feeders() {
  deployed=$(curl -s http://${_MANAGER1}:8090/v2/containers | jq -r '.[] |.instances[],.id'|grep _feeder)
  for (( key=1 ; key<=${#_FEEDERS_ARRAY_DIR_NAME[@]} ; key++ )) ; do
    local found=0
    for d in $deployed ; do
      [[ $d =~ ${_FEEDERS_ARRAY_DIR_NAME[${key}]#*_} ]] && found=1
    done
    [[ $found -ne 1 ]] && echo "Feeder not deployed: ${_FEEDERS_ARRAY_DIR_NAME[${key}]}"
  done
}

#TBD
create_feeder_array() {
  for f in ${_ALL_ORACLE_FEEDERS[@]} ; do     # e.g. [10]="1_KR_CHEDER"
    local fnum=${f%%_*} # echo fnum=$fnum
    local fname=${f#*_} # echo fname=$fname
    _FEEDERS_ARRAY[${fnum}]="${fname}"        # e.g. [1]="1_KR_CHEDER"
  done
}

# e.g. key/value array entry - [1]="1_KR_CHEDER"
create_feeders_array_dir_name() {
  for f in ${_ALL_ORACLE_FEEDERS[@]} ; do       # e.g. [10]="1_KR_CHEDER"
    fnum=${f%%_*} # echo fnum=$fnum
    _FEEDERS_ARRAY_DIR_NAME[${fnum}]="${f}"     # e.g. [1]="1_KR_CHEDER", [2]="2_KR_KURS", etc...
  done
}

# List dir names iterized e.g. "1     KR_CHEDER"
list_feeders_array_dir_name() {
  echo -e "\nList of feeders"
  echo -e "---------------"
  #for key in "${!_FEEDERS_ARRAY_DIR_NAME[@]}" ; do
  for (( key=1 ; key <= ${#_FEEDERS_ARRAY_DIR_NAME[@]} ; key++ )) ; do
    printf "%-5s %-10s\n" "${key}" "${_FEEDERS_ARRAY_DIR_NAME[${key}]#*_}"
  done
  echo
}

usage() {
  cat << EOF

  USAGE: $(basename $0) [<option>]

  OPTION:
  -h                                -Show this help
  -l                                -List all feeders by number and name
  -nondep                           -List non deployed feeders
  -a                                -Deploy all feeders
  -d <feeder_number>                -Deploy a specific feeder by number e.g. "1" will deploy "KR_CHEDER_feeder"
  -u <feeder_number>                -Undeploy a specific feeder by number e.g. "1" will undeploy "KR_CHEDER_feeder"
  -ua                               -Undeploy all feeder deployments
  -k <feeder_number>                -Kill a specific feeder by number e.g. "1" will kill "KR_CHEDER_feeder"
  -ka                               -Kill all feeder containers
  -c <feeder_number>                -Create a specific feeder by number e.g. "1" will create "KR_CHEDER_feeder"
  -start <feeder_number>            -Start a specific feeder by number e.g. "1" will start "KR_CHEDER_feeder"
  -start10 <feeder_number>          -Start a specific feeder by number and limit to 10 records
  -startlimitx <feeder_number> X    -Start a specific feeder by number and limit to X records
  -startall                         -Start all feeders
  -startall10                       -Start all feeders and limit to 10 records 
  -startalllimitx X                 -Start all feeders and limit to X records 
  -stop <feeder_number>             -Stop a specific feeder by number e.g. "1" will stop "KR_CHEDER_feeder"
  -stopall                          -Stop a specific feeder by number e.g. "1" will stop "KR_CHEDER_feeder"
  -copyjar                          -Copy OracleFeeder-1.0-SNAPSHOT.jar to all feeder directories
  -ddl                              -Load Types/Tables from /giga/yuval/TAU/deployment/DDL_PARSER

  DEFAULT OPTION:
  -h

  EXAMPLE:
  $(basename $0) -d 1       -Deploy feeder no. 1 which is "KR_CHEDER"

EOF
}

# example:
# feeder name: KR_CHEDER_feeder
# feeder zone: 1_KR_CHEDER_feeder
# feeder dir name: 1_KR_CHEDER
# feeders' directory path: /giga/yuval/TAU/deployment/ORACLE_FEEDER
# full path of 1st feeder: /giga/yuval/TAU/deployment/ORACLE_FEEDER/1_KR_CHEDER

# Input = table number e.g. "1"
# Arguments needed 
# 1. feeder number (which is used to find feeder name e.g. KR_CHEDER_feeder)
f_undeploy() {
  local feeder_name="${_FEEDERS_ARRAY_DIR_NAME[${1}]#*_}_feeder"     # 1 --> KR_CHEDER_feeder
  $_GS_HOME/bin/gs.sh service undeploy $feeder_name
}

f_undeploy_all() {
  for (( f=1 ; f<= ${#_FEEDERS_ARRAY_DIR_NAME[@]} ; f++ )) ; do
    f_undeploy $f
  done
}

# Input = table number e.g. "1"
# Arguments needed 
# 1. feeder number (which is used to find feeder zone e.g. 1_KR_CHEDER_feeder)
f_kill() {
  local zone_name="${_FEEDERS_ARRAY_DIR_NAME[${1}]}_feeder"   # "1" --> 1_KR_CHEDER_feeder
  $_GS_HOME/bin/gs.sh container kill --zones=$zone_name
}

f_kill_all() {
  for (( f=1 ; f<= ${#_FEEDERS_ARRAY_DIR_NAME[@]} ; f++ )) ; do
    f_kill $f
  done
}

# Input = table number e.g. "1"
# Arguments needed 
# 1. feeder zone name       e.g. 1_KR_CHEDER_feeder
# 2. target space server    e.g. gssb1
f_create() {
  local zone_name="${_FEEDERS_ARRAY_DIR_NAME[${1}]}_feeder"   # "1" --> 1_KR_CHEDER_feeder
  # Choose target space server using modulu - i.e. feeder 1 minus 1 will deploy on 1st space (0)
  local space_server_target=${_ALL_SPACE_SERVERS[$( echo "( $1 - 1 ) % 3 "|bc )]}  # 0, 1, 2 (for DEV spaces)
  $_GS_HOME/bin/gs.sh container create --count=1 --memory=256m --zone=$zone_name $space_server_target 
}

# ? spc_where_feeder_is_deployed:feeder_port 
# table-name=STUD.feeder_name
f_start() {
  [[ -z $1 ]] && { echo -e "\nMust specify feeder number.\n" ; exit 1 ; }
  local space_server_target=${_ALL_SPACE_SERVERS[$( echo "( $1 - 1 ) % 3 "|bc )]}  # 0, 1, 2 (for DEV spaces)
  local deploy_port=$(( 8015 + $1 - 1 ))
  local table_name="STUD.${_FEEDERS_ARRAY_DIR_NAME[${1}]#*_}"    # e.g. STUD.KR_CHEDER
#  curl -XPOST "http://gssb2.tau.ac.il:8015/table-feed/start?table-name=STUD.KR_CHEDER&base-column=T_IDKUN"
curl -XPOST "http://${space_server_target}:${deploy_port}/table-feed/start?table-name=${table_name}&base-column=T_IDKUN"
# curl -XPOST "http://${space_server_target}:${deploy_port}/table-feed/start?table-name=${table_name}&base-column=T_IDKUN&rows-limit=10"
echo " $1 "
}

f_start10() {
  [[ -z $1 ]] && { echo -e "\nMust specify feeder number.\n" ; exit 1 ; }
  local space_server_target=${_ALL_SPACE_SERVERS[$( echo "( $1 - 1 ) % 3 "|bc )]}  # 0, 1, 2 (for DEV spaces)
  local deploy_port=$(( 8015 + $1 - 1 ))
  local table_name="STUD.${_FEEDERS_ARRAY_DIR_NAME[${1}]#*_}"    # e.g. STUD.KR_CHEDER
  curl -XPOST "http://${space_server_target}:${deploy_port}/table-feed/start?table-name=${table_name}&base-column=T_IDKUN&rows-limit=10"
  echo " $1 "
}

f_start_limit_x() {
  local feeder_number=$1
  echo feeder_number=$1
  local custom_rows_limit=$2
  echo custom_rows_limit=$2
  local space_server_target=${_ALL_SPACE_SERVERS[$( echo "( $feeder_number - 1 ) % 3 "|bc )]}  # 0, 1, 2 (for DEV spaces)
  local deploy_port=$(( 8015 + $feeder_number - 1 ))
  local table_name="STUD.${_FEEDERS_ARRAY_DIR_NAME[${feeder_number}]#*_}"    # e.g. STUD.KR_CHEDER
curl -XPOST "http://${space_server_target}:${deploy_port}/table-feed/start?table-name=${table_name}&base-column=T_IDKUN&rows-limit=${custom_rows_limit}"
echo " $feeder_number "
}

f_startall() {
  for (( f=1 ; f<= ${#_FEEDERS_ARRAY_DIR_NAME[@]} ; f++ )) ; do
    f_start $f
  done
}

f_startall10() {
  for (( f=1 ; f<= ${#_FEEDERS_ARRAY_DIR_NAME[@]} ; f++ )) ; do
    f_start10 $f
  done
}

f_start_all_limit_x() {
  [[ -z $1 ]] && { echo -n "\nMust specify rows_limit.\n" ; exit 1 ; }
  local custom_rows_limit=$1
  for (( f=1 ; f<= ${#_FEEDERS_ARRAY_DIR_NAME[@]} ; f++ )) ; do
    f_start_limit_x $f $custom_rows_limit
  done
}

f_stop() {
  local space_server_target=${_ALL_SPACE_SERVERS[$( echo "( $1 - 1 ) % 3 "|bc )]}  # 0, 1, 2 (for DEV spaces)
  local deploy_port=$(( 8015 + $1 - 1 ))
#  curl -XPOST "http://gssb2.tau.ac.il:8015/table-feed/stop"
curl -XPOST "http://${space_server_target}:${deploy_port}/table-feed/stop"
echo " $1"
}

f_stopall() {
  for (( f=1 ; f<= ${#_FEEDERS_ARRAY_DIR_NAME[@]} ; f++ )) ; do
    f_stop $f
  done
}

# Input = table number e.g. "1"
# Arguments needed 
# 1. feeder name            e.g. KR_CHEDER_feeder
# 2. feeder zone name       e.g. 1_KR_CHEDER_feeder
# 3. deploy port            e.g. 8015 + feeder number - 1 = 8015 (if feeder number = 1)
# 4. target space server    e.g. gssb1
# JAR should be in feeder directory (cp ../OracleFeeder-1.0-SNAPSHOT.jar 1_feeder.jar)
# rest.port=(8015 + feeder number - 1), e.g. 8015+1-1=8015 (for 1_KR_CHEDER)
f_deploy() {
  f_undeploy $1
  f_kill $1
  f_create $1
  local feeder_name="${_FEEDERS_ARRAY_DIR_NAME[${1}]#*_}_feeder"     # 1 --> KR_CHEDER_feeder
  local zone_name="${_FEEDERS_ARRAY_DIR_NAME[${1}]}_feeder"   # "1" --> 1_KR_CHEDER_feeder
  local deploy_port=$(( 8015 + $1 - 1 ))
  local space_server_target=${_ALL_SPACE_SERVERS[$( echo "( $1 - 1 ) % 3 "|bc )]}  # 0, 1, 2 (for DEV spaces)
$_GS_HOME/bin/gs.sh service deploy $feeder_name --zones=$zone_name -p=space.name=dih-tau-space -p=rest.port=$deploy_port -p=rest.locators=${_MANAGER1} -p=oracle.host="${_ORACLE_HOST}" -p=oracle.database=STUD -p=oracle.user="${_ORACLE_USER}" -p=oracle.password="${_ORACLE_PASSWORD}" -p=feeder.writeBatchSize=400 -p=feeder.sleepAfterWriteInMillis=500 ${_ORACLE_FEEDER_PATH}/${_FEEDERS_ARRAY_DIR_NAME[${1}]}/${1}_feeder.jar
}

f_deploy_all() {
  local num_of_feeders=${#_FEEDERS_ARRAY_DIR_NAME[@]}
  for (( f=1 ; f<= $num_of_feeders ; f++ )) ; do
    f_deploy $f
  done
}

do_menu() {
  case $1 in
    "-h"|"") usage ;;
    "-l") list_feeders_array_dir_name ;;
    "-nondep") find_non_deployed_feeders ;;
    "-a") f_deploy_all ;;
    "-d") f_deploy $2 ;;
    "-u") f_undeploy $2 ;;
    "-ua") f_undeploy_all ;;
    "-k") f_kill $2 ;;
    "-ka") f_kill_all ;;
    "-c") f_create $2 ;;
    "-start") f_start $2 ;;
    "-start10") f_start10 $2 ;;
    "-startlimitx") f_start_limit_x $2 $3 ;;
    "-startall") f_startall ;;
    "-startall10") f_startall10 ;;
    "-startalllimitx") f_start_all_limit_x $2 ;;
    "-stop") f_stop $2 ;;
    "-stopall") f_stopall ;;
    "-copyjar") f_copyjar ;;
    "-ddl") cd /giga/yuval/TAU/deployment/DDL_PARSER ; ./deploy.sh ;;
    *) echo -e "\nInvalid option\n" ;;
  esac
  exit
}

############### MAIN ###############

create_feeders_array_dir_name
do_menu "$@"
