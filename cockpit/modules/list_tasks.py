#!/usr/bin/python3
# *-* coding: utf-8 *-*


import os
import yaml
import sqlite3
from sqlite3 import Error
from signal import SIGINT, signal
from functions import *
from modules.functions import handler, create_connection, list_tasks


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
tasks = list_tasks(conn)
if len(tasks) > 0:
    w = 62
    print("-"*w + f'\n| {"Id":^4} | {"UID":^40} | {"Type":^8} |\n' + "-"*w)
    for task_id, task_uid, task_type in tasks:
        print(f'| {task_id:<4} | {task_uid:<40} | {task_type:<8} |')
    print("-"*w)
else:
    print("No tasks found")

input("\nPress ENTER to go back to the main menu")
