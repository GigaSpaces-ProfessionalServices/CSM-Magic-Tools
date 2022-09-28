#!/usr/bin/python3
# *-* coding: utf-8 *-*

import os
import yaml
import uuid
import datetime
from colorama import Fore, Style
from functions import (
    create_connection,
    press_any_key, 
    validate_option_select, 
    validate_type_select, 
    list_jobs, 
    register_task
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
# get task type from user
choice = validate_type_select(data['tasks'])
if choice != -1:
    t_type = data['tasks'][choice]['name']
    t_type_sn = choice
    t_metadata = "NULL"
    t_content = "NULL"
    t_state = "NULL"
    t_created = datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')
    # list jobs from db
    conn = create_connection(cockpit_db)
    jobs = list_jobs(conn, '')
    if len(jobs) > 0:
        # conform jobs dictionary to validation func
        jobs_dict = {}
        for j in jobs:
            jobs_dict[j[0]] = list(j[1:])
        print('\n')
        title = "\nChoose jobs to be executed by this task:"
        selected_jobs = validate_option_select(jobs_dict, title)
        print()
        if selected_jobs[0] == -1:  # if no jobs selected we register a task without jobs
            task_data = (t_uid,t_type,t_type_sn,'NULL',t_metadata,t_content,t_state,t_created)
            r = register_task(conn, task_data)
            print(f"Task {t_uid} {Fore.GREEN}created successfully!{Style.RESET_ALL}")
            print(f"Associated Jobs:\n   None")
        else:
            for job_id in selected_jobs:
                task_data = (t_uid,t_type,t_type_sn,job_id,t_metadata,t_content,t_state,t_created)
                r = register_task(conn, task_data)
            print(f"Task {t_uid} {Fore.GREEN}created successfully!{Style.RESET_ALL}")
            print(f"Associated Jobs:")
            for job_id in selected_jobs:
                cur = conn.cursor()
                sql = f"SELECT name FROM jobs WHERE id = {job_id}"
                cur.execute(sql)
                rows = cur.fetchall()
                print(f"{' ':<3}{rows[0][0]}")
    else:
        print("There are no jobs registered yet\n* can be set later from the 'Edit Tasks' menu")
        task_data = (t_uid,t_type,t_type_sn,'NULL',t_metadata,t_content,t_state,t_created)
        r = register_task(conn, task_data)
        print(f"Task {t_uid} {Fore.GREEN}created successfully!{Style.RESET_ALL}")
    press_any_key()
