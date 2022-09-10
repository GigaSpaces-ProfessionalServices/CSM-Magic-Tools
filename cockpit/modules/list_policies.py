#!/usr/bin/python3
# *-* coding: utf-8 *-*


import os
import yaml
import sqlite3
from sqlite3 import Error
from signal import SIGINT, signal
from functions import handler, create_connection, list_policies


# main
config_yaml = f"{os.path.dirname(os.path.abspath(__file__))}/../config/config.yaml"

# catch user CTRL+C key press
signal(SIGINT, handler)

# load config yaml
with open(config_yaml, 'r') as yf:
    data = yaml.safe_load(yf)
cockpit_db_home = data['params']['cockpit']['db_home']
cockpit_db_name = data['params']['cockpit']['db_name']
cockpit_db = f"{cockpit_db_home}/{cockpit_db_name}"
conn = create_connection(cockpit_db)
policies = list_policies(conn)
if len(policies) > 0:
    w = 76
    print("-"*w + f'\n| {"Id":<4} | {"Schedule":<10} | {"Repeat":<10}| {"Associated Task":<40} |\n' + "-"*w)
    for policy in policies:
        p_id = policy[0]
        p_sched = policy[1]
        p_repeat = policy[2]
        p_assoc_task = policy[3]
        print(f'| {p_id:<4} | {p_sched:<10} | {p_repeat:<10}| {p_assoc_task:<40} |')
    print("-"*w)
else:
    print("No policies found")

input("\nPress ENTER to go back to the main menu")
