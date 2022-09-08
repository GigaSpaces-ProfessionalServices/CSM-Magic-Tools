#!/usr/bin/python3
# *-* coding: utf-8 *-*


import os
import yaml
import json
import sqlite3
from sqlite3 import Error
from signal import SIGINT, signal
import subprocess
from modules.functions import handler, create_connection, list_registered_job_names


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


# main
config_yaml = f"{os.path.dirname(os.path.abspath(__file__))}/../config/config.yaml"
jobs_home = f"{os.path.dirname(os.path.abspath(__file__))}/../jobs"

# catch user CTRL+C key press
signal(SIGINT, handler)

# load config yaml
with open(config_yaml, 'r') as yf:
    data = yaml.safe_load(yf)
cockpit_db_home = data['params']['cockpit']['db_home']
cockpit_db_name = data['params']['cockpit']['db_name']
cockpit_db = f"{cockpit_db_home}/{cockpit_db_name}"
conn = create_connection(cockpit_db)
jobs = dict(list_registered_job_names(conn))
if len(jobs) > 0:
    print("Which job would you like to test?")
    w = 41
    print("-"*w + f'\n| {"Id":^4} | {"Name":^30} |\n' + "-"*w)
    for job_id, job_name in jobs.items():
        print(f'| {job_id:<4} | {job_name:<30} |')
    print("-"*w + f'\n| {99:<4} | {"ESC":<30} |\n' + "-"*w)
    choice = validate_input(jobs)
else:
    print("No jobs found")
    input("\nPress ENTER to go back to the main menu")
if choice != -1:
    print(f"Testing Job: '{jobs[choice]}'")
    job_file = f"{jobs[choice]}.py".lower()
    script = f'{jobs_home}/{job_file}'
    response = subprocess.run([script], shell=True, stdout=subprocess.PIPE).stdout.decode()
    # converting string to dictionary
    response = json.loads(response.replace("\'", "\""))
    print("[TEST JOB RESULT]")
    for k,v in response.items():
        if k != 'java.lang.Object':
            print(f"{'Object type:':<14} {k}")
            print(f"{'# of entries:':<14} {v['entries']}")
    
    input("\nPress ENTER to go back to the main menu")
