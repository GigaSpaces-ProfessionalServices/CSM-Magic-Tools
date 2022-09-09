#!/usr/bin/python3
# *-* coding: utf-8 *-*

import os
import yaml
import datetime
from signal import SIGINT, signal
from functions import handler, create_connection, \
    list_tasks_grouped, parse_multi_select, \
        register_policy, sort_tuples_list


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
note = "(!) Collections are supported (i.e: 1,3,2-5)"
print(f"{note}\n" + '='*len(note))
conn = create_connection(cockpit_db)
# get the task(s) to be associated with the policy
tasks = sort_tuples_list(list_tasks_grouped(conn, 'id', 'uid', 'type'))
print(tasks)
if len(tasks) > 0:
    index = 1
    for task in tasks:
        i = f"{[index]}"
        print(f'{i:<4} - {task[2]} (uid: {task[1]}) ***more info required - TBD')
        index += 1
    print(f'{"[99]":<4} - {"ESC":<24}')
    selected_tasks = []
    for i in parse_multi_select(tasks):
        selected_tasks.append(i-1)
    if selected_tasks[0] != -1:
        # sched selection #### [ TBD ] ####
        p_sched = 1 # one minute
        p_repeat_times = 3
        p_metadata = 'NULL'
        p_content = 'NULL'
        p_state = 'NULL'
        p_created = datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        for t in selected_tasks:
            task_uid = tasks[t][1]
            task_type = tasks[t][2]
            policy_data = (p_sched,p_repeat_times,task_uid,p_metadata,p_content,p_state,p_created)
            r = register_policy(conn, policy_data)
            print(f"Policy id {r} (for task: {task_uid} ; type: {task_type}) registered successfully.")
else:
    print("There are no tasks registered yet. Cannot register policies without tasks.")

input("\nPress ENTER to go back to the menu")
