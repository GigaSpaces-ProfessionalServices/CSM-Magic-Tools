#!/bin/bash

### k6 parameterized parallel execution

function usage(){
cat << EOF

Usage:
    $(basename $0) SERVICE_FILE IDS_FILE [Options]

Description:
    Read parameters from file and execute K6 tests (serialized [default] or parallel)

Parameters:
    SERVICE_FILE    Defines which service will be tested. A file with service URL.
    IDS_FILE        Defines which ID(s) will be tested for the selected service.

Options:
    SERVICE-FILE    a file containing the query part of the URI (see service_example file for reference)
    IDS-FILE        a file containing a list of Ids
    -p              execute K6 tests in parallel
    -d              delete old output files before execution
    -v              show verbose information
    -h              show usage

EOF
}

function run_k6() {
    id=$1
    service=$2
    service_name=$(echo $service | cut -d / -f 1 | sed 's/\\//g')
    tmp_file="$output_dir/${service_name}_$id.k6.out"
    echo "Executing: k6 run $scripts_dir/${service_name}_${id}_script.js"
    echo $(date) >> $tmp_file
    k6 run $scripts_dir/${service_name}_${id}_script.js >> $tmp_file
}

function setup_k6_script_parameters(){
    id=$1
    service=$2
    service_name=$(echo $service | cut -d / -f 1 | sed 's/\\//g')
    cp -f $this_root_dir/template.js $scripts_dir/${service_name}_${id}_script.js
    sed -i "s|\[SERVICE\]|$service|g" $scripts_dir/${service_name}_${id}_script.js
    sed -i "s/\[IDNUMBER\]/$id/" $scripts_dir/${service_name}_${id}_script.js
}


### main ###

PARALLEL_EXEC=false
DELETE_OLD=false
VERBOSE=false
num_of_params=0
params=""
pids=""
this_root_dir=$(dirname $(realpath $0))
scripts_dir="$this_root_dir/scripts"
output_dir="$this_root_dir/output"


# parse argumens
if [[ $# -eq 0 ]]; then
    echo "error: missing parameter(s). use '-h' for help."
    exit
fi

while [[ $# -ne 0 ]]; do
    case $1 in
        -p) PARALLEL_EXEC=true ;;
        -v) VERBOSE=true ;;
        -d) DELETE_OLD=true ;;
        -h*) usage ; exit ;;
        *)
            SERVICE_FILE="$(realpath $1)"
            if [[ ! -e $SERVICE_FILE ]]; then
                echo "error: service file '$SERVICE_FILE' not found!"
                exit
            elif ! $(cat $SERVICE_FILE | grep -q "\[IDNUMBER\]"); then
                echo "error: incorrect service file '$SERVICE_FILE' or bad syntax."
                exit
            else
                shift
            fi
            IDS_FILE="$(realpath $1)"
            if [[ ! -e $IDS_FILE ]]; then
                echo "error: Ids file '$IDS_FILE' not found!"
                exit
            else
                shift
            fi
            continue
    esac
    shift
done

# create runtime folders
[[ ! -d $scripts_dir ]] && mkdir $scripts_dir
[[ ! -d $output_dir ]] && mkdir $output_dir

# delete old k6 report files
$DELETE_OLD && rm -f $output_dir/*.k6.out

# create a list of parameters from file
while read -r line; do
    params+=" $line"
    ((num_of_params++))
done < $IDS_FILE
params=$(echo $params | sed "s/^ //; s/ $//")

$VERBOSE && echo "SERVICE_FILE = $SERVICE_FILE"
$VERBOSE && echo "IDS_FILE = $IDS_FILE"
$VERBOSE && echo "PARALLEL_EXEC = $PARALLEL_EXEC"
$VERBOSE && echo "DELETE OLD DATA = $DELETE_OLD"
$VERBOSE && echo "NUMBER OF IDS = $num_of_params"

if [[ $num_of_params == 0 ]]; then
    echo "error: no Ids in file. aborting!"
    exit
fi

# itterate over services and run tests
while read l ; do
    echo $l | grep -q "^#" && continue
    echo $l | grep -q "^$" && continue
    service=$(echo $l | sed 's/[&/]/\\&/g')
    service_name=$(echo $service | cut -d / -f 1 | sed 's/\\//g')
    $VERBOSE && echo "TESTING SERVICE = $service_name"
    for p in $params; do
        setup_k6_script_parameters $p $service
        if $PARALLEL_EXEC ; then
            run_k6 $p $service &
            pids+=" $!"
        else
            run_k6 $p $service
        fi
    done
done < $SERVICE_FILE

if $PARALLEL_EXEC ; then
    while [[ $pids != "" ]]; do
        for p in $pids; do
            if ! kill -0 $p 2>/dev/null; then
                pids=$(echo $pids | sed "s/$p[ ]\{0,1\}//; s/^ //; s/ $//")
                $VERBOSE && echo "pid $p COMPLETED!"
            fi
        done
        sleep 1
    done
fi

# print k6 output
for param in $params; do
    echo ; printf '#%.0s' {1..80} ; echo
    # output results and remove k6 logo
    cat $output_dir/*_${param}.k6.out | egrep -v '(__|/\\|\\/|\|\\)'
done

exit
