#!/usr/bin/python3
# *-* coding: utf-8 *-*

"""
list_tasks: list tasks in database
"""

import os
import yaml
from modules.classes import MySQLite
from modules.cp_inputs import press_any_key

# load config yaml
CONFIG_YAML = f"{os.environ['COCKPIT_HOME']}/config/config.yaml"
with open(CONFIG_YAML, 'r', encoding="utf-8") as yf:
    data = yaml.safe_load(yf)

COCKPIT_DB_HOME = data['params']['cockpit']['db_home']
COCKPIT_DB_NAME = data['params']['cockpit']['db_name']
COCKPIT_DB = f"{COCKPIT_DB_HOME}/{COCKPIT_DB_NAME}"

# instantiate db object
sqlitedb = MySQLite(COCKPIT_DB)

# get tasks uids
task_uids = [t[0] for t in sqlitedb.select("SELECT uid FROM tasks GROUP BY uid")]
if len(task_uids) > 0:
    SPC = '- -'
    print(f'{"Type":<12} | {"UID":<40} | {"# Jobs":<6}')
    print("-"*12 + SPC + "-"*40 + SPC + "-"*6)
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
