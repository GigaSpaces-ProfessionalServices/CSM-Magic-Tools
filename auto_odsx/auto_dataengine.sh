#!/bin/bash

# ./odsx.py dataengine oracle-feeder start oraclefeeder_tl_kurs
# auto_dataengine.sh -f oracle -t tl_kurs
# |       32 | oraclefeeder_tl_kurs              | gsprod-space2 | ['oracle_tl_kurs']              | NA             | intact   |

usage() {
  cat << EOF

  USAGE: $(basename $0) [<option>] [<action>]

  OPTIONS:

  -f <feeder>      Feeder type
  -t <table>       Table name
  
  ACTIONS:

  start            Start feeder
  stop             Stop feeder

  EXAMPLE:
  $(basename $0) -f oracle -t tl_kurs start
  $(basename $0) -f mssql -t Portal_Calendary_Changes_View start

EOF
exit
}

[[ $# -eq 0 ]] && usage 

while [[ $# -gt 0 ]] ; do
  case $1 in
    "-f") shift ; feeder="${1,,}" ;;
    "-t") shift ; table="${1,,}" ;;
    "start") action=start ;;
    "stop") action=stop ;;
    *) echo -e "\nWrong option.\n" ; usage ;;
  esac
  shift
done

# cd /dbagiga/gs-odsx ; ./odsx.py dataengine oracle-feeder start oraclefeeder_ta_sem
echo -e "./odsx.py dataengine ${feeder}-feeder ${action} ${feeder}feeder_${table}"
cd /dbagiga/gs-odsx
./odsx.py dataengine ${feeder}-feeder ${action} ${feeder}feeder_${table}
