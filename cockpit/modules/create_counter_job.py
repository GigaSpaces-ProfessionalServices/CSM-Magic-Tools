#!/usr/bin/python3
# *-* coding: utf-8 *-*


import os
import yaml
from influxdb import InfluxDBClient
import datetime
from signal import SIGINT, signal
from functions import handler, get_object_types_from_db, \
    create_connection, jobs_exist, \
        generate_job_file, register_job


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


def get_selection(the_dict, description):
    # print menu
    q = f"Which {description} would you like to validate?"
    print(q + "\n" + '=' * len(q))
    for k, v in the_dict.items():
        index = f"[{k}]"
        print(f'{index:<4} - {v[0]:<24}')
    if len(the_dict) > 1:
        index = f"[{k+1}]"
        item = "All " + description.capitalize()
        print(f'{index:<4} - {item:<24}')
    print(f'{"[99]":<4} - {"ESC":<24}')
    result = validate_input(the_dict)
    if result != -1:
        return result


# main
config_yaml = f"{os.path.dirname(os.path.realpath(__file__))}/../config/config.yaml"

# catch user CTRL+C key press
signal(SIGINT, handler)

# load config yaml
with open(config_yaml, 'r') as yf:
    data = yaml.safe_load(yf)
cockpit_db_home = data['params']['cockpit']['db_home']
cockpit_db_name = data['params']['cockpit']['db_name']
cockpit_db = f"{cockpit_db_home}/{cockpit_db_name}"
conn = create_connection(cockpit_db)
index = 1
environments = {}
for k, v in data['params'].items():
    if k != 'cockpit':
        environments[index] = [f'{k}'.upper(), data['params'][k]['variables']['pivot']]
        index += 1
space_types = get_object_types_from_db(conn)
# choice env
choice = get_selection(environments, 'environments')
envs = {}
if choice == 'ALL':
    envs = environments
else:
    envs = {1: [environments[int(choice)][0], environments[int(choice)][1]]}
# choice type
choice = get_selection(space_types, 'types')
types = {}
if choice == 'ALL':
    types = space_types
else:
    types = {1: [space_types[int(choice)][0]]}
for e in envs.values():
    the_env = e[0]
    for t in types.values():
        the_type = t[0]
        j_metadata = "counter"
        j_name = f"{j_metadata}_{the_env}_{the_type}"
        j_content = the_type
        j_command = f"{j_metadata}_{the_env}_{the_type}.py".lower()
        j_dest = the_env.lower()
        j_creation_time = datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        job = (j_name, j_metadata, j_content, j_command, j_dest, j_creation_time)
        if jobs_exist(conn, j_name):
            print(f"Job: {j_name} already exists.")
        else:
            generate_job_file(the_env, the_type, data)
            r = register_job(conn, job)
            print(f"Job: {j_name} with Id: {r} created successfully")
input("\nPress ENTER to continue to the main menu.")
