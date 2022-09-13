#!/usr/bin/python3
# *-* coding: utf-8 *-*

### DEPLOY MSSQL FEEDER ###
import os
import yaml
from signal import SIGINT, signal
import subprocess
import json
from functions import create_connection, handler, \
    check_connection


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
    q = f"What {description} would you like to deploy the feeder on?"
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

# generate environments dictionary
index = 1
environments = {}
for k, v in data['params'].items():
    if k != 'cockpit':
        environments[index] = [f'{k}'.upper(), data['params'][k]['variables']['pivot']]
        index += 1

# choice env
choice = get_selection(environments, 'environments')
envs = {}
if choice == 'ALL':
    envs = environments
else:
    envs = {1: [environments[int(choice)][0], environments[int(choice)][1]]}

print(envs)
exit()


types = []
connections_ok = []
for env_name in yaml_data['params']:
    if env_name != 'cockpit':
        pivot = yaml_data['params'][env_name]['endpoints']['pivot']
        exec_script = f"{os.path.dirname(os.path.realpath(__file__))}/get_space_objects.py"
        if check_connection(pivot, 22):
            connections_ok.append(True)
            cmd = f"cat {exec_script} | ssh {pivot} python3 -"
            response = subprocess.run([cmd], shell=True, stdout=subprocess.PIPE).stdout.decode()
            response = json.loads(response.replace("\'", "\""))
            for k in response.keys():
                if k != 'java.lang.Object':
                    types.append(k)
if True in connections_ok:
    k = 1
    object_types = {}
    for the_type in set(types):
        v = [ the_type, response[the_type]['entries']]
        object_types[k] = v
        k += 1

for e in envs.values():
    the_env = e[0]
    for t in types.values():
        the_type = t[0]
        conn = create_connection(cockpit_db)
        job_name = f"validation_{the_env}_{the_type}"
        job_metadata = ""
        job_content = ""
        job_command = f"validation_{the_env}_{the_type}.py".lower()
        job_dest = the_env
        job_creation_timestamp = datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        job = (job_name, job_metadata, job_content, job_command, job_dest, job_creation_timestamp)
        if jobs_exist(conn, job_name):
            print(f"Job: {job_name} already exists.")
        else:
            generate_job_file(the_env, the_type, data)
            r = register_job(conn, job)
            print(f"Job: {job_name} with Id: {r} created successfully")
input("\nPress ENTER to continue to the main menu.")

input("\nPress ENTER to go back to the main menu")