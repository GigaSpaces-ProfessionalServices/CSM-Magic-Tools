#!/usr/bin/bash
ff=$1
local_ip=$(/usr/sbin/ifconfig |grep -A 1 eth0|awk '/inet/ {print $2}')
cat $ff |grep -v GS_NIC_ADDRESS > /tmp/tmp_setenv-override.sh
cp /tmp/tmp_setenv-override.sh $ff
echo "export GS_NIC_ADDRESS="$local_ip >> $ff
