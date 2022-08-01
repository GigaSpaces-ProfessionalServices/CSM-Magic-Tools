#!/bin/bash
# /usr/local/bin/pipeline-state.sh

# Query DB2 pipeline reference table for last update time, time should be UTC, convert it to POSIX nanosecond EPOCH time.
echo "pipelineState,pipeline=db2 last=$(date '+%s')000000000"

# Query MSSQL pipeline reference table for last update time, time should be UTC, convert it to POSIX EPOCH time.
echo "pipelineState,pipeline=mssql last=$(date '+%s')000000000"

