#!/bin/bash

get_tau_srno() {
/usr/bin/expect -c '
set timeout -1
set force_conservative 1

cd /dbagiga/gs-odsx
spawn ./odsx.py utilities scripts gcexplicit
expect "Enter Zone Srno"
sleep .5
send -- "\033"

expect eof
'
}

do_gc_all() {
srno="${1}" /usr/bin/expect -c '
set timeout -1
set force_conservative 1

cd /dbagiga/gs-odsx
spawn ./odsx.py utilities scripts gcexplicit
expect "Enter Zone Srno"
sleep .5
send -- "$env(srno)"
sleep .5
send -- "\r"
expect "Do you want to run on specific host"
sleep .5
send -- "n"
sleep .5
send -- "\r"

expect eof
'
}

################ MAIN ################
tau_srno=$(get_tau_srno | awk -F'|' '/\['\''tau'\''\]/ {print $2}' | sed -r 's/\x1B\[[0-9;]*[a-zA-Z]//g; s/[^[:print:]]//g; s/[ \t]//g')
do_gc_all $tau_srno
