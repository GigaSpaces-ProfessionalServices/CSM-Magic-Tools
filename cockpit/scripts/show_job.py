#!/usr/bin/python3
# *-* coding: utf-8 *-*

"""
show_job: show job attributes
"""

import os
import yaml
from modules.classes import MySQLite
from modules.cp_inputs import press_any_key

CONFIG_YAML = f"{os.environ['COCKPIT_HOME']}/config/config.yaml"

# load config yaml
with open(CONFIG_YAML, 'r', encoding="utf-8") as yf:
    data = yaml.safe_load(yf)

COCKPIT_DB_HOME = data['params']['cockpit']['db_home']
COCKPIT_DB_NAME = data['params']['cockpit']['db_name']
COCKPIT_DB = f"{COCKPIT_DB_HOME}/{COCKPIT_DB_NAME}"

# instantiate db object
sqlitedb = MySQLite(COCKPIT_DB)

print("### [ TBD ] ### SHOW JOB ###")

press_any_key()
