#!/bin/bash

_LOG="/gigalogs/oracle-cap.log"
_NUM_OF_TABLES=0      # No. of db2feeder tables
_TABLES=""            # List of tables
_ALL_MANAGERS=( $(/giga/utils/host-yaml.sh -m) )       # List ODS managers
_MANAGER1=${_ALL_MANAGERS[0]}
# The user and pass used in curl command
[[ -x /dbagiga/getUser.sh ]] && _CREDS=( $(/dbagiga/getUser.sh) ) || _CREDS=( user pass ) ; _USER=${_CREDS[0]} ; _PASS=${_CREDS[1]}

# The cap size for oracle tables
_DEFAUTL_CAP=1000

if [[ "${ENV_NAME}" == "TAUG" && -z $_DEFAUTL_CAP ]] ; then
  KR_CHEDER=5000
  KR_KURS=5000
  KR_KVUTZA=5000
  KR_KVUTZA_MOED=5000
  KR_KVUTZA_SEGEL=5000
  KR_KVUTZA_ZAMAK=5000
  MM_TOCHNIT_LIMUD=5000
  SL_HESHBON=5000
  SL_TNUA=5000
  SL_TOAR=5000
  TA_CALENDAR=5000
  TA_HODAA=5000
  TA_KTOVET=5000
  TA_PERSON=5000
  TA_PRATIM=5000
  TA_SEM=5000
  TB_002_OFEN_HORAA=5000
  TB_020_MADAD=5000
  TB_029_MAAMAD=5000
  TB_032_MATZAV_BACHUG=5000
  TB_059_SCL_TOSEFET=5000
  TB_060_TOAR=5000
  TB_069_SCL_PEULA=5000
  TB_071_SIMUL_TZIUN=5000
  TB_082_HEARA_TALMID=5000
  TB_104_SUG_MATALA=5000
  TB_911_BINYAN=5000
  TB_962_TOAR_MORE=5000
  TB_975_CALNDR_ERUA=5000
  TL_HEARA=5000
  TL_KURS=5000
  TL_KVUTZA=5000
  TL_MOED_TZIUN=5000
  TL_TOAR=5000
  TL_TOCHNIT=5000
else 
  echo -e "\nWrong ENV_NAME\n" ; exit
fi

# Leumi: auto_dataengine.sh -f db2 -t jotbmf01_tn_mati start

get_tables() {
  _TABLES=($( curl -u ${_USER}:${_PASS} -s http://${_ALL_MANAGERS}:8090/v2/internal/spaces/utilization  | jq '.[]."tieredConfiguration"|keys|.[]'|grep 'JOTB[MP]' | tr -d '"' |grep -v SEGMENT ))
  echo -e "$(date) CAP: tables = ${_TABLES[@]}" | tee -a $_LOG 
  _NUM_OF_TABLES=$(echo ${_TABLES[@]} |wc -w)
  echo -e "$(date) CAP: Number of tables=${_NUM_OF_TABLES}" | tee -a $_LOG
}

check_record_count() {
  curl -u ${_USER}:${_PASS} -s http://${_ALL_MANAGERS}:8090/v2/internal/spaces/utilization  | jq -c ".[] |."objectTypes"|.${1} | .entries"
}

############## MAIN

get_tables
while (( _NUM_OF_TABLES-- )) ; do 
  table_name=${_TABLES[${_NUM_OF_TABLES}]}
  table_cap_size=$(( $table_name ))
  temp_rec_count=$(check_record_count ${table_name})
  echo table_name=$table_name table_cap_size=$table_cap_size temp_rec_count=$temp_rec_count
  echo -e "$(date) CAP: Starting feeder for ${table_name}"
  /dbagiga/utils/auto_odsx/auto_dataengine.sh -f db2 -t $table_name start
  total_writes=0 ; total_writes_per_sec=0 ; writes_index=0
  while true ; do
    loop_start=$(date +%s -d 'now')
    sleep 30
    feeder_status=$(auto_db2feederlist 2>&1 | grep -i $table_name |grep -i 'ERROR\|SUCCESS' >/dev/null 2>&1 ; echo $?)
    rec_count=$(check_record_count ${table_name})
    echo table_name=$table_name table_cap_size=$table_cap_size _NUM_OF_TABLES left = $(( _NUM_OF_TABLES + 1 ))
    loop_time=$(( $(date +%s -d 'now') - loop_start ))
    writes_per_sec=$(( ($rec_count - $temp_rec_count) / $loop_time )) 
    total_writes_per_sec=$(( total_writes_per_sec+=writes_per_sec ))
    (( total_writes+=$(( $rec_count - $temp_rec_count )) )) ; (( writes_index++ ))
    echo -e "$(date) CAP: rec_count=$rec_count temp_rec_count=$temp_rec_count loop_start=$loop_start loop_stop=$(date +%s -d 'now') loop_time=${loop_time}"
    echo -e "$(date) CAP: Writes per sec: ${writes_per_sec} avg writes per loop: $(( total_writes / writes_index )) avg writes per sec: $(( total_writes_per_sec / writes_index ))"  | tee -a $_LOG
    if [[ $feeder_status -eq 0 || $rec_count -ge ${table_cap_size} ]] ; then
      echo -e "$(date) CAP: Stoping feeder for ${table_name} feeder_status=$feeder_status rec_count=${rec_count}" | tee -a $_LOG
      /dbagiga/utils/auto_odsx/auto_dataengine.sh -f db2 -t $table_name stop ; break
    fi
    temp_rec_count=$rec_count
  done
done
