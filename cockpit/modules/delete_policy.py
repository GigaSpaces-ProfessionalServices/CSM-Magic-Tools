#!/usr/bin/python3
# *-* coding: utf-8 *-*

from operator import index
import os
import yaml
from signal import SIGINT, signal
from functions import create_connection, get_selection, \
    handler, list_policies

# main
config_yaml = f"{os.environ['COCKPIT_HOME']}/config/config.yaml"

# catch user CTRL+C key press
signal(SIGINT, handler)

# load config yaml
with open(config_yaml, 'r') as yf:
    data = yaml.safe_load(yf)
cockpit_db_home = data['params']['cockpit']['db_home']
cockpit_db_name = data['params']['cockpit']['db_name']
cockpit_db = f"{cockpit_db_home}/{cockpit_db_name}"
conn = create_connection(cockpit_db)

# list policies
policies = {}
index = 1
for pol in list_policies(conn, ''):
    policies.update({index:[f'{pol[2]}']})
    index += 1
if len(policies) > 0:
    # choice policy
    q = f"Which policy would you like to delete?"
    choice = get_selection(policies, 'Policies', q)
    if choice == 'ALL':
        pol_selected = policies
    else:
        pol_selected = {1: [policies[int(choice)][0]]}
else:
    print("No policies found")

print(f"\n[DEMO]\nCHOSE TO DELETE POLICY: {pol_selected[int(choice)][0]}")


    # TO REMOVE
    # systemctl stop cockpit_policy_60.timer 
    # systemctl disable cockpit_policy_60.timer 
    # rm -f /etc/systemd/system/cockpit_policy_60.*
    # systemctl daemon-reload


input("\nPress ENTER to go back to the main menu")
