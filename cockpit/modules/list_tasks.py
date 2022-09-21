#!/usr/bin/python3
# *-* coding: utf-8 *-*


import os
import yaml
# import sqlite3
# from sqlite3 import Error
from signal import SIGINT, signal
from functions import *
from functions import handler, create_connection, list_tasks_grouped


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
tasks = list_tasks_grouped(conn, 'uid', 'type')
if len(tasks) > 0:
    spc = '- -'
    print(f'{"Type":<12} | {"UID":<40} | {"# Jobs":<6}')
    print("-"*12 + spc + "-"*40 + spc + "-"*6)
    for task_uid, task_type in tasks:
        # count number of jobs for each task uid
        cur = conn.cursor()
        cur.execute("SELECT * FROM tasks WHERE uid = ?;", (task_uid,))
        num_of_jobs = len(cur.fetchall())
        print(f'{task_type:<12} | {task_uid:<40} | {num_of_jobs:<6}')
else:
    print("No tasks found")

input("\nPress ENTER to go back to the main menu")
