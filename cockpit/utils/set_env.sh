#!/bin/bash

#
# setting environment variables in /etc/environment
#

#ENV_FILE=/etc/environment
COCKPIT_HOME=$(realpath $(dirname $0)/../)
BASHRC=$(realpath ~/.bashrc)

# set PYTHONPATH
if [[ $PYTHONPATH == "" ]]; then
    PYTHONPATH=${COCKPIT_HOME}
else
    PYTHONPATH="${PYTHONPATH}:${COCKPIT_HOME}"
fi

sed -i '/PYTHONPATH=/d' $BASHRC
echo "export PYTHONPATH=${PYTHONPATH}" >> $BASHRC
 
# set COCKPIT_HOME
sed -i '/COCKPIT_HOME=/d' $BASHRC
echo "export COCKPIT_HOME=${COCKPIT_HOME}" >> $BASHRC

# set pivots
sed -i '/PIVOT_.*=/d' $BASHRC
echo "PIVOT_PRD=1.1.1.1" >> $BASHRC
echo "PIVOT_DR=2.2.2.2" >> $BASHRC

# source it
source $BASHRC && exec bash


# sed -i '/PYTHONPATH=/d' $ENV_FILE
# echo "export PYTHONPATH=${PYTHONPATH}" >> $ENV_FILE
 
# # set COCKPIT_HOME
# sed -i '/COCKPIT_HOME=/d' $ENV_FILE
# echo "export COCKPIT_HOME=${COCKPIT_HOME}" >> $ENV_FILE

# # set pivots
# sed -i '/PIVOT_.*=/d' $ENV_FILE
# echo "PIVOT_PRD=1.1.1.1" >> $ENV_FILE
# echo "PIVOT_DR=2.2.2.2" >> $ENV_FILE

# source it
# source $ENV_FILE && exec bash
