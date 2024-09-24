#!/bin/bash

deploy() {
/usr/bin/expect -c '
set timeout -1
set force_conservative 1

cd /dbagiga/gs-odsx
spawn ./odsx.py dataengine notifier install-deploy
expect "Are you sure want to proceed"
sleep .5
send -- "\n"

expect eof
'
}

undeploy() {
/usr/bin/expect -c '
set timeout -1
set force_conservative 1

cd /dbagiga/gs-odsx
spawn ./odsx.py dataengine notifier remove-undeploy
expect "Proceeding with manager host"
sleep .5
send -- "\n"
expect "For all above PUs"
sleep .5
send -- "\n"
expect "Enter drain mode"
sleep .5
send -- "\n"
expect "Enter drain mode timeout"
sleep .5
send -- "\n"
expect "Do you want to remove gsc"
sleep .5
send -- "\n"

expect eof
'
}


usage() {
  cat << EOF

  USAGE:

   $(basename $0) [<action>] [<option>]

  OPTIONS:

  -d                  DEPLOY install
  -u                  UNDEPLOY remove
  -l                  LIST
  -h                  HELP

  EXAMPLES:

  $(basename $0) -d                           # Deploy notifiers

EOF
exit
}

do_menu() {
  [[ $# -eq 0 ]] && usage
  while [[ $# -gt 0 ]] ; do
    case $1 in
      "-d")
        deploy
        ;;
      "-u")
        undeploy
        ;;
      "-l")
        cd /giga/gs-odsx ; ./odsx.py dataengine notifier list
        ;;
      *) echo -e "\nOption not supported\n"
        ;;
    esac
    exit
  done
}

############### MAIN

do_menu "${@}"
