#!/usr/bin/python3
# *-* coding: utf-8 *-*


import os
import yaml
import sqlite3
from sqlite3 import Error
from functions import create_connection, list_policies


# main
config_yaml = f"{os.environ['COCKPIT_HOME']}/config/config.yaml"

# load config yaml
with open(config_yaml, 'r') as yf:
    data = yaml.safe_load(yf)
cockpit_db_home = data['params']['cockpit']['db_home']
cockpit_db_name = data['params']['cockpit']['db_name']
cockpit_db = f"{cockpit_db_home}/{cockpit_db_name}"
conn = create_connection(cockpit_db)
policies = list_policies(conn,'')
if len(policies) > 0:
    spc = '- -'
    print(f'{"Id":<4} | {"Name":<30} | {"UID":<38} | {"Schedule":<8} | {"Associated Task":<38} | {"Is_Active":<11}')
    print("-"*4 + spc + "-"*30 + spc + "-"*38 + spc + "-"*8 + spc + "-"*38 + spc + "-"*11)
    for policy in policies:
        p_id = policy[0]
        p_uid = policy[1]
        p_name = f"{policy[2]}"
        p_sched = f"{policy[3]} sec"
        p_assoc_task = policy[4]
        p_active = f"{policy[7]}"
        print(f'{p_id:<4} | {p_name:<30} | {p_uid:<38} | {p_sched:<8} | {p_assoc_task:<38} | {p_active:<11}')
else:
    print("No policies found")

input("\nPress ENTER to go back to the main menu")
