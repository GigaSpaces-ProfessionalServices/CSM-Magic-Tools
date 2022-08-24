#!/bin/bash

#
# this script executes a local python script on the pivot via ssh
#

PIVOT=$1
PYTHON_FILE="/usr/local/bin/get_obj_count.py"

echo $(cat ${PYTHON_FILE} | ssh ${PIVOT} python3 -)

exit

