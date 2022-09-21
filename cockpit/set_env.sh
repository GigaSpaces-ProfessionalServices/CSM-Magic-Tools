#!/bin/bash

#
# setting COCKPIT_HOME value in bashrc
#

COCKPIT_HOME=$(realpath $(dirname $0))

# remove current setting
sed -i '/COCKPIT_HOME=/d' ~/.bashrc

# insert new setting
echo "export COCKPIT_HOME=${COCKPIT_HOME}" >> ~/.bashrc

# source it
source ~/.bashrc && exec bash

exit