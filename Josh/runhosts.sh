#!/bin/bash

[[ ! -x  /dbagiga/utils/host-yaml.sh ]] && { echo -e "\nFile host-yaml.sh is required by script\n" ; exit 1 ; }

check_disk_usage() {
  perc=$2
  ssh "$1" 'bash -s' "${perc}" <<-'ENDSSH'
    perc=$1
    # Run df -h to get disk usage information, then use awk to process the output
    df -h | awk -v threshold="${perc}" '(NR > 1) && ($5+0 > threshold) {printf "%-20s %-40s %-10s %-10s\n", "'$(hostname)'", $6, $5, $2}'
ENDSSH
}

#check_disk_usage() {
#  ssh "${1}" 'bash -s' <<-'ENDSSH'
#    usage_threshold="${2}"
#    # Print the header of the table
#    printf "%-20s %-40s %-10s %-10s\n" "Hostname" "Partition" "Usage (%)" "Total Size"
#    # Run df -h to get disk usage information, then use awk to process the output
#    df -h | awk -v threshold="${usage_threshold}" '(NR > 1) && ($5+0 > threshold) {printf "%-20s %-40s %-10s %-10s\n", "'$(hostname)'", $6, $5, $2}'
#ENDSSH
#}

disk_usage() {
  if [[ -z $1 ]] ; then
    echo -e "\nUsage threshold not specified - setting it to 7%.\n"
    local usage_threshold=7
  else
    usage_threshold=$1
  fi
  # Print the header of the table
  printf "%-20s %-40s %-10s %-10s\n" "Hostname" "Partition" "Usage (%)" "Total Size"
  for h in $(host-yaml.sh -A) ; do
    check_disk_usage $h "${usage_threshold}" | grep -v 'docker\|container'
  done
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
    -hu     Show hostname and uptime
    -df x   Show partitions above x percent

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
    "-hu") runhosts.sh -A 'printf "%-20s %s\n" "$(hostname)" "$(uptime)"' ; exit ;;
#   "-df") [[ -n $2 ]] && disk_usage "$2" || { echo -e "\nMust give percent as 2nd parameter\n" ; exit ; } ; exit ;;
    "-df") disk_usage "${2}" ;;
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

