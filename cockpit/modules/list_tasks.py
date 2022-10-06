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

# get tasks uids
sql = "SELECT uid FROM tasks GROUP BY uid"
task_uids = [t[0] for t in sqlitedb.select(sql)]
if len(task_uids) > 0:
    spc = '- -'
    print(f'{"Type":<12} | {"UID":<40} | {"# Jobs":<6}')
    print("-"*12 + spc + "-"*40 + spc + "-"*6)
    for task_uid in task_uids:
        sql = f"SELECT 'uid', 'type', 'job_id' FROM tasks WHERE uid = '{task_uid}'"
        task = sqlitedb.select(sql)        
        for attr in task:
            _, task_type, task_job = attr
        if task_job != 'NULL':
            num_of_jobs = len(task)
        else:
            num_of_jobs = 0
        print(f'{task_type:<12} | {task_uid:<40} | {num_of_jobs:<6}')
else:
    print("No tasks found")
press_any_key()
