#!/usr/bin/python3
# *-* coding: utf-8 *-*

### DEPLOY MSSQL FEEDER ###

import os
import yaml
from signal import SIGINT, signal
import subprocess
import json
from functions import create_connection, handler, \
    check_connection, validate_input

def get_selection(the_dict, description):
    # print menu
    print(description + "\n" + '=' * len(description))
    for k, v in the_dict.items():
        index = f"[{k}]"
        print(f'{index:<4} - {v[0]:<24}')
    if len(the_dict) > 1:
        index = f"[{k+1}]"
        item = "All"
        print(f'{index:<4} - {item:<24}')
    print(f'{"[99]":<4} - {"ESC":<24}')
    result = validate_input(the_dict)
    if result != -1:
        return result


# main
config_yaml = f"{os.path.dirname(os.path.realpath(__file__))}/../config/config.yaml"

# catch user CTRL+C key press
signal(SIGINT, handler)

# load config yaml
with open(config_yaml, 'r') as yf:
    data = yaml.safe_load(yf)
cockpit_db_home = data['params']['cockpit']['db_home']
cockpit_db_name = data['params']['cockpit']['db_name']
cockpit_db = f"{cockpit_db_home}/{cockpit_db_name}"
conn = create_connection(cockpit_db)

# generate environments dictionary
index = 1
environments = {}
for k, v in data['params'].items():
    if k != 'cockpit':
        environments[index] = [f'{k}'.upper(), data['params'][k]['variables']['pivot']]
        index += 1

# choice env
choice = get_selection(environments, "Where would you like to deploy the feeder?")
envs = {}
if choice == 'ALL':
    envs = environments
else:
    envs = {1: [environments[int(choice)][0], environments[int(choice)][1]]}

# generate feeders dictionary
feeders = {}
for k, v in data['feeders'].items(): feeders[k] = [v['name']]

# choice feeder
choice = get_selection(feeders, "Which feeder would you like to deploy?")
feed_select = {}
if choice == 'ALL':
    feed_select = feeders
else:
    feed_select = {1: [feeders[int(choice)][0], feeders[int(choice)][1]]}

# run deployments
connections_ok = []
for e in environments.values():
    env_name = e[0].lower()
    pivot = data['params'][env_name]['endpoints']['pivot']
    #exec_script = f"{os.path.dirname(os.path.realpath(__file__))}/get_space_objects.py"
    if check_connection(pivot, 22):
        for f in feeders.values():
            feeder_name = f[0].lower()
            cmd = f'ssh {pivot} "/dbagiga/josh/auto_odsx/auto_{feeder_name}feederdeploy"'
            response = subprocess.run([cmd], shell=True, stdout=subprocess.PIPE).stdout.decode()
            #response = json.loads(response.replace("\'", "\""))
            ###########################################################################
            ### NEED TO ADD A ROUTINE TO CHECK THAT FEEDER DEPLOYMENT IS SUCCESSFUL ###
            ###########################################################################

input("\nPress ENTER to go back to the main menu")