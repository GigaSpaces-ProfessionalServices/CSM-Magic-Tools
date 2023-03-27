#!/bin/bash

role_found=0
_ROLE=""

shortcuts() {
  case $1 in 
    "m"|"manager" ) _ROLE=manager ;;
    "s"|"space" ) _ROLE=space ;;
    "d"|"c"|"dataIntegration" ) _ROLE=dataIntegration ;;
    "g"|"grafana" ) _ROLE=grafana ;;
    "i"}"influxdb" ) _ROLE=influxdb ;;
    "na"|"nb_applicative" ) _ROLE=nb_applicative ;;
    "nm"|"nb_management" ) _ROLE=nb_management ;;
    "dvs"|"data_validator_server" ) _ROLE=data_validator_server ;;
    "dva"|"data_validator_agent" ) _ROLE=data_validator_agent ;;
    "p"|"pivot" ) _ROLE=pivot ;;
    * ) _ROLE=""
  esac
}

show_usage() {
cat << EOF

  Display DIH hostnames
    Usage: $0

    PARAMETERS:
      -l | -r     - Display all role names
      -A          - Display all hostnames
      <role_name> - Display hostnames of type <role_name>

EOF
}

display_hosts_per_role() {
  shortcuts $1
  [[ -z $_ROLE ]] && { echo -e "\n Role does not exist\n" ; return 1 ; }
  while read line ; do
    [[ $(echo "${line}" | grep "^ *${_ROLE} *:" >/dev/null 2>&1 ; echo $? ) -eq 0 ]] && { role_found=1 ; continue ; }
    if [[ $role_found -ne 0 && $(echo "${line}" | grep -w '^ *host[0-9]* *:' >/dev/null 2>&1 ; echo $? ) -eq 0 ]] ; then
      echo $line | sed 's/^ *host[0-9]* *: *//'
    else
      role_found=0
    fi
  done < <(cat ${ENV_CONFIG}/host.yaml | sed '/^ *$/d')
}

# Check for parameters
host_menu() {
  [[ $# -lt 1 ]] && show_usage
  [[ ! -f /dbagigashare/env_config/host.yaml && ! -f /dbagigashare/dr/env_config/host.yaml && ! -f /dbagigashare/prd/env_config/host.yaml ]] && ENV_CONFIG=/dbagiga/env_config
  while [[ $# -gt 0 ]] ; do
    case $1 in
      "-h" | "" ) show_usage ;;
      "-r" | "-l" ) tail -n +2 ${ENV_CONFIG}/host.yaml | grep -v 'host[0-9]*.*:' | sed '/^ *$/d' | sed -n '/: *$/p' | sed 's/ *: *$//' ;;
      "-A" ) sed -En 's/(.*host.*: *)(.*)$/\2/p' ${ENV_CONFIG}/host.yaml | tr -d ' ' | sort -u ;;
      * ) display_hosts_per_role $1 ;;
    esac
    shift
  done
}

########## MAIN

host_menu "$@"
