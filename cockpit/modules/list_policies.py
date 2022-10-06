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

# get policies
sql = f"SELECT * FROM policies ORDER BY name;"
policies = sqlitedb.select(sql)

if len(policies) > 0:
    spc = '- -'
    print(f'{"Id":<4} | {"Name":<30} | {"UID":<38} | {"Schedule":<8} | {"Retry":<7} | {"Associated Task UID":<38} | {"Is_Active":<11}')
    print("-"*4 + spc + "-"*30 + spc + "-"*38 + spc + "-"*8 + spc + "-"*5 + spc + "-"*38 + spc + "-"*11)
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
