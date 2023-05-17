#!/bin/bash

[[ ! -x  /dbagiga/utils/host-yaml.sh ]] && { echo -e "\nFile host-yaml.sh is required by script\n" ; exit 1 ; }

_NEW=/tmp/new_hosts$$
_HOSTS_NUM=$(tail -n +3 /etc/hosts | wc -l)
_PUBLIC_NUM=$(tail -n +3 /etc/hosts | sed -n '/#[[:space:]]*[0-9]\{1,3\}\.[0-9]\{1,3\}\.[0-9]\{1,3\}\.[0-9]\{1,3\}[[:space:]]*$/p' | wc -l )

add_public() {
  sed -i 's/[[:space:]]*#.*$//' /etc/hosts
  local private_ips=( $(tail -n +3 /etc/hosts | awk '{print $1}') )
  for (( i=0 ; i < $_HOSTS_NUM ; i++ )) ; do
    if [[ $(ping -c1 -w2 ${private_ips[$i]} >/dev/null 2>&1 ; echo $?) -eq 0 ]] ; then
      public_ips=( ${public_ips[@]} $(ssh ${private_ips[$i]} curl http://169.254.169.254/latest/meta-data/public-ipv4 2>/dev/null) )
      sed -i "$(( $i + 3 )) s/$/ \t# ${public_ips[$i]}/" /etc/hosts
    fi
  done
}

verify_public() {
  [[ $_PUBLIC_NUM -ne $_HOSTS_NUM ]] && add_public
}

force_add_public() {
  add_public
}

remote_hosts() {
  shift
  if [[ "$1" = "-m" || "$1" = "-s" || "$1" = "-n" || "$1" = "-nm" || "$1" = "-na" || "$1" = "-d" || "$1" = "-a" ]] ; then
    local hosts=$( (runhosts $1) )
    shift
    for h in ${hosts[@]} ; do
      my_cmd="ssh $h ${@}"
      eval $my_cmd
    done
  else
    echo -e "\n 2nd param must be one of the following: -m, -s, -n, -na, -nm, -d, -a\n" ; exit 1
  fi
}

usage() {
  cat << EOF

  USAGE: $(basename $0) [<option>]

  OPTIONS:
    -h      Display this help
    -p      Show pivot hosts
    -m      Show Manager hosts
    -s      Show Space hosts
    -g      Show Grafana hosts
    -i      Show Influx hosts
    -n      Show NB management, NB applicative, NB agent (space) hosts
    -nm     Show NB Management hosts
    -na     Show NB Applicative hosts
    -d      Show DI hosts
    -a      Show manager and space hosts
    -A      Show all hosts
    -l      Show all roles and their hosts

  DEFAULT:
    -h      Show usage

  EXAMPLE: Run 'df -h' on all manager servers
   $(basename $0) -m 'df -h'

EOF
exit 0
}

do_main() {
  case $1 in
    "-h"|"") usage ;;
    "-p") H=$(host-yaml.sh -p) ;;
    "-m") H=$( host-yaml.sh -m ) ;;
    "-s") H=$( host-yaml.sh -s ) ;;
    "-g") H=$( host-yaml.sh -g ) ;;
    "-i") H=$( host-yaml.sh -i ) ;;
    "-n") H=$( host-yaml.sh -nm ; host-yaml.sh -na ; host-yaml.sh -s ) ;;
    "-nm") H=$( host-yaml.sh -nm ) ;;
    "-na") H=$( host-yaml.sh -na ) ;;
    "-d") H=$(host-yaml.sh -d) ;;
    "-a") H=$(host-yaml.sh -m -s) ;;
    "-A") H=$(host-yaml.sh -A) ;;
    "-l") host-yaml.sh -l ; exit ;;
    *) echo -e "\n Unknown parameter passed: ${1}\n" ; exit 1 ;;
  esac
}

############## MAIN ###############

do_main "$@"
shift
if [[ -z $1 ]] ; then
  [[ -n $H ]] && echo $H | tr ' ' '\n' || echo -e "\nThis ROLE has no hosts assigned\n"
else
  for h in $H ; do ssh $h "$@" ; done
fi
