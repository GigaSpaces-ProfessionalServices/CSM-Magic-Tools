#!/usr/bin/python3
# *-* coding: utf-8 *-*

### DEPLOY MSSQL FEEDER ###

import os
from time import sleep
import yaml
from signal import SIGINT, signal
import subprocess
import json
from functions import create_connection, handler, \
    check_connection, validate_input, get_selection, \
        pretty_print
from spinner import Spinner

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

# generate environments dictionary
index = 1
environments = {}
for k, v in data['params'].items():
    if k != 'cockpit':
        environments[index] = [f'{k}'.upper(), data['params'][k]['variables']['pivot']]
        index += 1

# choice env
q = "Where would you like to deploy the feeder?"
choice = get_selection(environments, 'Environments', q)
envs = {}
if choice == 'ALL':
    env_select = environments
else:
    env_select = {1: [environments[int(choice)][0], environments[int(choice)][1]]}

# generate feeders dictionary
feeders = {}
for k, v in data['feeders'].items(): feeders[k] = [v['name']]

# choice feeder
q = "Which feeder would you like to deploy?"
choice = get_selection(feeders, 'Feeders', q)
feeder_select = {}
if choice == 'ALL':
    feeder_select = feeders
else:
    feeder_select = {1: [feeders[int(choice)][0]]}

# run feeder deployments
for e in env_select.values():
    env_name = e[0].lower()
    pivot = data['params'][env_name]['endpoints']['pivot']
    port = 22
    if check_connection(pivot, port):
        for f in feeder_select.values():
            feeder_name = f[0].lower()
            
            # FOR DEMO
            spinner = Spinner
            with spinner(f"\nDeploying '{feeder_name} feeder' on {env_name}", delay=0.1):
                sleep(2)
            #cmd = f'ssh {pivot} "/dbagiga/josh/auto_odsx/auto_{feeder_name}feederdeploy"'
            #response = subprocess.run([cmd], shell=True, stdout=subprocess.PIPE).stdout.decode()
            #response = json.loads(response.replace("\'", "\""))
            #
            #
            ### NEED TO ADD A ROUTINE TO CHECK THAT FEEDER DEPLOYMENT IS SUCCESSFUL ###
            #
            #
    else:
        pretty_print(f"ERROR: connection to {e[0]} pivot ({pivot}:{port}) could not be established", 'red')
input("\nPress ENTER to go back to the main menu")