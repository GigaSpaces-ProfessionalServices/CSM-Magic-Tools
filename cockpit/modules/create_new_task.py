#!/usr/bin/python3
# *-* coding: utf-8 *-*

import os
import yaml
import uuid
import datetime
from colorama import Fore, Style
from functions import (
    create_connection,
    get_user_ok,
    press_any_key,
    pretty_print,
    validate_option_select, 
    validate_type_select, 
    db_select,
    db_insert
    )

# main
config_yaml = f"{os.environ['COCKPIT_HOME']}/config/config.yaml"

# load config yaml
with open(config_yaml, 'r') as yf:
    data = yaml.safe_load(yf)
# db from yaml
cockpit_db_home = data['params']['cockpit']['db_home']
cockpit_db_name = data['params']['cockpit']['db_name']
cockpit_db = f"{cockpit_db_home}/{cockpit_db_name}"

# generate db data by columns
t_uid = str(uuid.uuid4())

# introduction
intro = [
    'The task defines the ultimate desired goal.',
    'It acts as a "container" for the jobs associated with it.'
    ]
for line in intro: pretty_print(line, 'LIGHTBLUE_EX')

# get type of task from user
print()
choice = validate_type_select(data['tasks'])
if choice == None or choice == -1:
    quit()

t_type = data['tasks'][choice]['name']
t_type_sn = choice
t_metadata = "NULL"
t_content = "NULL"
t_state = "NULL"
t_created = datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')
# list jobs from db
conn = create_connection(cockpit_db)
sql = "SELECT * FROM jobs"
jobs = db_select(conn, sql)
jobs_selected = False

# get jobs selections
if len(jobs) > 0:
    # conform jobs dictionary to validation func
    jobs_dict = {}
    for j in jobs:
        jobs_dict[j[0]] = list(j[1:])
    print('\n')
    title = "\nChoose jobs to be executed by this task:"
    selected_jobs = validate_option_select(jobs_dict, title, esc_to='Skip')
    if selected_jobs == None: quit()    
    if selected_jobs[0] != -1: jobs_selected = True

# get user confirmation
print()
pretty_print("\n\n(!) Please confirm the following task registration:", 'yellow')
print(f"Task: {t_type}")
if not jobs_selected:
    print(f"Associated Jobs:\n   None (* can be set later from the 'Edit Tasks' menu)")
else:
    print(f"Associated Jobs:")
    for job_id in selected_jobs:
        cur = conn.cursor()
        sql = f"SELECT name FROM jobs WHERE id = {job_id}"
        cur.execute(sql)
        rows = cur.fetchall()
        print(f"{' ':<3}{rows[0][0]}")
if not get_user_ok("\nContinue with task registration?"): quit()

# execute registration
sql = """ INSERT INTO tasks(uid,type,sn_type,job_id,metadata,content,state,created) 
VALUES(?,?,?,?,?,?,?,?) """
if jobs_selected:
    for job_id in selected_jobs:
        task_data = (t_uid,t_type,t_type_sn,job_id,t_metadata,t_content,t_state,t_created)
        r = db_insert(conn, sql, task_data)
else:
    task_data = (t_uid,t_type,t_type_sn,'NULL',t_metadata,t_content,t_state,t_created)
    r = db_insert(conn, sql, task_data)
print(f"\n\nTask '{t_type}' with UID {t_uid} {Fore.GREEN}registered successfully!{Style.RESET_ALL}")

press_any_key()
