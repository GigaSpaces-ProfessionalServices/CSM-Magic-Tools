#!/usr/bin/python3
# *-* coding: utf-8 *-*


import os
from posixpath import split
import yaml
import sqlite3
import uuid
from sqlite3 import Error
import datetime
from signal import SIGINT, signal
from functions import handler, create_connection, get_type_selection, list_registered_tasks


def validate_input(items_dict):
    from colorama import Fore
    choice = input("\nEnter your choice: ")
    while True:
        if choice == '99':
            return -1
        if len(items_dict) > 1:
            if choice == str(len(items_dict) + 1): # if 'ALL' is selected
                return "ALL"
        if not choice.isdigit() or int(choice) not in items_dict.keys():
            choice = input(f"{Fore.RED}ERROR: Input must be a menu index!{Fore.RESET}\nEnter you choice: ")
        else:
            return int(choice)


def parse_jobs_selections(jobs):
    from colorama import Fore
    choice = input("\nEnter your choice: ")
    
    # check if choice in range
    def choice_ok(value, limit):
        if not value.isdigit() or int(value) < 1 or int(value) > limit:
            return False
        return True
        
    while True:
        valid_selections = []
        if choice == '99':
            valid_selections.append(-1)
            return valid_selections
        else:
            selected_ok = False
            selected = choice.split(',')
            for c in selected:
                if '-' in c:
                    range_select = c.split('-')
                    if len(range_select) != 2:
                        selected_ok = False
                        break
                    for i in range(int(range_select[0]), int(range_select[1])+1):
                        if choice_ok(str(i), len(jobs)):
                            selected_ok = True
                            valid_selections.append(i)
                        else:
                            selected_ok = False
                            break
                elif choice_ok(c, len(jobs)):
                    selected_ok = True
                    valid_selections.append(int(c))
                else:
                    selected_ok = False
                    break
            if selected_ok:
                return list(set(valid_selections))
            else:
                choice = input(f"{Fore.RED}ERROR: Invalid input!{Fore.RESET}\nEnter you choice: ")


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
result = get_type_selection(data['types'])
if result == -1:
    input("\nPress ENTER to go back to the menu")
else:
    t_type = data['types'][result]
    t_type_sn = result
    t_metadata = "NULL"
    t_content = "NULL"
    t_state = "NULL"
    t_created = datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')
    # list jobs from db
    print("\nChoose jobs to associate with this task.")
    note = "* Multiple selection available (i.e: 1,3,4) and range (i.e: 2-4)"
    print(f"{note}\n" + '='*len(note))
    conn = create_connection(cockpit_db)
    jobs = list_registered_jobs(conn)
    if len(jobs) > 0:
        for j in jobs:
            index = f"[{j[0]}]"
            print(f'{index:<4} - {j[1]:<24}')
        print(f'{"[99]":<4} - {"Skip (can be selected later from the Edit Tasks menu)":<24}')
        selected_jobs = parse_jobs_selections(jobs)
        if selected_jobs[0] == -1:
            task_data = (t_uid,t_type,t_type_sn,'NULL',t_metadata,t_content,t_state,t_created)
            r = register_task(conn, task_data)
            print(f"Task id {r} (type = {t_type} ; uid = {t_uid}) registered successfully.")
        else:
            for job_id in selected_jobs:
                task_data = (t_uid,t_type,t_type_sn,job_id,t_metadata,t_content,t_state,t_created)
                r = register_task(conn, task_data)
                print(f"Task id {r} (type = {t_type} ; uid = {t_uid}) registered successfully.")
    else:
        print("There are no jobs registered yet\n* can be selected later from the Edit Tasks menu")
        task_data = (t_uid,t_type,t_type_sn,'NULL',t_metadata,t_content,t_state,t_created)
        r = register_task(conn, task_data)
        print(f"Task id {r} (type = {t_type} ; uid = {t_uid}) registered successfully.")

input("\nPress ENTER to go back to the menu")
