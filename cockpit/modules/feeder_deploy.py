#!/usr/bin/python3
# *-* coding: utf-8 *-*

import os
from time import sleep
import yaml
import subprocess
import json
from functions import (
    create_connection, 
    check_connection,
    press_any_key, 
    validate_option_select, 
    pretty_print
    )
from spinner import Spinner

# main
config_yaml = f"{os.environ['COCKPIT_HOME']}/config/config.yaml"

# load config yaml
with open(config_yaml, 'r') as yf:
    data = yaml.safe_load(yf)
cockpit_db_home = data['params']['cockpit']['db_home']
cockpit_db_name = data['params']['cockpit']['db_name']
cockpit_db = f"{cockpit_db_home}/{cockpit_db_name}"
conn = create_connection(cockpit_db)

# choice env
index = 1
environments = {}   # generate environments dictionary
for k, v in data['params'].items():
    if k != 'cockpit':
        environments[index] = [f'{k}'.upper(), data['params'][k]['variables']['pivot']]
        index += 1
q = "Where would you like to deploy the feeder?"
choices = validate_option_select(environments, q)
envs = {}
for choice in choices:
    envs[int(choice)] = [environments[int(choice)][0], environments[int(choice)][1]]

# choice feeder
feeders = {}    # generate feeders dictionary
for k, v in data['feeders'].items(): feeders[k] = [v['name']]
print('\n\n')
q = "Select feeder(s) to deploy?"
choices = validate_option_select(feeders, q)
feeder_select = {}
for choice in choices:
    feeder_select[int(choice)] = [feeders[int(choice)][0]]
print('\n\n')

# run feeder deployments
for e in envs.values():
    env_name = e[0].lower()
    pivot = data['params'][env_name]['endpoints']['pivot']
    port = 22
    if check_connection(pivot, port):
        for f in feeder_select.values():
            feeder_name = f[0].lower()
            ### FOR DEMO ###
            spinner = Spinner
            title = f"Deploying '{feeder_name} feeder' on {env_name}... "
            with spinner(title, delay=0.1):
                sleep(2)
                import sys
                sys.stdout.write('\b')
                print('done')
            
            """
            cmd = f'ssh {pivot} "/dbagiga/josh/auto_odsx/auto_{feeder_name}feederdeploy"'
            response = subprocess.run([cmd], shell=True, stdout=subprocess.PIPE).stdout.decode()
            response = json.loads(response.replace("\'", "\""))
            """
            
            #
            #
            ### NEED TO ADD A ROUTINE TO CHECK THAT FEEDER DEPLOYMENT IS SUCCESSFUL ###
            #
            #
    else:
        pretty_print(f"ERROR: connection to {e[0]} pivot ({pivot}:{port}) could not be established", 'red')
press_any_key()