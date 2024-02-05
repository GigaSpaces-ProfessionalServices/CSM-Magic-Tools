#!/bin/bash

function edit_toml() {
    # change tables / vaules in TOML (configuration) files
    opt=$1      # set | enable | disable | enable_tree | disable_tree
    table=$2
    key=$3
    value="$(echo $4 | sed 's/\[/\\[/g' | sed 's/\]/\\]/g')"
    if [[ $opt == "set" ]]; then
        file=$5     # the TOML file
        sed -i "/^\[$table\]/,/^\[.*\]/ s/^\(\s*$key = \).*/\1$value/" "$file"
    else
        # enable or disable a toml table
        [[ $# -ne 4 ]] && (echo "error: expecting 4 parameters for enable | disable operations!" ; return)
        file=$4     # the TOML file
        case $opt in
            "enable")
                sed -i "/^\[$table\]/,/^\[.*\]/ s/^\(\s*\)#\?\(\s*$key = \)/\1\2/" "$file"
                ;;
            "disable")
                sed -i "/^\[$table\]/,/^\[.*\]/ s/^\(\s*\)$key =/\1#$key =/" "$file"
                ;;
        esac
    fi
}


KAPACITOR_CONF="/etc/kapacitor/kapacitor.conf"

# save default kapacitor.conf
[[ ! -f "${KAPACITOR_CONF}.default" ]] && cp "${KAPACITOR_CONF}" "${KAPACITOR_CONF}.default"

# set kapacitor url
sed -i '/KAPACITOR_URL/d' /root/.bashrc
echo -e "export KAPACITOR_URL=http://$(hostname -I | awk '{print $1}'):9992\n" >> /root/.bashrc

source /root/.bashrc

# enable http parameters
edit_toml enable http bind-address $KAPACITOR_CONF
edit_toml enable http log-enabled $KAPACITOR_CONF

# set http parameters
edit_toml set http bind-address '":9992"' $KAPACITOR_CONF
edit_toml set http log-enabled false $KAPACITOR_CONF

# enable smtp parameters
edit_toml enable smtp enabled $KAPACITOR_CONF
edit_toml enable smtp global $KAPACITOR_CONF
edit_toml enable smtp from $KAPACITOR_CONF
edit_toml enable smtp to $KAPACITOR_CONF
edit_toml enable smtp state-changes-only $KAPACITOR_CONF

# set smtp parameters
edit_toml set smtp enabled true $KAPACITOR_CONF
edit_toml set smtp global true $KAPACITOR_CONF
edit_toml set smtp from '"kapacitor-alerts@tau.ac.il"' $KAPACITOR_CONF
edit_toml set smtp to '["tau-alerts@gigaspaces.com"]' $KAPACITOR_CONF
edit_toml set smtp state-changes-only true $KAPACITOR_CONF


# # setup shob alerts file
# alerts_log="/gigalogs/shob_alerts.log"
# touch $alerts_log
# chown kapacitor:kapacitor $alerts_log

# set endpoint parameters according to environment
# cat >> $KAPACITOR_CONF << EOL
# [[httppost]]
#     endpoint = "debug"
#     url = "http://localhost:4242"
#     alert-template-file = "/etc/kapacitor/templates/debug.json"

# [[httppost]]
#     endpoint = "common-alert"
#     url = "https://servicenow-preprod-itom/api/global/em/jsonv2"
#     headers = { Accept = "application/json" , Content-Type = "application/json" }
#     basic-auth = { username = "", password = "" }
#     alert-template-file = "/etc/kapacitor/templates/common-alert.json"

# [[httppost]]
#     endpoint = "nginx-load-raw"
#     url = "https://servicenow-preprod-itom/api/global/em/jsonv2"
#     headers = { Accept = "application/json" , Content-Type = "application/json" }
#     basic-auth = { username = "", password = "" }
#     row-template-file = "/etc/kapacitor/templates/nginx-load-raw.json"

# EOL

exit
