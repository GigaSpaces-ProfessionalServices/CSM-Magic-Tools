#!/bin/bash
# /usr/local/bin/gc-state.sh

function get_cluster_hosts {
    local cluster_name=$1
    local prefix=$2
    local s='[[:space:]]*' w='[a-zA-Z0-9_]*' fs=$(echo @|tr @ '\034')
    sed -ne "s|^\($s\):|\1|" \
        -e "s|^\($s\)\($w\)$s:$s[\"']\(.*\)[\"']$s\$|\1$fs\2$fs\3|p" \
        -e "s|^\($s\)\($w\)$s:$s\(.*\)$s\$|\1$fs\2$fs\3|p" \
        ${ODSXARTIFACTS}/odsx/host.yaml |
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

function set_credentials {
	local flag=$(echo $ODSXARTIFACTS | cut -d / -f 3)
	[[ ${flag} == 'prd' || ${flag} == 'dr' ]] && env_suffix='PRD' || env_suffix='STG'
	# get available manager
	for h in $(get_cluster_hosts "manager"); do
		if [[ $(nc -z $h 22) -eq 0 ]]; then
			manager="$h"
			break
		fi
	done
	sdk_path="/opt/CARKaim/sdk/clipasswordsdk"
	appdescs_appid="AppDescs.AppID=APPODSUSERSBLL${env_suffix}"
	query="Query='Safe=AIMODSUSERSBLL${env_suffix};Folder=;Object=ACCHQudkodsl;'"
	case $1 in
		'usr') option="PassProps.UserName" ;;
		'pwd') option="Password" ;;
	esac
	ssh ${manager} "$sdk_path GetPassword -p $appdescs_appid -p $query -o $option"
}

BASEURL=https://<NB_MNG_FARM>:<V2_PORT>
SPACE="bllservice"
vusr=$(set_credentials usr)
vpwd=$(set_credentials pwd)

splitter() {
	sed -e 's/,/\n/g' | grep $SPACE | \
	sed -e s'/.*'$SPACE'~//g' -e 's/_/ /g' -e 's/"//g'
}

DEFINED=$(curl -ks -u "${vusr}:${vpwd}" $BASEURL/v2/pus/$SPACE | jq -r '.instances' | splitter)
REAL=$(curl -ks -u "${vusr}:${vpwd}" $BASEURL/v2/containers | jq -r '.[].instances?' | splitter)

echo -e "${DEFINED}\n$REAL" | sort -V | \
awk '
BEGIN	{
	old = 0 ;
	count = 0 ;
}
{
#print "DEBUG before: old="old", count="count", string="$0 ;
	if ( $1 != old ) {
		if (count == 4) print "gcState,pu="old" state=\"healthy\"";
		if (count == 3) print "gcState,pu="old" state=\"partial\"";
		if (count == 2) print "gcState,pu="old" state=\"faulty\"";
		count = 1;
		old = $1 ;
	} else {
		count += 1 ;
	}
#print "DEBUG: old="old", count="count", string="$0 ;
}
END	{
		if (count == 4) print "gcState,pu="old" state=\"healthy\"";
		if (count == 3) print "gcState,pu="old" state=\"partial\"";
		if (count == 2) print "gcState,pu="old" state=\"faulty\"";
	}
'
