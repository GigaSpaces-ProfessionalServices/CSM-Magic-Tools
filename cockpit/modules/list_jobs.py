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

sql = "SELECT id, name FROM jobs"
jobs = sqlitedb.select(sql)
j_len = []
for ji, jn in jobs:
    j_len.append(len(jn))
if len(jobs) > 0:
    spc = '- -'
    print(f'{"Id":<4} | Name')
    print("-"*4 + spc + "-"*max(j_len))
    for job_id, job_name in jobs:
        print(f'{job_id:<4} | {job_name}')
else:
    print("No jobs found")
press_any_key()
