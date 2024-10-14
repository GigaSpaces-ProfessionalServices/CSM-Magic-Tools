#!/bin/bash


function get_cluster_hosts {
    local cluster_name=$1
    local prefix=$2
    local s='[[:space:]]*' w='[a-zA-Z0-9_]*' fs=$(echo @|tr @ '\034')
    sed -ne "s|^\($s\):|\1|" \
        -e "s|^\($s\)\($w\)$s:$s[\"']\(.*\)[\"']$s\$|\1$fs\2$fs\3|p" \
        -e "s|^\($s\)\($w\)$s:$s\(.*\)$s\$|\1$fs\2$fs\3|p" \
        ${ENV_CONFIG}/host.yaml |
    awk -F$fs '{
        indent = length($1)/2;
        vname[indent] = $2;
        for (i in vname) {if (i > indent) {delete vname[i]}}
        if (length($3) > 0) {
            vn=""; for (i=0; i<indent; i++) {vn=(vn)(vname[i])("_")}
            printf("%s%s%s=\"%s\"\n", "'$prefix'",vn, $2, $3);
        }
    }' | while read line; do
        [[ "$line" =~ .*"${cluster_name}_host".* ]] && \
        echo $line | sed 's/ *//g' | sed 's/"//g' | cut -d= -f2
    done
}


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


function get_auth() {
    sec_flag=$(cat /gigashare/env_config/app.config | grep "app.setup.profile" | cut -d= -f2)
    if [[ $sec_flag != "" ]]; then

      AUTH_USER=$(awk -F= '/app.manager.security.username=/ {print $2}' ${ENV_CONFIG}/app.config)
      if grep '^app.vault.use=true' ${ENV_CONFIG}/app.config > /dev/null ; then
        _VAULT_PASS=$(awk -F= '/app.manager.security.password.vault=/ {print $2}' ${ENV_CONFIG}/app.config)
        AUTH_PASS=$(java -Dapp.db.path=/dbagigawork/sqlite/ -jar /dbagigashare/current/gs/jars/gs-vault-1.0-SNAPSHOT-jar-with-dependencies.jar --get ${_VAULT_PASS})
      else
        AUTH_PASS=$(awk -F= '/app.manager.security.password=/ {print $2}' ${ENV_CONFIG}/app.config)
      fi
    fi
}


function is_manager_rest_ok() {
    local the_manager=$1
    local port_ok=false
    local rest_ok=false

    # check port
    nc -z $the_manager 8090 && port_ok=true
    
    # check rest
    local rest="http://${the_manager}:8090/v2/index.html"
    local status_code=$(curl -u "$AUTH_USER:$AUTH_PASS" \
    --write-out '%{http_code}' --silent --output /dev/null "$rest")
    [[ $status_code -eq 200 ]] && rest_ok=true
    
    ($port_ok && $rest_ok) && return 0 || return 1
}

#
# # # MAIN # # #
#

ENV_CONFIG="/gigashare/env_config"
AUTH_USER=""
AUTH_PASS=""

# tables thresholds dictionary
# * using bash v4.x associative array
declare -A SHOB_TABLES
SHOB_TABLES=([STUD.KR_KURS]=2592000 \
[STUD.KR_KVUTZA]=2592000 \
[STUD.KR_KVUTZA_MOED]=2592000 \
[STUD.KR_KVUTZA_SEGEL]=2592000 \
[STUD.KR_KVUTZA_ZAMAK]=2592000 \
[STUD.MJ_HODAOT_WEB]=2592000 \
[STUD.MM_TOCHNIT_LIMUD]=2592000 \
[STUD.MM_YEHIDA]=2592000 \
[STUD.SL_HESHBON]=2592000 \
[STUD.SL_TNUA]=2592000 \
[STUD.TA_HODAA]=2592000 \
[STUD.TA_IDS]=2592000 \
[STUD.TA_KTOVET]=2592000 \
[STUD.TA_PERSON]=2592000 \
[STUD.TA_PRATIM]=2592000 \
[STUD.TL_HEARA]=2592000 \
[STUD.TL_KURS]=2592000 \
[STUD.TL_TOAR]=2592000 \
[STUD.TL_TOCHNIT]=2592000 \
[STUD.TA_CALENDAR]=2592000 \
[STUD.TB_020_MADAD]=2592000 \
[STUD.TB_029_MAAMAD]=2592000 \
[STUD.TB_032_MATZAV_BACHUG]=2592000 \
[STUD.TB_038_MATZAV_TAL_MUM_MOR]=2592000 \
[STUD.TB_059_SCL_TOSEFET]=2592000 \
[STUD.TB_060_TOAR]=2592000 \
[STUD.TB_082_HEARA_TALMID]=2592000 \
[STUD.TB_101_SIVUG_TOAR]=2592000 \
[STUD.TB_975_CALNDR_ERUA]=2592000 \
[dbo.Portal_Calendary_View]=2592000 \
[STUD.TB_036_MATZAV_TZIUN]=2592000 \
[STUD.TB_071_SIMUL_TZIUN]=2592000 \
[STUD.TB_069_SCL_PEULA]=2592000 \
[STUD.TB_104_SUG_MATALA]=2592000 \
[STUD.TB_005_SHAOT_KURS]=2592000 \
[STUD.TB_911_BINYAN]=2592000 \
[STUD.TA_SEM]=2592000 \
[STUD.TB_002_OFEN_HORAA]=2592000 \
[STUD.TA_PERSON_SHAOT]=2592000 \
[STUD.TB_962_TOAR_MORE]=2592000 \
[STUD.KR_CHEDER]=2592000 \
[STUD.TL_SEM]=2592000 \
[STUD.TB_917_SHANA]=2592000 \
[STUD.TL_MOED_TZIUN]=2592000 \
[STUD.TL_KVUTZA]=2592000 \
[STUD.TM_MECHKAR]=2592000 \
[STUD.TM_SEGEL]=2592000 \
[STUD.KR_KVUTZA_MOED_HEARA]=2592000 \
[STUD.TL_CHOVOT_UNI]=2592000 \
[STUD.TL_MOED_NOSAF_BAKASHA]=2592000 \
[STUD.TB_023_MOED]=2592000 \
[STUD.TL_IRUR_BAKASHA]=2592000 \
[STUD.TB_022_MOSAD]=2592000 \
[STUD.TB_982_HODAA_URL]=2592000 \
)

