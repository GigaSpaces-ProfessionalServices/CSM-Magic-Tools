#!/bin/bash

[[ -x /dbagiga/getUser.sh ]] && _CREDS=( $(/dbagiga/getUser.sh) ) || _CREDS=( user pass ) ; _USER=${_CREDS[0]} ; _PASS=${_CREDS[1]}
_ALL_MANAGER_SERVERS=( $(/usr/local/bin/runall -m -l | grep -v ====) )
_MEASUREMENT_TYPE=""
_TABLES=""
_ONE_TABLE=()
_FAIL_ONLY=""
_TABLE_NUM=0

# Temporary file treatment code
_TMPFILE=/tmp/data-validator-tmp$$
# Function to clean up temporary file
cleanup() {
  #echo "Cleaning up..."
  rm -f "${_TMPFILE}"
}
# Set trap to call cleanup function on script exit
trap cleanup EXIT


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

compare_tables() {
  get_tables
  for t in ${_TABLES[@]} ; do
    unset myarr ; declare -a myarr=( $(get_measurement_ids ${t}) )
    #get_measurement_ids ${t^^}
    [[ ${#myarr[@]} -ne 2 ]] && { echo -e "\nThere are not 2 results for table ${t}.\n" ; continue ; }
    do_compare ${myarr[0]} ${myarr[1]} 2>&1 | grep 'Test Result\|Test Failed\|Details:' > $_TMPFILE
    grep 'Test Failed' $_TMPFILE > /dev/null 2>&1
    local exit_code=$?
    if [[ ( $exit_code -eq 0 && -n $_FAIL_ONLY ) || -z $_FAIL_ONLY ]] ; then
      echo -e "$(( ++_TABLE_NUM ))\t${t}\t\t\t\t$(date)"
      echo -e "The numbers are: ${myarr[@]}"
      cat $_TMPFILE
      [[ "${t}" != "${_TABLES[-1]}" ]] && { echo "sleep 5s" ; sleep 5 ; }
    fi
  done
}


usage() {
  cat << EOF

  USAGE: 

   $(basename $0) [<action>]

  OPTIONS:      

    -t <table name>   Give one table name to check. If this parameter is missing 
                      then compare all tables.
    -lt               List tables (REST call)
    -lm               List measurements
    -f                TBD - Display only failed comparison resultes
    -h                Display usage

  ACTIONS:

    count            Count number of records (default)
    max              Maximum field
    avg              TBD - Average field
    min              TBD - Minimum field

  EXAMPLES:

   $(basename $0) count
   $(basename $0) max -t STUD.KR_CHEDER

EOF
exit
}

do_menu() {
  [[ $# -eq 0 ]] && usage
  while [[ $# -gt 0 ]] ; do
    case $1 in
      "count") 
        _MEASUREMENT_TYPE=count
        ;;
      "max") 
        _MEASUREMENT_TYPE=max
        ;;
      "-t") 
        [[ -z $2 ]] && { echo -e "\nTable name is missing\n" ; exit 1 ; }  || _ONE_TABLE+=( "${2}" )
        shift
        ;;
      "-lt") 
        get_tables ; echo -e "Number of tables: $( echo -e "${_TABLES[@]}" | wc -w)" ; echo -e "${_TABLES[@]}" ; exit 0
        ;;
      "-f") 
        _FAIL_ONLY="yes"
        ;;
      "-lm") 
        cd /dbagiga/gs-odsx ; ./odsx.py datavalidator measurement list ; exit
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

do_menu "${@}"
compare_tables
