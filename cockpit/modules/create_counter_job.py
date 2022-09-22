#!/usr/bin/python3
# *-* coding: utf-8 *-*


import os
import yaml
from influxdb import InfluxDBClient
import datetime
from signal import SIGINT, signal
from colorama import Fore, Style
from functions import handler, get_object_types_from_db, \
    create_connection, jobs_exist, generate_job_file, \
        register_job, validate_input, get_selection


# main
config_yaml = f"{os.environ['COCKPIT_HOME']}/config/config.yaml"
#config_yaml = f"{os.path.dirname(os.path.realpath(__file__))}/../config/config.yaml"

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
q = f"Which environments would you like to validate?"
choice = get_selection(environments, 'Environments', q)
envs = {}
if choice == 'ALL':
    envs = environments
else:
    envs = {1: [environments[int(choice)][0], environments[int(choice)][1]]}
# choice type
q = f"Which type(s) would you like to validate?"
choice = get_selection(space_types, 'Types', q)
types = {}
if choice == 'ALL':
    types = space_types
else:
    types = {1: [space_types[int(choice)][0]]}
print()
for e in envs.values():
    the_env = e[0]
    for t in types.values():
        obj_type = t[0]
        j_metadata = "counter"
        j_name = f"{j_metadata}_{the_env}_{obj_type}"
        j_content = obj_type
        j_command = f"{j_metadata}_{the_env}_{obj_type}.py".lower()
        j_dest = the_env.lower()
        j_creation_time = datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        job = (j_name, j_metadata, j_content, j_command, j_dest, j_creation_time)
        if jobs_exist(conn, j_name):
            print(f"Job {j_name} already exists. {Fore.RED}creation aborted!{Style.RESET_ALL}")
        else:
            generate_job_file(j_metadata, the_env, obj_type, data)
            r = register_job(conn, job)
            print(f"Job {j_name} {Fore.GREEN}created successfully!{Style.RESET_ALL}")
input("\nPress ENTER to continue to the main menu.")