# special vaule for GilboaSync dbo.Portal_Calendary_View table
GILBOASYNC=2592000

# get credentials if env is secured
get_auth

# get manager host
for m in $(get_cluster_hosts "manager"); do
    if is_manager_rest_ok $m ; then
        MANAGER=$m
        break
    fi
done
if [[ -z $MANAGER ]]; then
    echo "[ERROR] no avaialable managers found!"
    exit
fi

BASE_URL="http://${MANAGER}:8090/v2"
SHOB_COOKIE=/tmp/.shob_cookie_${RANDOM}


if [[ $AUTH_USER != "" ]] ; then
    # cache login
    [[ -e $SHOB_COOKIE ]] && rm -f $SHOB_COOKIE
    curl --user $AUTH_USER:$AUTH_PASS --cookie-jar $SHOB_COOKIE $BASE_URL
    if [[ ! -e $SHOB_COOKIE ]]; then
        echo "error: could not build auth cache file"
        exit 1
    fi
fi

# check if connecion to space available
if [[ $(curl --cookie $SHOB_COOKIE -ks "${BASE_URL}/spaces") == "Connect failed" ]]; then
    echo "Connection failed."
    exit
fi

# check if connecion returns data
if [[ $(curl --cookie $SHOB_COOKIE -ks "${BASE_URL}/spaces") == "" ]]; then
    echo "No freshness data is currently available."
    exit
fi

# get space name
SPACE_ID=$(curl --cookie $SHOB_COOKIE -ks "${BASE_URL}/spaces" | jq -r '.[].name' | head -1)

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
    if [[ $source_name == "GilboaSync" ]]; then
        th=$GILBOASYNC
    else
        th=$(get_table_threshold $table_name)
    fi
    #echo table_name=$table_name time_stamp=$time_stamp
    time_diff=$(expr $(date +%s) - $time_stamp)
    [[ $time_diff -gt $th ]] && freshness=0 || freshness=1
    shob_info+=("shobStatus,source=$source_name,table_name=$table_name,threshold=$th,updated=${time_stamp_hr} freshness=$freshness")
done< <(curl --cookie $SHOB_COOKIE -ks "${BASE_URL}/spaces/${SPACE_ID}/query?typeName=SHOB_GA&maxResults=100" | jq --raw-output '.results[] | "\(.values)"')



# get CDC object data - build a list of shob related objects (= ZZ_META_DI_TIMESTAMP field)
shob_objects=""
objectTypesMetadata=$(curl --cookie $SHOB_COOKIE -ks ${BASE_URL}/spaces/${SPACE_ID}/objectsTypeInfo | jq -r '.objectTypesMetadata[]')
while read -r obj; do
    [[ $obj == "SHOB_GA" ]] && continue
    has_zz_meta_di_timestamp=$(echo $objectTypesMetadata | \
    jq -r "select(.objectName == \"$obj\") | .schema" | \
    jq '. | any(.name == "ZZ_META_DI_TIMESTAMP")')
    $has_zz_meta_di_timestamp && shob_objects+=" $obj"
done < <(curl --cookie $SHOB_COOKIE -ks "${BASE_URL}/spaces/${SPACE_ID}/statistics/types" | jq --raw-output 'keys[]' | grep -v "java.lang.Object" | sort -n)

# calculate CDC timestamps and generate influx data
source_name="CDC"
field="ZZ_META_DI_TIMESTAMP"
lookup_group=$(cat $ENV_CONFIG/app.config | grep -oE '\lus\.groups=[^[:space:]]+' | awk -F'=' '{print $2}' | tail -1)
dv_jar="/gigashare/current/data-validator/jars/common-0.0.1-SNAPSHOT-jar-with-dependencies.jar"

for table_name in $shob_objects; do
    zz_time=$(java -jar $dv_jar $SPACE_ID $MANAGER $lookup_group Long $table_name $field 2>/dev/null | tail -1 | awk '{print $NF}')
    time_stamp=$(echo "$zz_time" / 1000 | bc)
    time_stamp_hr=$(date -d @${time_stamp} +"%Y-%m-%dT%H:%M:%SZ")
    th=$(get_table_threshold $table_name)
    #echo table_name=$table_name time_stamp=$time_stamp
    time_diff=$(expr $(date +%s) - $time_stamp)
    [[ $time_diff -gt $th ]] && freshness=0 || freshness=1
    shob_info+=("shobStatus,source=$source_name,table_name=$table_name,threshold=$th,updated=${time_stamp_hr} freshness=$freshness")
done

# output influx data
for i in "${shob_info[@]}"; do echo "$i" ; done

# delete session cookie
rm -f $SHOB_COOKIE

exit

