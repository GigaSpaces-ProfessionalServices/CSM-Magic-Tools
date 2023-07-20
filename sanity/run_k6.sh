#!/bin/bash

if [[ -z $1 ]]; then
    echo "missing random id parameter" ; exit
else
    id=$1
fi

root_dir="/dbagiga/utils/sanity"
k6_dir="${root_dir}/k6"
tmp_file="${root_dir}/k6-${id}.out"
filter='checks|data_received|data_sent|http_req_failed|http_reqs|iteration_duration|vus'

script_name="script.js"
# get service name from script
service_name=$(cat ${k6_dir}/${script_name} | grep "let url" | cut -d/ -f4)

k6 run ${k6_dir}/${script_name} >> $tmp_file
if [[ -f $tmp_file ]]; then
    echo "service: ${service_name}" > ${tmp_file}.tmp
    echo >> ${tmp_file}.tmp
    cat $tmp_file | egrep '(scenarios|\* )' | sed 's/ *//' >> ${tmp_file}.tmp
    echo >> ${tmp_file}.tmp
    cat $tmp_file | grep 'status' | sed 's/ *//' >> ${tmp_file}.tmp
    echo >> ${tmp_file}.tmp
    cat $tmp_file | sed -n '/status/,/vus_max/p' | egrep "(${filter})" | sed 's/ *//' >> ${tmp_file}.tmp
    mv ${tmp_file}.tmp ${tmp_file}.report
    rm -f ${tmp_file}
fi
