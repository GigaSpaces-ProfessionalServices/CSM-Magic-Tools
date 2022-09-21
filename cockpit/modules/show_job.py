#!/usr/bin/python3
# *-* coding: utf-8 *-*


import os
import yaml
from signal import SIGINT, signal
from functions import create_connection, handler

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

print("### [ TBD ] ### SHOW JOB ###")

input("\nPress ENTER to go back to the main menu")
