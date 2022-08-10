#!/bin/bash
# /usr/local/bin/pipeline-state.sh

# Query DB2 pipeline reference table for last update time, time should be UTC, convert it to POSIX nanosecond EPOCH time.
O=$(/usr/local/bin/test.sh 2>/dev/null)
[ $? -ne 0 ] && O='2000-01-01 00:00:00'
D=$(echo "$O" | grep -v logger)

echo "pipelineState,pipeline=db2 last=$(date -d "$D" '+%s')000000000"

# Query MSSQL pipeline reference table for last update time, time should be UTC, convert it to POSIX EPOCH time.
echo "pipelineState,pipeline=mssql last=$(date '+%s')000000000"

