#!/usr/bin/python3
# *-* coding: utf-8 *-*

"""
create_new_job: create new cockpit job script
"""

import os
import sys
import datetime
import yaml
from colorama import Fore, Style
from modules.classes import MySQLite
from modules.cp_print import pretty_print
from modules.cp_utils import generate_job_file
from modules.cp_inputs import (
    press_any_key,
    validate_option_select
)

# load config yaml
CONFIG_YAML = f"{os.environ['COCKPIT_HOME']}/config/config.yaml"
with open(CONFIG_YAML, 'r', encoding="utf-8") as yml:
    data = yaml.safe_load(yml)

COCKPIT_DB_HOME = data['params']['cockpit']['db_home']
COCKPIT_DB_NAME = data['params']['cockpit']['db_name']
COCKPIT_DB = f"{COCKPIT_DB_HOME}/{COCKPIT_DB_NAME}"

# instantiate db object
sqlitedb = MySQLite(COCKPIT_DB)

i = 1
# build environment dictionary
environments = {}
for key in data['params'].keys():
    if key != 'cockpit':
        environments[i] = [f'{key}'.upper(), data['params'][key]['variables']['pivot']]
        i += 1

# build space types dictionary
rows = sqlitedb.select("SELECT name FROM types;")
space_types = {}
if len(rows) > 0:
    i = 1
    for t in rows:
        space_types[i] = [t[0]]
        i += 1

# introduction
intro = [
    "The job defines an execution of a single specific function.",
    "One or more jobs may be created in order to acheive a wider goal.",
    "Jobs are executed only via association with tasks."
    ]
for line in intro:
    pretty_print(line, 'LIGHTBLUE_EX')

# choice env
TITLE = "\nWhich environments would you like to validate?"
choices = validate_option_select(environments, TITLE)
if choices is not None:
    envs = {}
    for choice in choices:
        envs[int(choice)] = [environments[int(choice)][0], environments[int(choice)][1]]
else:
    sys.exit()

# choice type
TITLE = "\n\nWhich type(s) would you like to validate?"
choices = validate_option_select(space_types, TITLE)
if choices is not None:
    types = {}
    for choice in choices:
        types[int(choice)] = [space_types[int(choice)][0]]
else:
    sys.exit()

print('\n\n')
for e in envs.values():
    the_env = e[0]
    for t in types.values():
        obj_type = t[0]
        J_MD = "counter"
        j_name = f"{J_MD}_{the_env}_{obj_type}"
        j_content = obj_type
        j_command = f"{J_MD}_{the_env}_{obj_type}.py".lower()
        j_dest = the_env.lower()
        j_creation_time = datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        job = (j_name, J_MD, j_content, j_command, j_dest, j_creation_time)
        # if job doesn't exist we create it
        if len(sqlitedb.select(f"SELECT name FROM jobs WHERE name = '{j_name}';")) > 0:
            print(f"Job {j_name} already exists. {Fore.RED}creation aborted!{Style.RESET_ALL}")
        else:
            generate_job_file(J_MD, the_env, obj_type, data)
            SQL = """ INSERT INTO jobs(name,metadata,content,command,destination,created)
              VALUES(?,?,?,?,?,?) """
            r = sqlitedb.insert(SQL, job)
            print(f"Job {j_name} {Fore.GREEN}created successfully!{Style.RESET_ALL}")
press_any_key()
