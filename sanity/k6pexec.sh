#!/bin/bash

### k6 parameterized parallel execution

function usage(){
cat << EOF
Usage:
        $(basename $0) PARAMS_FILE [Options]

Description:
        Execute K6 tests serialized (default) or in parallel

Options:
    -p  execute K6 tests in parallel
    -d  delete old output files before execution
    -v  show verbose information
    -h  show usage
EOF
}

function run_k6() {
    id=$1
    tmp_file=/$output_dir/$id.k6.out
    
    #k6 run $script >> $tmp_file    
    echo "executing: k6 run $script >> $tmp_file"
    echo $(date) >> $tmp_file
    sleep $(expr $RANDOM / 10000 + 1)
}

function setup_k6_script_parameters(){
    param=$1
    # cp template and insert $param here
    #cp script_template.js $scripts_dir/${param}_script.js
    #sed -i "s/\[IDNUMBER\]/$param/" $scripts_dir/${param}_script.js
    echo "running setup_k6_script_parameters for $param"
}


### main ###

PARALLEL_EXEC=false
DELETE_OLD=false
VERBOSE=false
num_of_params=0
params=""
pids=""
scripts_dir="scripts"
output_dir="output"

# parse argumens
if [[ $# -eq 0 ]]; then
    echo "error: missing parameter(s)!"
    exit
fi

while [[ $# -ne 0 ]]; do
    case $1 in
        -p) PARALLEL_EXEC=true ;;
        -v) VERBOSE=true ;;
        -d) DELETE_OLD=true 
        -h*) usage ; exit ;;
        *) PARAMS_FILE=$1 ;;
    esac
    shift
done

$VERBOSE && echo "PARAMS_FILE = $PARAMS_FILE"
$VERBOSE && echo "PARALLEL_EXEC = $PARALLEL_EXEC"

# check if parameters file exists
if [[ ! -e $PARAMS_FILE ]]; then
    echo "error: parameter(s) file not found!"
    exit
fi

# create runtime folders
[[ ! -d $scripts_dir ]] && mkdir $scripts_dir
[[ ! -d $output_dir ]] && mkdir $output_dir

# create a list of parameters from file
while read -r line; do
    params+=" $line"
    ((num_of_params++))
done < $PARAMS_FILE
params=$(echo $params | sed "s/^ //; s/ $//")

$VERBOSE && echo "NUMBER OF PARAMETERS = $num_of_params"

if [[ $num_of_params == 0 ]]; then
    echo "error: no params in file. aborting!"
    exit
fi

for p in $params; do
    setup_k6_script_parameters $p
    if $PARALLEL_EXEC ; then
        run_k6 $p &
        pids+=" $!"
    else
        run_k6 $p
    fi
done

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
    echo "K6 REPORT FOR: $param"
    cat /tmp/${param}.k6.out
done

$DELETE_OLD && \rm $output_dir/*.k6.out

exit