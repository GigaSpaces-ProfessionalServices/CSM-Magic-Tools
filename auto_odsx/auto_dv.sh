#!/bin/bash

do_env() {
export ENV_CONFIG=/gigashare/env_config
# Get user/pass creds
_USER=$(awk -F= '/app.manager.security.username=/ {print $2}' ${ENV_CONFIG}/app.config)
if grep '^app.vault.use=true' ${ENV_CONFIG}/app.config > /dev/null ; then
  _VAULT_PASS=$(awk -F= '/app.manager.security.password.vault=/ {print $2}' ${ENV_CONFIG}/app.config)
  _PASS=$(java -Dapp.db.path=/dbagigawork/sqlite/ -jar /dbagigashare/current/gs/jars/gs-vault-1.0-SNAPSHOT-jar-with-dependencies.jar --get ${_VAULT_PASS})
else
  _PASS=$(awk -F= '/app.manager.security.password=/ {print $2}' ${ENV_CONFIG}/app.config)
fi
_ALL_MANAGER_SERVERS=( $(/usr/local/bin/runall -m -l | grep -v ===) )
MANAGER=${_ALL_MANAGER_SERVERS[0]}
BASE_URL="http://${MANAGER}:8090/v2"
SPACE_ID=$(curl -u ${_USER}:${_PASS} -ks "${BASE_URL}/spaces" | jq -r '.[].name' | head -1)
LOOKUP_GROUP=$(grep -o "groups=gs-tau-[a-z]\{3\}" ${ENV_CONFIG}/app.config | head -1 | awk -F= '{print $2}')
case $LOOKUP_GROUP in
  "gs-tau-dev") ENV_NAME=TAUG ;;
  "gs-tau-stg") ENV_NAME=TAUS ;;
  "gs-tau-prd") ENV_NAME=TAUP ;;
esac
_MEASUREMENT_TYPE="count"
_TABLES=""
_ONE_TABLE=()
_FAIL_ONLY=""
_TABLE_NUM=0
_LOGC=/tmp/jrbatchc_influx
_LOGM=/tmp/jrbatchm_influx
_DV_MAX_LOG=/gigalogs/auto_dv_max.log
_DV_COUNT_LOG=/gigalogs/auto_dv_count.log
declare -g -A _INFLUX
# Create and indexed array holding all CDC tables
#[[ "${ENV_NAME}" != "TAUG" ]] && _CDC_TABLES=( $( ssh $(runall -d -l |grep -v === | head -1) /giga/scripts/listPipelineTables.sh |grep STUD) )
_CDC_TABLES=()
get_CDC_object_data
HOST_NAME=$(hostname)
#_CDC_TABLES=( $(cat /tmp/jrcdctables) )
# Temporary file treatment code
#_TMPFILE=/tmp/data-validator-tmp$$
_TMP_COMPARE=""
# Function to clean up temporary file
#cleanup() {
#  #echo "Cleaning up..."
#  rm -f "${_TMPFILE}"
#}
## Set trap to call cleanup function on script exit
#trap cleanup EXIT
}

