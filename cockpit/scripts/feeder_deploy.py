#!/usr/bin/python3
# *-* coding: utf-8 *-*

"""
feeder_deploy; deploy a feeder on the ods grid
"""

import os
from time import sleep
import subprocess
import json
import yaml
from modules.classes import Spinner
from modules.cp_utils import check_connection
from modules.cp_print import pretty_print
from modules.cp_inputs import press_any_key, validate_option_select

# main
CONFIG_YAML = f"{os.environ['COCKPIT_HOME']}/config/config.yaml"

# load config yaml
with open(CONFIG_YAML, 'r', encoding="utf-8") as y:
    data = yaml.safe_load(y)

COCKPIT_DB_HOME = data['params']['cockpit']['db_home']
COCKPIT_DB_NAME = data['params']['cockpit']['db_name']
COCKPIT_DB = f"{COCKPIT_DB_HOME}/{COCKPIT_DB_NAME}"

# choice env
i = 1
environments = {}   # generate environments dictionary
for key in data['params'].keys():
    if key != 'cockpit':
        environments[i] = [f'{key}'.upper(), data['params'][key]['variables']['pivot']]
        i += 1
choices = validate_option_select(environments, "Where would you like to deploy the feeder?")
envs_selected = {}
for choice in choices:
    envs_selected[int(choice)] = [environments[int(choice)][0], environments[int(choice)][1]]

# choice feeder
feeders = {}    # generate feeders dictionary
for key, val in data['feeders'].items():
    feeders[key] = [val['name']]
print('\n\n')
choices = validate_option_select(feeders, "Select feeder(s) to deploy?")
feeders_selected = {}
for choice in choices:
    feeders_selected[int(choice)] = [feeders[int(choice)][0]]
print('\n\n')

# run feeder deployments
for e in envs_selected.values():
    env_name = e[0].lower()
    pivot = data['params'][env_name]['endpoints']['pivot']
    PORT = 22
    if check_connection(pivot, PORT):
        for f in feeders_selected.values():
            feeder_name = f[0].lower()
            ### FOR DEMO ###
            spinner = Spinner
            title = f"Deploying '{feeder_name} feeder' on {env_name}... "
            with spinner(title, delay=0.1):
                sleep(2)
                import sys
                sys.stdout.write('\b')
                print('done')

            # cmd = f'ssh {pivot} "/dbagiga/josh/auto_odsx/auto_{feeder_name}feederdeploy"'
            # response = subprocess.run([cmd], shell=True, stdout=subprocess.PIPE).stdout.decode()
            # response = json.loads(response.replace("\'", "\""))


            #
            #
            ### NEED TO ADD A ROUTINE TO CHECK THAT FEEDER DEPLOYMENT IS SUCCESSFUL ###
            #
            #
    else:
        pretty_print(f"ERROR: connection to {e[0]} pivot ({pivot}:{PORT}) \
            could not be established", 'red')
press_any_key()
