#!/bin/bash

function usage() {
    printf "\n%-10s\n%7s%-50s\n" "Usage:" " " "$(basename $0) [arguments] [start|stop|...]"
    printf "\n%-10s\n" "Description:"
    printf "%7s%-50s\n" " " "Execute odsx dataengine command(s)"
    case $1 in
        err0)
            printf "\nERROR: At least one argument is missing.\n"
            printf "See help [-h, --help] for options.\n"
            ;;
    esac
    printf "\n%-10s\n" "Arguments:"
    printf "%2s%-20s%-50s\n" "" "-f <FEEDER TYPE>" "one of: oracle|mssql|db2"
    printf "%2s%-20s%-50s\n" "" "-t <TABLE NAME>" "the table name"
    printf "%2s%-5s%-15s%-50s\n" "" "-h, " "--help" "Display this help screen"
    printf "\n"
    exit
}

### main ###
date

# Waiting random miliseconds before executing
sleep $( bc -l <<< "scale=3 ; ${RANDOM}/32767" )

ZONE=""
TYPE_SUFFIX="feeder"

while [[ $# -gt 0 ]]; do
    case $1 in
        -f)
            the_feeder=$(echo $2 | tr '[:upper:]' '[:lower:]')
            shift
            ;;
        'start'|'stop')
            opt=$1 ;;
        -t)
            table_name=$(echo $2 | tr '[:upper:]' '[:lower:]')
            shift
            ;;
        -h|--help) usage && exit ;;
    esac
    shift
done

# if any argument is missing we show usage and exit
[[ -z $the_feeder ]] || [[ -z $table_name ]] || [[ -z $opt ]] && usage err0

# build table name
if [[ $the_feeder == 'mssql' ]]; then
    table_name="${the_feeder}${TYPE_SUFFIX}_${ZONE}_${table_name}"
elif [[ $the_feeder == 'oracle' ]]; then
    table_name="${the_feeder}${TYPE_SUFFIX}_${table_name}"
else
    table_name="${the_feeder}${TYPE_SUFFIX}_${table_name}"
fi

# build feeder name
the_feeder="${the_feeder}-${TYPE_SUFFIX}"

# run command
echo -e ./odsx.py dataengine $the_feeder $opt $table_name
cd /dbagiga/gs-odsx
./odsx.py dataengine $the_feeder $opt $table_name
