#!/usr/bin/python3
# *-* coding: utf-8 *-*

"""
list_jobs: list jobs from database
"""

import os
import yaml
from modules.classes import MySQLite
from modules.cp_inputs import press_any_key

# load config yaml
CONFIG_YAML = f"{os.environ['COCKPIT_HOME']}/config/config.yaml"
with open(CONFIG_YAML, 'r', encoding="utf-8") as yml:
    data = yaml.safe_load(yml)

COCKPIT_DB_HOME = data['params']['cockpit']['db_home']
COCKPIT_DB_NAME = data['params']['cockpit']['db_name']
COCKPIT_DB = f"{COCKPIT_DB_HOME}/{COCKPIT_DB_NAME}"

# instantiate db object
sqlitedb = MySQLite(COCKPIT_DB)

# list jobs
jobs = sqlitedb.select("SELECT id, name FROM jobs")
j_len = []
for ji, jn in jobs:
    j_len.append(len(jn))
if len(jobs) > 0:
    SPC = '- -'
    print(f'{"Id":<4} | Name')
    print("-"*4 + SPC + "-"*max(j_len))
    for job_id, job_name in jobs:
        print(f'{job_id:<4} | {job_name}')
else:
    print("No jobs found")

press_any_key()
