#!/usr/bin/python3
# *-* coding: utf-8 *-*

import os
import yaml
import sqlite3
from sqlite3 import Error
from signal import SIGINT, signal
from functions import handler, create_connection, list_jobs

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
jobs = list_jobs(conn, 'id', 'name')
j_len = []
for ji, jn in jobs:
    j_len.append(len(jn))
if len(jobs) > 0:
    print(f'{"Id":<4} | Name')
    print("-" * 4 + ' ' * 3 + "-" * max(j_len))
    for job_id, job_name in jobs:
        print(f'{job_id:<4} | {job_name}')
else:
    print("No jobs found")

input("\nPress ENTER to go back to the main menu")
