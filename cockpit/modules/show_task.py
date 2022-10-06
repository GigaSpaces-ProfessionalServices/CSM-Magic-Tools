#!/usr/bin/python3
# *-* coding: utf-8 *-*


import os
import yaml
from classes import MySQLite
from functions import press_any_key

# main
config_yaml = f"{os.environ['COCKPIT_HOME']}/config/config.yaml"

# load config yaml
with open(config_yaml, 'r') as yf:
    data = yaml.safe_load(yf)

cockpit_db_home = data['params']['cockpit']['db_home']
cockpit_db_name = data['params']['cockpit']['db_name']
cockpit_db = f"{cockpit_db_home}/{cockpit_db_name}"

# instantiate db object
sqlitedb = MySQLite(cockpit_db)

print("### [ TBD ] ### SHOW TASK ###")

press_any_key()
