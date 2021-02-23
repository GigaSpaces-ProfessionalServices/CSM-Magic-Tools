#!/usr/bin/bash
gs_home=/opt/gigaspaces-smart-ods-enterprise-15.8.0/
contCount=1
zone=AAAA
mem=1234

for host in `$gs_home/bin/gs.sh host list |awk '/compute/ {print $1}'`
	do
	echo  "$gs_home/bin/gs.sh container create --count=$contCount --zone=$zone --memory=$mem $host"
	done

