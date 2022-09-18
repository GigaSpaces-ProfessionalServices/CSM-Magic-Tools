#!/usr/bin/python3
# *-* coding: utf-8 *-*


import os
import yaml
import sqlite3
from sqlite3 import Error
from signal import SIGINT, signal
from functions import handler, create_connection, list_policies


# main
config_yaml = f"{os.path.dirname(os.path.realpath(__file__))}/../config/config.yaml"

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
    w = 93
    print("-" * w)
    print(f'| {"Id":<4} | {"Schedule":<10} | {"Repeat Times":<12}| {"Repeat Every":<12} | {"Associated Task UID":<40} |')    
    print("-" * w)
    for policy in policies:
        p_id = policy[0]
        p_sched = f"{policy[2]} sec"
        p_repeat = f"{policy[3]}"
        p_wait = f"{policy[4]} sec"
        p_assoc_task = policy[5]
        print(f'| {p_id:<4} | {p_sched:<10} | {p_repeat:<12}| {p_wait:<12} | {p_assoc_task:<40} |')
    print("-"*w)
else:
    print("No policies found")

input("\nPress ENTER to go back to the main menu")
