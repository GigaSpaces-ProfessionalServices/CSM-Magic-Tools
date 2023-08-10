#!/bin/bash

function usage() {
    printf "\n%-10s\n%7s%-50s\n" "Usage:" " " "$(basename $0) [arguments] [start|stop|...]"
    printf "\n%-10s\n" "Description:"
    printf "%7s%-50s\n" " " "Execute odsx dataengine command(s)"
    case $1 in
        err0)
            printf "\nERROR: At least one argument is missing.\n"
            printf "See help [-h, --help] for options.\n"
            exit 1
            ;;
        *)
            printf "\n%-10s\n" "required arguments:"
            printf "%2s%-20s%-50s\n" "" "-f <FEEDER TYPE>" "the feeder type (db2, mssql ...)"
            printf "%2s%-20s%-50s\n" "" "-t <TABLE NAME>" "the table name"
            printf "%2s%-5s%-15s%-50s\n" "" "-h, " "--help" "Display this help screen"
            printf "\n"
    esac
}

### main ###
ZONE=bll
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

# if any argument is missing we exit with code 1
[[ -z $the_feeder ]] || [[ -z $table_name ]] || [[ -z $opt ]] && usage err0

# build table name for dataengine
if [[ $the_feeder == 'mssql' ]]; then
    table_name="${the_feeder}${TYPE_SUFFIX}_${ZONE}_${table_name}"
else
    table_name="${the_feeder}${TYPE_SUFFIX}_${table_name}"
fi
# build feeder name for dataengine
the_feeder="${the_feeder}-${TYPE_SUFFIX}"

# run command
cd /dbagiga/gs-odsx
./odsx.py dataengine $the_feeder $opt $table_name
