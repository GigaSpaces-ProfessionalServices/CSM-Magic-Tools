#!/bin/bash

SPACE_SERVER_SRNO=""
_ZONES=""
declare -A ZONE_KEY_VALUES
SPACE_HOST_NAME_ARRAY=()
SPACE_HOST_IP_ARRAY=()
SPACE_SERVERS_IP=( $( runall -s -l | grep -v === ) )
SPACE_SERVERS_COUNT=$( runall -s -l | grep -v === | wc -l )
SPACE_SERVER_HOST=""
SPACE_SERVER_IP=""
SPACE_NUM_ARG=""
_FILE=""

function get_current_zones() {
/usr/bin/expect -c '
set timeout -1
set force_conservative 1

cd /dbagiga/gs-odsx
spawn ./odsx.py space containers create
expect "if you want to Create container."
sleep .2
send -- "\033"

expect eof
' | grep -v '^[[:space:]]$\|---\| for exit\|[Cc]reate\|GSC' | sed -E 's/\x1B\[[0-9;]*[a-zA-Z]//g ; s/[^[:print:]]//g ; s/[ \t]//g ; /^$/d' | sed "s/'/\"/g"
}

function create_zone_container() {
var="${1}" serialno="${2}" /usr/bin/expect -c '
set zonename "$env(var)"
set serno "$env(serialno)"

set timeout -1
set force_conservative 1

cd /dbagiga/gs-odsx
spawn ./odsx.py space containers create
expect "if you want to Create container."
sleep .2
send -- "\r"
expect "Enter host Srno"
sleep .2
send -- "$serno"
sleep .2
send -- "\r"
expect "Enter number of GSCs"
sleep .2
send -- "\r"
expect "Enter zone of GSC to create"
sleep .2
for {set i 0} {$i < [string length $zonename]} {incr i} {
    # Get the current character
    set char [string index $zonename $i]
    
    # Send the current character
    send -- "$char"
    sleep .2
}
send -- "\r"
expect "Enter memory of GSC"
sleep .2
send -- "1"
sleep .2
send -- "g"
sleep .2
send -- "\r"
expect "Do you want to create container"
sleep .2
send -- "\r"
expect "Press \\\[99\\\] for exit.:"
sleep .2
send -- "\033"

expect eof
'
}


function create_space_arrays() {
  local spc spc_host_name spc_number=1
  for spc in ${SPACE_SERVERS_IP[@]} ; do
    SPACE_HOST_IP_ARRAY[$((spc_number))]=$spc
    spc_host_name=$(host $spc | awk '{print $NF}' | sed 's/\.$//')             # if spc is 10.14.8.21 then spc_host_name=gstest-space1
    #spc_number=$(echo -n $spc_host_name | sed -En 's/[^0-9]+([0-9]+)$/\1/p')  # spc_number of gstest-space1 = 1
    SPACE_HOST_NAME_ARRAY[$((spc_number++))]=$spc_host_name
  done
}

function get_space_server_srno() {
  # e.g. SPACE_NUM_ARG=1 --> gstest-space1 --> 10.14.8.13
  local host_ip
  [[ -z $SPACE_NUM_ARG || $SPACE_NUM_ARG -lt 1 || $SPACE_NUM_ARG -gt $SPACE_SERVERS_COUNT ]] && { echo "space number invalid" ; exit 1 ; }
  SPACE_SERVER_SRNO=$( echo -e "${_ZONES}" | awk -F\| -v host_ip="${SPACE_HOST_IP_ARRAY[${SPACE_NUM_ARG}]}" '$3==host_ip {print $2}' )
}

function get_zone_key_value() {
local key value
while IFS="=" read -r key value; do
  #echo key=$key value=$value
  ZONE_KEY_VALUES[$key]=$value
done < <(echo "${_ZONES}" | awk -F\| -v host_ip="${SPACE_HOST_IP_ARRAY[${SPACE_NUM_ARG}]}" '$3==host_ip {print $5}' | jq -r 'to_entries | map(select(.key != "tau")) | .[] | "\(.key)=\(.value)"')
}

function print_zone_key_value() {
# ${!ZONE_KEY_VALUES[@]} - keys
# ${ZONE_KEY_VALUES[@]} - values
#declare -p ZONE_KEY_VALUES
  local key idx
for key in "${!ZONE_KEY_VALUES[@]}"; do
  #ZONE_KEY_VALUES[$key]=2
  for (( idx=1 ; idx<=${ZONE_KEY_VALUES[$key]} ; idx++ )) ; do
    #echo print $key for ${ZONE_KEY_VALUES[$key]} time
    echo "$key=${ZONE_KEY_VALUES[$key]}"
  done
done
}

function create_containers_for_zones() {
  local key idx
for key in "${!ZONE_KEY_VALUES[@]}"; do
  for (( idx=1 ; idx<=${ZONE_KEY_VALUES[$key]} ; idx++ )) ; do
    create_zone_container $key $SPACE_SERVER_SRNO
    #read -p "Press ENTER to continue: " < /dev/tty
  done
done
}

function process_zones() {
if [[ -z $_FILE ]] ; then
  _ZONES="$(get_current_zones)"
  echo -e "${_ZONES}" > /tmp/space-zones-$$
  echo -e "Zones saved to file /tmp/space-zones-${$}"
else
  _ZONES="$(cat $_FILE)"
fi
}

function process_arguments() {
  [[ $# -eq 0 ]] && { echo "No arguments" ; exit 1 ; }
  while [[ $# -gt 0 ]] ; do
    case "${1}" in
      "-s")
        SPACE_NUM_ARG=$2
        SPACE_SERVER_HOST=${SPACE_HOST_NAME_ARRAY[${SPACE_NUM_ARG}]}
        SPACE_SERVER_IP=${SPACE_HOST_IP_ARRAY[${SPACE_NUM_ARG}]}
        shift
        ;;
      "-f")
        _FILE=$2
        [[ ! -s $_FILE ]] && { echo "File is empty or does not exist." ; exit 1 ; }
        shift
        ;;
      *)
        echo -e "\n Option $1 not supported.\n" ; exit 1
        ;;
    esac
    shift
  done

}

########## MAIN ##########

#awk -v num="${SPACE_NUM_ARG}" -F\| ' $2 == num {print }' /tmp/2426190zones.out
#for (( i=1 ; i<=$SPACE_SERVERS_COUNT ; i++ )) ; do echo ${SPACE_HOST_NAME_ARRAY[${i}]} ; done

process_arguments "${@}"
create_space_arrays
process_zones
get_space_server_srno
get_zone_key_value          # Populate ZONE_KEY_VALUES excluding tau key/value
print_zone_key_value
read -p "Press ENTER after reboot"
create_containers_for_zones

exit
echo SPACE_SERVER_SRNO=$SPACE_SERVER_SRNO
echo SPACE_SERVER_HOST=$SPACE_SERVER_HOST
echo SPACE_SERVER_IP=$SPACE_SERVER_IP
