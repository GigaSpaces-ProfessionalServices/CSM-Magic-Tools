#!/usr/bin/python3
# *-* coding: utf-8 *-*

import os
import yaml
import uuid
import datetime
from signal import SIGINT, signal
from functions import handler, create_connection, \
    parse_jobs_selections, get_type_selection, \
        list_jobs, register_task

# main
config_yaml = f"{os.path.dirname(os.path.abspath(__file__))}/../config/config.yaml"

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
    t_type = data['tasks'][result]
    t_type_sn = result
    t_metadata = "NULL"
    t_content = "NULL"
    t_state = "NULL"
    t_created = datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')
    # list jobs from db
    print("\nChoose jobs to associate with this task.")
    note = "(!) Collections are supported (i.e: 1,3,2-5)"
    print(f"{note}\n" + '='*len(note))
    conn = create_connection(cockpit_db)
    jobs = list_jobs(conn)
    if len(jobs) > 0:
        for j in jobs:
            index = f"[{j[0]}]"
            print(f'{index:<4} - {j[1]:<24}')
        print(f'{"[99]":<4} - {"Skip (can be selected later from the Edit Tasks menu)":<24}')
        selected_jobs = parse_jobs_selections(jobs) ##### SHOULD CHANGE TO GENERIC PARSE_MULTI_SELECT INSTEAD
        if selected_jobs[0] == -1:
            task_data = (t_uid,t_type,t_type_sn,'NULL',t_metadata,t_content,t_state,t_created)
            r = register_task(conn, task_data)
            print(f"Task id {r} (type: {t_type} ; job_id: None) registered successfully.")
        else:
            for job_id in selected_jobs:
                task_data = (t_uid,t_type,t_type_sn,job_id,t_metadata,t_content,t_state,t_created)
                r = register_task(conn, task_data)
                print(f"Task id {r} (type: {t_type} ; job_id: {job_id}) registered successfully.")
    else:
        print("There are no jobs registered yet\n* can be selected later from the Edit Tasks menu")
        task_data = (t_uid,t_type,t_type_sn,'NULL',t_metadata,t_content,t_state,t_created)
        r = register_task(conn, task_data)
        print(f"Task id {r} (type: {t_type} ; job_id: None) registered successfully.")
    input("\nPress ENTER to go back to the menu")
