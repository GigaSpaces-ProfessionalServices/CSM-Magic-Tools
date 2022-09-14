#!/usr/bin/python3
# *-* coding: utf-8 *-*

import os
import yaml
import datetime
from signal import SIGINT, signal
from colorama import Fore
from functions import handler, create_connection, \
    list_tasks_grouped, parse_multi_select, \
        register_policy, sort_tuples_list, \
            get_user_permission

# main
config_yaml = f"{os.path.dirname(os.path.realpath(__file__))}/../config/config.yaml"

# catch user CTRL+C key press
signal(SIGINT, handler)

# load config yaml
with open(config_yaml, 'r') as yf:
    data = yaml.safe_load(yf)
# db from yaml
cockpit_db_home = data['params']['cockpit']['db_home']
cockpit_db_name = data['params']['cockpit']['db_name']
cockpit_db = f"{cockpit_db_home}/{cockpit_db_name}"
note = "Choose tasks to be associated with this policy"
print(f"\n{note}")
print("(!) Collections are supported (i.e: 1,3,2-5)")
print('-' * len(note))
conn = create_connection(cockpit_db)
# get the task(s) to be associated with the policy
tasks = sort_tuples_list(list_tasks_grouped(conn, 'id', 'uid', 'type'))
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
# get the schedule for this task
note = "Choose the schedule (seconds) and retry for this policy"
print(f"\n{note}")
print('  schedule = 210 means "run every 3m:30s"')
print("  retry    = number of times to retry on failure")
print('-' * len(note))
schedule_ok = False
while not schedule_ok:
    schedule = input("Enter schedule: ")
    while True:
        if not schedule.isdigit():
            schedule = input(f"{Fore.RED}ERROR: Input must be a number!{Fore.RESET}\nEnter schedule: ")
        else:
            schedule = int(schedule)
            break
    repeat = input("Enter retry: ")
    while True:
        if not repeat.isdigit():
            repeat = input(f"{Fore.RED}ERROR: Input must be a number!{Fore.RESET}\nEnter retry: ")
        else:
            repeat = int(repeat)
            break
    # calculate wait time between retries
    wait_repeat = int(schedule / (repeat + 1))
    print("\nPolicy will be created with the following parameters:")
    print(f" {'run every: ':<20} {schedule} seconds")
    print(f" {'times to retry : ':<20} {repeat} times")
    print(f" {'wait between retries: ':<20} {wait_repeat} seconds")
    if get_user_permission("\nContinue with policy registration?"):
        break
    else:
        print("\n")

if selected_tasks[0] != -1:
    p_metadata = 'NULL'
    p_content = 'NULL'
    p_state = 'NULL'
    p_created = datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')
    for t in selected_tasks:
        task_uid = tasks[t][1]
        task_type = tasks[t][2]
        policy_data = (schedule,repeat, wait_repeat,task_uid,p_metadata,p_content,p_state,p_created)
        r = register_policy(conn, policy_data)
        print(f"Policy id {r} (for task: {task_uid} ; type: {task_type}) registered successfully.")
else:
    print("There are no tasks registered yet. Cannot register policies without tasks.")

input("\nPress ENTER to go back to the menu")
