#!/usr/bin/python3
# *-* coding: utf-8 *-*

import os
import yaml
import uuid
import datetime
from signal import SIGINT, signal
from colorama import Fore, Style
from functions import handler, create_connection, \
    parse_jobs_selections, get_type_selection, \
        list_jobs, register_task

# main
config_yaml = f"{os.environ['COCKPIT_HOME']}/config/config.yaml"

# catch user CTRL+C key press
signal(SIGINT, handler)

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
result = get_type_selection(data['tasks'])
if result != -1:
    t_type = data['tasks'][result]['name']
    t_type_sn = result
    t_metadata = "NULL"
    t_content = "NULL"
    t_state = "NULL"
    t_created = datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')
    # list jobs from db
    print("\nChoose jobs to be executed by this task.")
    note = "(!) collections are supported (i.e: 1,3,2-5)"
    print(f"{note}\n" + '='*len(note))
    conn = create_connection(cockpit_db)
    jobs = list_jobs(conn)
    if len(jobs) > 0:
        for j in jobs:
            index = f"[{j[0]}]"
            print(f'{index:<4} - {j[1]:<24}')
        print(f"{'[99]':<4} - Skip (can be set later from the 'Edit Tasks' menu)")
        selected_jobs = parse_jobs_selections(jobs) ##### THIS SHOULD CHANGE TO GENERIC PARSE_MULTI_SELECT INSTEAD
        print()
        if selected_jobs[0] == -1:  # if no jobs selected we register a task without jobs
            task_data = (t_uid,t_type,t_type_sn,'NULL',t_metadata,t_content,t_state,t_created)
            r = register_task(conn, task_data)
            print(f"Task {t_uid} {Fore.GREEN}created successfully!{Style.RESET_ALL}")
            
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
    input("\nPress ENTER to go back to the menu")
