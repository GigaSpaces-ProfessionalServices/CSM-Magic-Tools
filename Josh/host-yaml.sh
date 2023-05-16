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
      -h          - Show usage
      -r          - Display all roles
      -A          - Display all hosts
      -l          - Display all roles and hosts
      <role_name> - Display hostnames of type <role_name>

    DEFAULT:
      Show usage

    A <role_name> can be one of the following:
      m | manager
      s | space
      d | c | dataIntegration
      g | grafana
      i | influxdb
      na | nb_applicative
      nm | nb_management
      dvs | data_validator_server
      dva | data_validator_agent
      p | pivot

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
  done < <( sed '/servers :/,${//d};/^ *$/d' ${ENV_CONFIG}/host.yaml )
}

show_no_dups() {
  local all_hosts=()
  while read h ; do
    local found=0
    for (( i=0 ; i < ${#all_hosts[@]} ; i++ )) ; do
      [[ "${h}" == "${all_hosts[${i}]}" ]] && { found=1 ; break ; }
    done
    [[ $found -ne 1 ]] && { echo $h ; all_hosts=( ${all_hosts[@]} $h ) ; }
  done < <( sed -En 's/ //g ; /x\.x\./d ; s/(.*host.*: *)(.*)$/\2/p' ${ENV_CONFIG}/host.yaml )
}

# Check for parameters
host_menu() {
  [[ $# -lt 1 ]] && show_usage
  if [[ ! -f ${ENV_CONFIG}/host.yaml ]] ; then
    local hostyaml="${ENV_CONFIG}/host.yaml"
    if [[ -f /dbagiga/host.yaml ]] ; then
      ENV_CONFIG=/dbagiga/env_config
    elif [[ -f /giga/host.yaml ]] ; then
      ENV_CONFIG=/giga/env_config
    else
      echo -e "\nFile host.yaml not found!\n" ; exit 1
    fi
    echo -e "\n${hostyaml}\n not found!\n" ; sleep 1
  fi
  while [[ $# -gt 0 ]] ; do
    case $1 in
      "-h" | "" ) show_usage ;;
      "-l" ) sed '/servers :/,${//d};/^ *$/d' ${ENV_CONFIG}/host.yaml ;;
      "-r" ) sed '/servers :/,${//d} ; /^ *$/d ; /host[0-9]*.*:/d ; /: *$/!d ; s/ *: *$// ' ${ENV_CONFIG}/host.yaml ;;
      # Out put all unique hosts/ips.
      "-A" ) show_no_dups ;;
      * ) display_hosts_per_role $1 ;;
    esac
    shift
  done
}

########## MAIN

host_menu "$@"
