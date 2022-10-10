#!/usr/bin/python3
# *-* coding: utf-8 *-*

"""
list_policies: list policies in database
"""
import os
import sys
# adding modules to path for imports
sys.path.insert(1, os.path.realpath(f"{os.path.dirname(__file__)}/../modules"))

import yaml
from classes import MySQLite
from cp_inputs import press_any_key

# load config yaml
CONFIG_YAML = f"{os.environ['COCKPIT_HOME']}/config/config.yaml"
with open(CONFIG_YAML, 'r', encoding="utf-8") as yf:
    data = yaml.safe_load(yf)

COCKPIT_DB_HOME = data['params']['cockpit']['db_home']
COCKPIT_DB_NAME = data['params']['cockpit']['db_name']
COCKPIT_DB = f"{COCKPIT_DB_HOME}/{COCKPIT_DB_NAME}"

# instantiate db object
sqlitedb = MySQLite(COCKPIT_DB)

# get policies
policies = sqlitedb.select("SELECT * FROM policies ORDER BY name;")

if len(policies) > 0:
    SPC = '- -'
    print(f'{"Id":<4} | {"Name":<30} | {"UID":<38} | {"Schedule":<8} | {"Retry":<7} | {"Associated Task UID":<38} | {"Is_Active":<11}')
    print("-"*4 + SPC + "-"*30 + SPC + "-"*38 + SPC + "-"*8 + SPC + "-"*7 + SPC + "-"*38 + SPC + "-"*11)
    for policy in policies:
        p_id = policy[0]
        p_uid = policy[1]
        p_name = f"{policy[2]}"
        p_sched = f"{policy[3]} sec"
        p_retry = f"{policy[4]} times"
        p_assoc_task = policy[6]
        p_active = f"{policy[9]}"
        print(f'{p_id:<4} | {p_name:<30} | {p_uid:<38} | {p_sched:<8} | {p_retry:<7} | {p_assoc_task:<38} | {p_active:<11}')
else:
    print("No policies found")

press_any_key()
