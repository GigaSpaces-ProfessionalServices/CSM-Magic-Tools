#!/usr/bin/python3
# *-* coding: utf-8 *-*
"""
create_new_task; create new cockpit task
"""

import os
import sys
import uuid
import datetime
import yaml
from colorama import Fore, Style
from modules.cp_utils import sort_tuples_list
from modules.classes import MySQLite
from modules.cp_print import pretty_print
from modules.cp_inputs import (
    press_any_key,
    get_user_ok,
    validate_option_select,
    validate_type_select
)

# load config yaml
CONFIG_YAML = f"{os.environ['COCKPIT_HOME']}/config/config.yaml"
with open(CONFIG_YAML, 'r', encoding="utf-8") as yf:
    data = yaml.safe_load(yf)

COCKPIT_DB_HOME = data['params']['cockpit']['db_home']
COCKPIT_DB_NAME = data['params']['cockpit']['db_name']
COCKPIT_DB = f"{COCKPIT_DB_HOME}/{COCKPIT_DB_NAME}"

# instantiate db object
sqlitedb = MySQLite(COCKPIT_DB)

# generate db data by columns
t_uid = str(uuid.uuid4())

# introduction
intro = [
    'The task acts as a "container" for the jobs associated with it.'
    ]
for line in intro:
    pretty_print(line, 'LIGHTBLUE_EX')

# list the environments
the_envs = [e for e in data['params'] if e != 'cockpit']

# get type of task from user
print()
choice = validate_type_select(data['tasks'])
if choice is None or choice == -1:
    sys.exit()

t_type = data['tasks'][choice]['name']
t_type_sn = choice
t_metadata = "NULL"
t_content = "NULL"
t_state = "NULL"
t_created = datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')

# list jobs from db
jobs = sqlitedb.select("SELECT * FROM jobs")
jobs_selected = False

# get jobs selections
if len(jobs) > 0:
    # conform jobs to dictionary for validation func
    jobs_dict = {}
    for j in jobs:
        jobs_dict[j[0]] = list(j[1:])
    print('\n')
    title = "\nChoose jobs to be executed by this task:"
    while True:
        selected_jobs = validate_option_select(jobs_dict, title, _esc_to='Skip')        
        if selected_jobs is None:
            sys.exit()
        if len(selected_jobs) != len(the_envs):
            pretty_print("ERROR: number of jobs must correspond to number of environments. try again.", 'red')
            continue
        arr = []
        for j in selected_jobs:
            arr.append(sqlitedb.select(f"SELECT content FROM jobs WHERE id = {j}")[0][0])
        if arr.count(arr[0]) != len(arr):
            pretty_print("ERROR: selected jobs' object type is not the same!", 'red')
            continue
        jobs_selected = True
        break

# get user confirmation
print()
pretty_print("\n\n(!) Please confirm the following task registration:", 'yellow')
print(f"Task: {t_type}")
if not jobs_selected:
    print("Associated Jobs:\n   None (* can be set later from the 'Edit Tasks' menu)")
else:
    print("Associated Jobs:")
    for job_id in selected_jobs:
        rows = sqlitedb.select(f"SELECT name FROM jobs WHERE id = {job_id}")
        print(f"{' ':<3}{rows[0][0]}")
if not get_user_ok("\nContinue with task registration?"):
    sys.exit()

# execute registration
SQL = """ INSERT INTO tasks(uid,type,sn_type,job_id,metadata,content,state,created)
VALUES(?,?,?,?,?,?,?,?) """
if jobs_selected:
    for job_id in selected_jobs:
        task_data = (t_uid,t_type,t_type_sn,job_id,t_metadata,t_content,t_state,t_created)
        r = sqlitedb.insert(SQL, task_data)
else:
    task_data = (t_uid,t_type,t_type_sn,'NULL',t_metadata,t_content,t_state,t_created)
    r = sqlitedb.insert(SQL, task_data)
print(f"\n\nTask '{t_type}' with UID {t_uid} \
    {Fore.GREEN}registered successfully!{Style.RESET_ALL}")

press_any_key()
