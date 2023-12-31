#!/bin/bash

function edit_toml() {
    # change tables / vaules in TOML (configuration) files
    element=$1      # the element to target
    table_name=$2   # the table name to address
    if [[ $element == "table" ]]; then
        # enable or disable a toml table
        [[ $# -ne 4 ]] && (echo "error: missing parameters!" ; return)
        action=$3          # operation: enable | disable
        file=$4         # the TOML file
        case $action in
            "enable") sed -i "/\[${table_name}\]/s/#/ /g" $file ;;
            "disable") sed -i "/\[${table_name}\]/s/ /#/" $file ;;
        esac
    elif [[ $element == "value" ]]; then
        # change the value of a key
        [[ $# -ne 5 ]] && (echo "error: missing parameters!" ; return)
        key=$3          # the key to change
        value=$4        # the new value to set
        file=$5         # the TOML file
        sed -i "/^\[$table\]/,/^[[]/ s/\($key =\).*/\1 $value/" $file
    else
        echo "Error: bad parameter!"
    fi
}

#TELEGRAF_CONF="/etc/telegraf/telegraf.conf"
TELEGRAF_CONF="/tmp/test"

#edit_toml $1 $2 $3 "$TELEGRAF_CONF"

exit

# save default kapacitor.conf
[[ ! -f "${TELEGRAF_CONF}.default" ]] && cp "${TELEGRAF_CONF}" "${TELEGRAF_CONF}.default"

# set http parameters
edit_toml "http" "bind-address" ":9992" "$TELEGRAF_CONF"
edit_toml "http" "log-enabled" "false" "$TELEGRAF_CONF"

# set stateChangesOnly()
edit_toml "smtp" "state-changes-only" "true" "$KAPACITOR_CONF"

# setup shob alerts file
alerts_log="/gigalogs/shob_alerts.log"
touch $alerts_log
chown kapacitor:kapacitor $alerts_log

set endpoint parameters according to environment
cat >> $KAPACITOR_CONF << EOL
[[httppost]]
    endpoint = "debug"
    url = "http://localhost:4242"
    alert-template-file = "/etc/kapacitor/templates/debug.json"

[[httppost]]
    endpoint = "common-alert"
    url = "https://servicenow-preprod-itom/api/global/em/jsonv2"
    headers = { Accept = "application/json" , Content-Type = "application/json" }
    basic-auth = { username = "", password = "" }
    alert-template-file = "/etc/kapacitor/templates/common-alert.json"

[[httppost]]
    endpoint = "nginx-load-raw"
    url = "https://servicenow-preprod-itom/api/global/em/jsonv2"
    headers = { Accept = "application/json" , Content-Type = "application/json" }
    basic-auth = { username = "", password = "" }
    row-template-file = "/etc/kapacitor/templates/nginx-load-raw.json"

EOL

exit
