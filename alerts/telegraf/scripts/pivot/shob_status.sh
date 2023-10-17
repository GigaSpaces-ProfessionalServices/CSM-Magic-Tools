#!/bin/bash

# get a manager
MANAGER=$(cat /gigashare/env_config/host.yaml | grep -A 1 manager | awk '/host/ {print $3}')
SWAGGER="http://${MANAGER}:8090/v2"

# get space name
SPACE_ID=$(curl -ks "${SWAGGER}/spaces" | jq -r '.[].name' | head -1)

# get shob tables
SHOB_TABLES=""
for t in $(curl -ks ${SWAGGER}/spaces/${SPACE_ID}/objectsTypeInfo | jq | grep -i "shob" | sed 's/[",]//g' | cut -d: -f2); do
    [[ $SHOB_TABLES == "" ]] && SHOB_TABLES+="$t" || SHOB_TABLES+=" $t"
done

# echo shob status 
for shob_t in $SHOB_TABLES; do
    [[ ${shob_t,,} == "shob" ]] && continue
    url="${SWAGGER}/spaces/${SPACE_ID}/query?typeName=${shob_t}&columns=ZZ_META_DI_TIMESTAMP"
    shob_status=$(curl -ks $url | jq -r '.results[0].values[0]')
    [[ $shob_status == 'null' ]] && continue
    echo "shobStatus,table_name=$shob_t last_updated=$shob_status"
done
