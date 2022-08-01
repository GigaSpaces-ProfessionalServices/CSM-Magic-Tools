#!/bin/bash
#
# Output should be:
# metricname[,tagname=tagvalue].. valuename=value[,valuename=value]
# Tag hostname will be added automatically by telegraf.

# Tiered storage location:
TSL="/dbagigadata/tiered-storage"

find $TSL -type f -name '*wal' -ls 2>/dev/null | \
awk '{
	gsub("'$TSL'","",$11)
	i=split($11,space,"/")
	print "walSize,space="space[i-1]" size="$7
}'  
