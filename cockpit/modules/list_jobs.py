#!/usr/bin/python3
# *-* coding: utf-8 *-*

import os
import yaml
import sqlite3
from sqlite3 import Error
from signal import SIGINT, signal
from modules.functions import handler, create_connection, list_registered_job_names


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
jobs = list_registered_job_names(conn)
if len(jobs) > 0:
    w = 41
    print("-"*w + f'\n| {"Id":^4} | {"Name":^30} |\n' + "-"*w)
    for job_id, job_name in jobs:
        print(f'| {job_id:<4} | {job_name:<30} |')
    print("-"*w)
else:
    print("No jobs found")

input("\nPress ENTER to go back to the main menu")
