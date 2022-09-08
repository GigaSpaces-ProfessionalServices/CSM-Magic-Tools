#!/usr/bin/python3
# *-* coding: utf-8 *-*

import os
import yaml
import datetime
from signal import SIGINT, signal
from functions import handler, create_connection, \
    list_tasks_grouped, parse_multi_select, register_policy

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
print("\nChoose tasks to associate with this policy")
note = "* Multiple selection available (i.e: 1,3,4) and range (i.e: 2-4)"
print(f"{note}\n" + '='*len(note))
conn = create_connection(cockpit_db)
# get the task(s) from user to associate with the policy
tasks = list_tasks_grouped(conn, 'id', 'uid', 'type')
if len(tasks) > 0:
    for task in tasks:
        index = f"[{task[0]}]"
        print(f'{index:<4} - {task[2]} (uid: {task[1]})')
    print(f'{"[99]":<4} - {"Skip (can be selected later from the Edit Tasks menu)":<24}')
    selected_tasks = parse_multi_select(tasks)
    if selected_tasks[0] != -1:
        p_created = datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        for task_id in selected_tasks:
            policy_data = (t_uid,t_type,t_type_sn,job_id,t_metadata,t_content,t_state,t_created)
            r = register_policy(conn, policy_data)
            print(f"Policy id {r} (type: {t_type} ; job_id: {job_id}) registered successfully.")

    input("\nPress ENTER to go back to the menu")
else:
    print("There are no tasks registered yet. Cannot register policies without tasks.")

input("\nPress ENTER to go back to the menu")
