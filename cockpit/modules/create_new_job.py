#!/usr/bin/python3
# *-* coding: utf-8 *-*


import os
import yaml
from influxdb import InfluxDBClient
import datetime
from colorama import Fore, Style
from classes import MySQLite
from functions import (
    pretty_print,
    generate_job_file,
    press_any_key, 
    validate_option_select
    )

# main
config_yaml = f"{os.environ['COCKPIT_HOME']}/config/config.yaml"

# load config yaml
with open(config_yaml, 'r') as yf:
    data = yaml.safe_load(yf)
cockpit_db_home = data['params']['cockpit']['db_home']
cockpit_db_name = data['params']['cockpit']['db_name']
cockpit_db = f"{cockpit_db_home}/{cockpit_db_name}"

# instantiate db object
sqlitedb = MySQLite(cockpit_db)

index = 1
# build environment dictionary
environments = {}
for k, v in data['params'].items():
    if k != 'cockpit':
        environments[index] = [f'{k}'.upper(), data['params'][k]['variables']['pivot']]
        index += 1
# build space types dictionary
sql = "SELECT name FROM types;"
rows = sqlitedb.select(sql)

space_types = {}
if len(rows) > 0:
    index = 1
    for t in rows:
        space_types[index] = [t[0]]
        index += 1

# introduction
intro = [
    "The job defines an execution of a single specific function.",
    "One or more jobs may be created in order to acheive a wider goal.",
    "Jobs are executed only via association with tasks."
    ]
for line in intro: pretty_print(line, 'LIGHTBLUE_EX')

# choice env
title = f"\nWhich environments would you like to validate?"
choices = validate_option_select(environments, title)
if choices != None:
    envs = {}
    for choice in choices:
        envs[int(choice)] = [environments[int(choice)][0], environments[int(choice)][1]]
else: quit()

# choice type
title = f"\n\nWhich type(s) would you like to validate?"
choices = validate_option_select(space_types, title)
if choices != None:
    types = {}
    for choice in choices:
        types[int(choice)] = [space_types[int(choice)][0]]
else: quit()

print('\n\n')
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
        # if job doesn't exist we create it
        sql = f"SELECT name FROM jobs WHERE name = '{j_name}';"
        if len(sqlitedb.select(sql)) > 0:
            print(f"Job {j_name} already exists. {Fore.RED}creation aborted!{Style.RESET_ALL}")
        else:
            generate_job_file(j_metadata, the_env, obj_type, data)
            sql = """ INSERT INTO jobs(name,metadata,content,command,destination,created)
              VALUES(?,?,?,?,?,?) """
            r = sqlitedb.insert(sql, job)
            print(f"Job {j_name} {Fore.GREEN}created successfully!{Style.RESET_ALL}")
press_any_key()