get_tables() {
  if [[ -n $_ONE_TABLE ]] ; then
    _TABLES=( "${_ONE_TABLE[@]}" )
  else
    if [[ "${_MEASUREMENT_TYPE}" == "max" ]] ; then
    _TABLES=($(curl -u ${_USER}:${_PASS} -s http://${_ALL_MANAGER_SERVERS}:8090/v2/internal/spaces/utilization  | jq -r '.[]."objectTypes"|keys|.[]' | grep '^STUD'))
    else
    _TABLES=($(curl -u ${_USER}:${_PASS} -s http://${_ALL_MANAGER_SERVERS}:8090/v2/internal/spaces/utilization  | jq -r '.[]."objectTypes"|keys|.[]' | grep '^dbo\|^STUD'))
    fi
  fi
  [[ -z $_TABLES ]] && { echo -e "No TYPES are loaded\n" ; exit 1 ; }
}

# $1 is the name of the table e.g. 
get_measurement_ids() {
  cd /dbagiga/gs-odsx ; ./odsx.py datavalidator measurement list | grep "${_MEASUREMENT_TYPE}" | awk -F'|' '/'\'''"${1}"''\''/ {print $2}' | sed -E 's/\x1B\[[0-9;]*[a-zA-Z]//g; s/[^[:print:]]//g; s/[ \t]//g'
}

# Compare measurement (e.g. count) between space and source db
#
#------------------------------------------------------------
#Test Result: PASS
#Details: Results matched. gigaspaces-Result: 3979  oracle-Result: 3979
#------------------------------------------------------------
do_compare() {
compare1="${1}" compare2="${2}" /usr/bin/expect -c '
    set num1 "$env(compare1)"
    set num2 "$env(compare2)"
    set timeout -1
    set force_conservative 1
    cd /dbagiga/gs-odsx
    spawn ./odsx.py datavalidator compare run
    expect "Select 1st measurement Id for comparison"

    # Process num1
    set num1Len [string length $num1]
    for {set i 0} {$i < $num1Len} {incr i} {
        set digit [string index $num1 $i]
        sleep .5
        send -- "$digit"
    }
    sleep .5
    send -- "\r"

    expect "Select 2nd measurement Id for comparison"

    # Process num2
    set num2Len [string length $num2]
    for {set i 0} {$i < $num2Len} {incr i} {
        set digit [string index $num2 $i]
        sleep .5
        send -- "$digit"
    }
    sleep .5
    send -- "\r"

    expect "Execution time delay"
    sleep .5
    send -- "\r"

    expect "Do you want to store results in influxdb"
    sleep .5
    send -- "y"
    sleep .5
    send -- "e"
    sleep .5
    send -- "s"
    sleep .5
    send -- "\r"

    expect eof
    '
}

stop_services() {
/usr/bin/expect -c '
set timeout -1
set force_conservative 1

cd /dbagiga/gs-odsx
spawn ./odsx.py datavalidator install stop
expect "stop all servers"
sleep .5
send -- "\r"
sleep .5
send -- "y"
sleep .5
send -- "\r"

expect eof
'
}

start_services() {
/usr/bin/expect -c '
set timeout -1
set force_conservative 1

cd /dbagiga/gs-odsx
spawn ./odsx.py datavalidator install start
expect "start all servers"
sleep .5
send -- "\r"
sleep .5
send -- "y"
sleep .5
send -- "\r"

expect eof
'
}

start_server_service() {
/usr/bin/expect -c '
set timeout -1
set force_conservative 1

cd /dbagiga/gs-odsx
spawn ./odsx.py datavalidator install start
expect "if you want to start individual server"
sleep .5
send -- "1"
sleep .5
send -- "\r"
expect "Enter host Sr Number to start"
sleep .5
send -- "1"
sleep .5
send -- "\r"

expect eof
'
}

remove_services() {
/usr/bin/expect -c '
set timeout -1
set force_conservative 1

cd /dbagiga/gs-odsx
spawn ./odsx.py datavalidator install remove
expect "Enter user to connect to Data validation server"
sleep .5
send -- "\r"
expect "To remove all"
sleep .5
send -- "\r"
sleep .5
send -- "\r"

expect eof
'
}

install_server_service() {
/usr/bin/expect -c '
set timeout -1
set force_conservative 1

cd /dbagiga/gs-odsx
spawn ./odsx.py datavalidator install installserver
expect "Are you sure want to install Data Validation Service server"
sleep .5
send -- "\r"

expect eof
'
}

install_agents_service() {
/usr/bin/expect -c '
set timeout -1
set force_conservative 1

cd /dbagiga/gs-odsx
spawn ./odsx.py datavalidator install installagents
expect "Are you sure want to install Data Validation Service agents"
sleep .5
send -- "\r"

expect eof
'
}


compare_tables() {
  get_tables
  for t in ${_TABLES[@]} ; do
    echo -e "----------------------------------------------------------------"
    lsof /gigawork/sqlite/datavalidator.db | tail -n +2
    unset myarr ; declare -a myarr=( $(get_measurement_ids ${t}) )
    #get_measurement_ids ${t^^}
    [[ ${#myarr[@]} -ne 2 ]] && { echo -e "\nThere are not 2 results for table ${t}.\n" ; continue ; }
    _TMP_COMPARE=$( do_compare ${myarr[0]} ${myarr[1]} 2>&1 | grep 'Test Result\|Test Failed\|Details:' )
    echo "${_TMP_COMPARE}" | grep 'Test Failed' > /dev/null 2>&1
    local exit_code=$?
    if [[ ( $exit_code -eq 0 && -n $_FAIL_ONLY ) || -z $_FAIL_ONLY ]] ; then
      if echo ${_CDC_TABLES[@]} | grep -w "${t}" >/dev/null 2>&1 ; then
        echo -e "$(( ++_TABLE_NUM ))\t${t} CDC\t\t\t\t$(date)"
      else
        echo -e "$(( ++_TABLE_NUM ))\t${t}\t\t\t\t$(date)"
      fi
      echo -e "The numbers are: ${myarr[@]}"
      echo "${_TMP_COMPARE}"
      #[[ "${t}" != "${_TABLES[-1]}" ]] && { echo "sleep 5s" ; sleep 5 ; }
    fi
  done
}

list_tables_column() {
  get_tables ; echo -e "=== Number of tables: $( echo -e "${_TABLES[@]}" | wc -w)"
  for t in ${_TABLES[@]} ; do
    [[ $(echo ${_CDC_TABLES[@]} | grep -w "${t}" >/dev/null 2>&1 ; echo $?) -eq 0 ]] && echo $t CDC || echo $t
  done
}

git_compile_reinstall() {
#[[ ! -d /dbagiga/REPOS/CSM-Magic-Tools/ ]] && { echo -e "/dbagiga/REPOS/CSM-Magic-Tools does not exist" ; exit ; }
ssh gs-jenkins.tau.ac.il "
cd /dbagiga/REPOS/CSM-Magic-Tools/
echo cd /dbagiga/REPOS/CSM-Magic-Tools/ exit_code=\$?
git checkout tau
echo git checkout tau exit_code=\$?
git pull
echo git pull exit_code=\$?
cd common
echo exit_code=\$?
mvn clean install -DskipTests
echo mvn clean install exit_code=\$?
cd ../data-validator/
echo exit_code=\$?
mvn clean install -DskipTests
echo mvn clean install exit_code=\$?
"
for h in dev stg prd ; do
scp -3 gs-jenkins.tau.ac.il:/dbagiga/REPOS/CSM-Magic-Tools/common/target/common-0.0.1-SNAPSHOT-jar-with-dependencies.jar \
${h}:/gigashare/current/data-validator/jars/
scp -3 gs-jenkins.tau.ac.il:/dbagiga/REPOS/CSM-Magic-Tools/data-validator/data-validator-server/target/data-validator-server-0.0.1-SNAPSHOT.jar \
${h}:/gigashare/current/data-validator/jars/
scp -3 gs-jenkins.tau.ac.il:/dbagiga/REPOS/CSM-Magic-Tools/data-validator/data-validator-agent/target/data-validator-agent-0.0.1-SNAPSHOT.jar \
${h}:/gigashare/current/data-validator/jars/
done
ls -l /gigashare/current/data-validator/jars/
auto_dv.sh -reinstall
}

batch_count() {
/usr/bin/expect -c '
set timeout -1
set force_conservative 1

cd /dbagiga/gs-odsx
spawn ./odsx.py datavalidator batchexecute execute
expect "Select Test type"
sleep .5
send -- "c"
sleep .5
send -- "o"
sleep .5
send -- "u"
sleep .5
send -- "n"
sleep .5
send -- "t"
sleep .5
send -- "\r"

expect eof
'
}

batch_max() {
/usr/bin/expect -c '
set timeout -1
set force_conservative 1

cd /dbagiga/gs-odsx
spawn ./odsx.py datavalidator batchexecute execute
expect "Select Test type"
sleep .5
send -- "m"
sleep .5
send -- "a"
sleep .5
send -- "x"
sleep .5
send -- "\r"

expect eof
'  
}

function do_batchm2() {
  local num=1 dvmax_out dvmax line table gigaspaces oracle table_type
  dvmax_out="$( batch_max | tee -a $_DV_MAX_LOG | sed -E 's/\x1B\[[0-9;]*[a-zA-Z]//g ; s/[^[:print:]]//g ; s/[ \t]//g' )"
  echo -e "\nNumber of tables processed: $(echo -e "${dvmax_out}" | grep 'dbo\.\|STUD\.' | wc -l)"
  dvmax=$( echo -e "${dvmax_out}" | grep 'dbo\.\|STUD\.' )
  while read line ; do 
    table=$( echo $line | awk -F\| '{print $4}' )
    gigaspaces=$( echo "$( echo $line | grep -o 'gigaspaces-Result:[0-9]*' | awk -F: '{print $2}') / 1000" | bc )
    oracle=$( echo "$( echo $line | grep -o 'oracle-Result:[0-9]*' | awk -F: '{print $2}') / 1000" | bc )
    echo "${_CDC_TABLES[@]}" | grep -w "${table}" >/dev/null 2>&1
    [[ $? -eq 0 ]] && table_type=CDC || table_type=FEEDER
    printf "%3d %-50s %s     %s  %s %s\n" "$((num++))" "${table}" "GS = $(date -d @${gigaspaces} +"%Y-%m-%d %H:%M:%S" )" "ORA = $( date -d @${oracle} +"%Y-%m-%d %H:%M:%S" )" "$(echo $line | awk -F\| '{print $5}')" "${table_type}"
  done < <(echo "${dvmax}")
  echo -e "${dvmax_out}" | grep 'dbo\.\|STUD\.' | grep 'gigaspaces-Result:FAIL'
}

get_CDC_object_data() {
# Get list of CDC tables (tables that have field ZZ_META_DI_TIMESTAMP)
objectTypesMetadata=$(curl -u ${_USER}:${_PASS} -sk ${BASE_URL}/spaces/${SPACE_ID}/objectsTypeInfo | jq -r '.objectTypesMetadata[]')
while read -r obj; do
    [[ $obj =~ SHOB ]] && continue
    has_zz_meta_di_timestamp=$(echo $objectTypesMetadata | \
    jq -r "select(.objectName == \"$obj\") | .schema" | \
    jq '. | any(.name == "ZZ_META_DI_TIMESTAMP")')
    $has_zz_meta_di_timestamp && _CDC_TABLES+=( $obj )
done < <(curl -u ${_USER}:${_PASS} -sk "${BASE_URL}/spaces/${SPACE_ID}/statistics/types" | jq --raw-output 'keys[]' | grep -v "java.lang.Object" | sort -n)
}

# myarr[STUD.TL_KVUTZA]="1"
# [[ -z ${_INFLUX[${t}]} ]] && echo empty || echo not empty
# [[ "${line}" =~ gigaspaces-Result:[0-9]+ && "${line}" =~ oracle-Result:[0-9]+ ]]
function batchm_influx() {
  local dvmax_out line result table table_type
  dvmax_out="$( batch_max | grep 'dbo\.\|STUD\.' | sed -E 's/\x1B\[[0-9;]*[a-zA-Z]//g ; s/[^[:print:]]//g ; s/[ \t]//g' )"
  #dvmax_out="$(cat /tmp/jrbatchmsed)"
  > $_LOGM
  while read line ; do 
    read table result < <( echo "${line}" | awk -F\| '{ print $4 " " $5 }' )
    echo $table $result >> $_LOGM
    if [[ -z ${_INFLUX[${table}]} ]] ; then       # if not exist then add table with result
      _INFLUX[${table}]=$result
    elif [[ $result == "FAIL" ]] ; then           # if exist then set value only if result="FAIL" - At least one FAIL = FAIL
        _INFLUX[${table}]=$result
    fi
  done < <(echo "${dvmax_out}") 
  #for table in ${!_INFLUX[@]} ; do echo $table ${_INFLUX[${table}]} ; done | sort > $_LOGM
  sort $_LOGM > ${_LOGM}2
}

function batchc_influx() {
  local dvcount_out line result table table_type
  dvcount_out="$( batch_count | grep 'dbo\.\|STUD\.' | sed -E 's/\x1B\[[0-9;]*[a-zA-Z]//g ; s/[^[:print:]]//g ; s/[ \t]//g')"
  #dvcount_out="$(cat /tmp/jrbatchc)"
  > $_LOGC
  while read line ; do 
    read table result < <( echo "${line}" | awk -F\| '{ print $4 " " $5 }' )
    echo $table $result >> $_LOGC
    if [[ -z ${_INFLUX[${table}]} ]] ; then       # if not exist then add table with result
      _INFLUX[${table}]=$result
    elif [[ $result == "FAIL" ]] ; then           # if exist then set value only if result="FAIL" - At least one FAIL = FAIL
        _INFLUX[${table}]=$result
    fi
  done < <(echo "${dvcount_out}") 
  #for table in ${!_INFLUX[@]} ; do echo $table ${_INFLUX[${table}]} ; done | sort > $_LOGC
  sort $_LOGC > ${_LOGC}2
}

# e.g. dvState,env=TAUS host=gstest-pivot obj_type=STUD.TM_SEGEL result=1 state=PASS table_type=CDC
function batch_influx() {
  local table result state table_type
  for table in ${!_INFLUX[@]} ; do
    if echo "${_CDC_TABLES[@]}" | grep -w "${table}" >/dev/null 2>&1 ; then table_type=CDC ; else table_type=FEEDER ; fi
    result=1 ; [[ "${_INFLUX[${table}]}" == "FAIL" ]] && result=0
    state=${_INFLUX[${table}]}
    echo "dvState,env=${ENV_NAME},host=${HOST_NAME},obj_type=${table} result=${result}i,state=\"${state}\",table_type=\"${table_type}\""
  done
}


usage() {
  cat << EOF

  USAGE: 

   $(basename $0) [<action>] [<option>]

  OPTIONS:      

    -t <table name>   Give one TABLE NAME to check. If this parameter is missing 
                      then compare all tables.
    -lt               lIST TABLES (REST call)
    -lt1              LIST TABLES 1 line per table (REST call)
    -l                LIST STATUS of server and agents
    -lm               LIST Measurements
    -stop             STOP all server and agent services
    -start            START all server and agent services
    -restart          RESTART all server and agent services
    -reinstall        REINSTALL server and agents
    -c                GIT PULL, COMPILE and REINSTALL server and agents
    -f                TBD - Display only failed comparison resultes
    -h                Display usage

  ACTIONS:

    batch_influx     BATCH MAX and COUNT aggregation for influx
    batchm_influx    BATCH MAX for influx
    batchc_influx    BATCH COUNT for influx
    batchc           BATCH Compare COUNT
    batchm           BATCH Compare MAX
    batchm2          BATCH Compare MAX and convert from epoc to regular date and time as a list
    batchcm          BATCH Compare COUNT and MAX
    count            Loop Compare COUNT
    max              Loop Compare MAX (except dbo.Portal_Calendary_View)
    cm               Loop Compare COUNT and MAX
    avg              TBD - Average field
    min              TBD - Minimum field

  EXAMPLES:

    $(basename $0) count                                            # Loop compare COUNT for all tables
    $(basename $0) max -t STUD.KR_CHEDER                            # Compare MAX for single table named STUD.KR_CHEDER
    $(basename $0) -t "STUD.KR_CHEDER STUD.TB_032_MATZAV_BACHUG"    # Loop compare COUNT (default) for more than 1 table
    $(basename $0) cm                                               # Loop compare COUNT and MAX for all tables
    $(basename $0) batchc                                           # BATCH Compare COUNT

EOF
exit
}

do_menu() {
  while [[ $# -gt 0 ]] ; do
    case $1 in
      "count") 
        _MEASUREMENT_TYPE=count
        ;;
      "max") 
        _MEASUREMENT_TYPE=max
        ;;
      "batchc") 
        batch_count ; exit
        ;;
      "batchc_influx") 
        batchc_influx ; exit
        ;;
      "batchm") 
        batch_max ; exit
        ;;
      "batchm_influx") 
        batchm_influx ; exit
        ;;
      "batchm2") 
        exit
        do_batchm2 ; exit
        ;;
      "batch_influx") 
        batchm_influx
        batchc_influx
        batch_influx | sort | tee /gigalogs/dv_for_grafana ; exit
        ;;
      "batchcm") 
        auto_dv.sh batchc
        auto_dv.sh batchm
        exit
        ;;
      "cm") 
        auto_dv.sh count
        auto_dv.sh max
        exit
        ;;
      "-t") 
        [[ -z $2 ]] && { echo -e "\nTable name is missing\n" ; exit 1 ; }  || _ONE_TABLE+=( "${2}" )
        shift
        ;;
      "-lt") 
        get_tables ; echo -e "=== Number of tables: $( echo -e "${_TABLES[@]}" | wc -w)" ; echo -e "${_TABLES[@]}" ; exit 0
        ;;
      "-lt1") 
        list_tables_column ; exit
        ;;
      "-f") 
        _FAIL_ONLY="yes"
        ;;
      "-l") 
        cd /dbagiga/gs-odsx ; ./odsx.py datavalidator install list ; exit
        ;;
      "-lm") 
        cd /dbagiga/gs-odsx ; ./odsx.py datavalidator measurement list ; exit
        ;;
      "-stop") 
        stop_services ; exit
        ;;
      "-start") 
        start_services ; exit
        ;;
      "-restart") 
        stop_services ; start_services ; cd /dbagiga/gs-odsx ; ./odsx.py datavalidator install list ; exit
        ;;
      "-reinstall") 
        stop_services ; remove_services ; install_server_service ; start_server_service
        echo -e "\nSleeping 15 sec ..." ; sleep 15 ; install_agents_service ; start_services ; auto_dv.sh -l ; exit
        ;;
      "-c")
        git_compile_reinstall ; exit
        ;;
      "-h") 
        usage
        ;;
      *) 
        echo -e "\n Option $1 not supported.\n" ; exit 1
        ;;
    esac
    shift
  done
}

################ MAIN ################
[[ $# -eq 0 || "${1}" == "-h" ]] && usage
do_env
do_menu "${@}"
compare_tables
