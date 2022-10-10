#!/usr/bin/python3
# *-* coding: utf-8 *-*

"""
feeder_start: start deployed feeder
"""

import os
import yaml
from modules.cp_inputs import press_any_key

CONFIG_YAML = f"{os.environ['COCKPIT_HOME']}/config/config.yaml"

# load config yaml
with open(CONFIG_YAML, 'r', encoding="utf-8") as yf:
    data = yaml.safe_load(yf)

COCKPIT_DB_HOME = data['params']['cockpit']['db_home']
COCKPIT_DB_NAME = data['params']['cockpit']['db_name']
COCKPIT_DB = f"{COCKPIT_DB_HOME}/{COCKPIT_DB_NAME}"

print("### [ TBD ] ### START FEEDER ###")

press_any_key()
