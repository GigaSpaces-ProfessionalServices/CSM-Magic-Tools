#!/bin/bash

[[ ! -x /dbagiga/utils/host-yaml.sh ]] && { echo -e "Can not find host-yaml.sh - exiting" ; exit 1 ; }

################### PRERUN

source ~/.bash_profile
_ALL_MANAGERS=( $( host-yaml.sh -m ) )
_GSM=0
_WEBUI=0
_KILL_WAIT=120

################### FUNCTIONS

show_usage() {
cat << EOF

  Kill GSM and WEBUI on managers 

    USAGE: 
      $(basename $0 ) [<options>]

    OPTIONS:
      --gsm | --rest            Kill GSM/REST process on all managers with $_KILL_WAIT second wait in between.
      --webui                   Kill WEBUI on all managers
      -a                        Execute both --gsm and --webui
      -h | --help               Show this usage

    DEFAULT VALUES:
      -h

    EXAMPLE: 
      $(basename $0 ) --gsm     Only kill GSM on all managers 

EOF
}

do_main() {
  for m in ${_ALL_MANAGERS[@]} ; do
    if [[ $_GSM -ne 0 ]] ; then 
      local gsm_pid=$(ssh $m 'ps -ef | grep "SystemBoot services=MANAGER\[LH,ZK,GSM,REST\]" | grep -v grep | awk '\''{print $2}'\'' ' )
      echo "Killing LH,ZK,GSM,REST pid $gsm_pid on manager ${m}." 
      local gsm_kill_string=$( echo -e "ssh $m 'kill ${gsm_pid}'")
      eval $gsm_kill_string
      #echo -e $gsm_kill_string
      [[ "${m}" != "${_ALL_MANAGERS[-1]}" ]] && { echo "Waiting for $_KILL_WAIT seconds" ; sleep $_KILL_WAIT ; }
    fi
    if [[ $_WEBUI -ne 0 ]] ; then
      local webui_pid=$( ssh $m 'ps -ef | grep "SystemBoot services=WEBUI" | grep -v grep | awk '\''{print $2}'\'' ' )
      echo "Killing WEBUI pid $webui_pid on manager ${m}."
      local webui_kill_string=$( echo -e "ssh $m 'kill ${webui_pid}'" )
      eval $webui_kill_string
    fi
  done
}

do_options() {
  [[ $# -eq 0 || "${1}" == "-h" || "${1}" == "--help" ]] && { show_usage ; exit 0 ; }
  while [[ $# -gt 0 ]] ; do 
    case $1 in
      "--gsm"|"--rest")
        _GSM=1 ;;
      "--webui")
        _WEBUI=1 ;;
      "-a")
        _GSM=1 ; _WEBUI=1 ;;
      *) 
        echo -e "\nOption $1 unknown.\n" ; show_usage ; exit 1 ;;
    esac
    shift
  done
}

################### MAIN

do_options "${@}"
do_main
