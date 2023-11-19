#!/bin/bash

function get_table_threshold() {
    local tbl=$1

    for t in ${!SHOB_TABLES[*]}; do
        if [[ "$t" == "$tbl" ]]; then
            echo "${SHOB_TABLES[$t]}"
            return
        fi
    done
    echo "null"
}

# tables thresholds dictionary
# * using bash v4.x associative array
declare -A SHOB_TABLES
SHOB_TABLES=([STUD.KR_KURS]=18000 \
[STUD.KR_KVUTZA]=18000 \
[STUD.KR_KVUTZA_MOED]=18000 \
[STUD.KR_KVUTZA_SEGEL]=18000 \
[STUD.KR_KVUTZA_ZAMAK]=18000 \
[STUD.MJ_HODAOT_WEB]=86400 \
[STUD.MM_TOCHNIT_LIMUD]=86400 \
[STUD.MM_YEHIDA]=86400 \
[STUD.SL_HESHBON]=18000 \
[STUD.SL_TNUA]=18000 \
[STUD.TA_HODAA]=3600 \
[STUD.TA_IDS]=86400 \
[STUD.TA_KTOVET]=86400 \
[STUD.TA_PERSON]=86400 \
[STUD.TA_PRATIM]=86400 \
[STUD.TL_HEARA]=18000 \
[STUD.TL_KURS]=3600 \
[STUD.TL_TOAR]=86400 \
[STUD.TL_TOCHNIT]=86400 \
[STUD.TA_CALENDAR]=86400 \
[STUD.TB_020_MADAD]=86400 \
[STUD.TB_029_MAAMAD]=86400 \
[STUD.TB_032_MATZAV_BACHUG]=86400 \
[STUD.TB_038_MATZAV_TAL_MUM_MOR]=86400 \
[STUD.TB_059_SCL_TOSEFET]=86400 \
[STUD.TB_060_TOAR]=86400 \
[STUD.TB_082_HEARA_TALMID]=86400 \
[STUD.TB_101_SIVUG_TOAR]=86400 \
[STUD.TB_975_CALNDR_ERUA]=86400 \
[dbo.Portal_Calendary_View]=3600 \
[STUD.TB_036_MATZAV_TZIUN]=86400 \
[STUD.TB_071_SIMUL_TZIUN]=86400 \
[STUD.TB_069_SCL_PEULA]=86400 \
[STUD.TB_104_SUG_MATALA]=86400 \
[STUD.TB_005_SHAOT_KURS]=86400 \
[STUD.TB_911_BINYAN]=86400 \
[STUD.TA_SEM]=86400 \
[STUD.TB_002_OFEN_HORAA]=86400 \
[STUD.TA_PERSON_SHAOT]=86400 \
[STUD.TB_962_TOAR_MORE]=86400 \
[STUD.KR_CHEDER]=86400 \
[STUD.TL_SEM]=86400 \
)

# set verbosity
[[ $1 == '-v' ]] && verbose=true || verbose=false

# get a manager
MANAGER=$(cat /gigashare/env_config/host.yaml | grep -A 1 manager | awk '/host/ {print $3}' | tail -1)
BASE_URL="http://${MANAGER}:8090/v2"

# get space name
SPACE_ID=$(curl -ks "${BASE_URL}/spaces" | jq -r '.[].name' | head -1)

# initialize shob data array
shob_info=()

# get feeders (SHOB_GA object) data
while read -r value; do
    readarray -t elements <<< "$(echo "$value" | jq -r '.[]')"
    source_name=${elements[0]}
    table_name=${elements[1]}
    time_stamp="$(date -d "${elements[2]}" +%s)"
    seconds_since_epoch=$(date -d "${elements[2]}" "+%s")
    time_stamp_hr=$(date -d "@$seconds_since_epoch" "+%Y-%m-%dT%H:%M:%SZ")
    th=$(get_table_threshold $table_name)
    time_diff=$(expr $(date +%s) - $time_stamp)
    [[ $time_diff -gt $th ]] && freshness=0 || freshness=1
    shob_info+=("shobStatus,source=$source_name,table_name=$table_name,threshold=$th,updated=${time_stamp_hr} freshness=$freshness")
    $verbose && echo "$source_name | $table_name | threshold=$th | timestamp = $time_stamp_hr | now = $(date +%s) | time_diff = $time_diff"
done< <(curl -ks "${BASE_URL}/spaces/${SPACE_ID}/query?typeName=SHOB_GA&maxResults=100" | jq --raw-output '.results[] | "\(.values)"')

# get CDC object data - build a list of shob related objects (= ZZ_META_DI_TIMESTAMP field)
shob_objects=""
objectTypesMetadata=$(curl -ks ${BASE_URL}/spaces/${SPACE_ID}/objectsTypeInfo | jq -r '.objectTypesMetadata[]')
while read -r obj; do
    [[ $obj == "SHOB_GA" ]] && continue
    has_zz_meta_di_timestamp=$(echo $objectTypesMetadata | \
    jq -r "select(.objectName == \"$obj\") | .schema" | \
    jq '. | any(.name == "ZZ_META_DI_TIMESTAMP")')
    $has_zz_meta_di_timestamp && shob_objects+=" $obj"
done < <(curl -ks "${BASE_URL}/spaces/${SPACE_ID}/statistics/types" | jq --raw-output 'keys[]' | grep -v "java.lang.Object" | sort -n)

# calculate CDC timestamps and generate influx data
source_name="CDC"
for table_name in $shob_objects; do
    zz_time=$(curl -ks "${BASE_URL}/spaces/dih-tau-space/query?typeName=${table_name}&columns=ZZ_META_DI_TIMESTAMP")
    time_stamp=$(echo $(echo "$zz_time" | jq -r '.results[].values[0] | tonumber' | jq -s 'max') / 1000 | bc)
    time_stamp_hr=$(date -d @${time_stamp} +"%Y-%m-%dT%H:%M:%SZ")
    th=$(get_table_threshold $table_name)
    time_diff=$(expr $(date +%s) - $time_stamp)
    [[ $time_diff -gt $th ]] && freshness=0 || freshness=1
    shob_info+=("shobStatus,source=$source_name,table_name=$table_name,threshold=$th,updated=${time_stamp_hr} freshness=$freshness")
    $verbose && echo "$source_name | $table_name | threshold=$th | timestamp = $time_stamp_hr | now = $(date +%s) | time_diff = $time_diff"
done

# output influx data
for i in "${shob_info[@]}"; do echo "$i" ; done

exit
