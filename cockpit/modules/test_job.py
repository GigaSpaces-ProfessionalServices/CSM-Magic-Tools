#!/usr/bin/python3
# *-* coding: utf-8 *-*

import os
import yaml
import json
import subprocess
from classes import MySQLite
from functions import (
    press_any_key, 
    validate_option_select, 
    check_connection, 
    pretty_print
    )

# main
config_yaml = f"{os.environ['COCKPIT_HOME']}/config/config.yaml"
jobs_home = f"{os.environ['COCKPIT_HOME']}/jobs"

# load config yaml
with open(config_yaml, 'r') as yf:
    data = yaml.safe_load(yf)

cockpit_db_home = data['params']['cockpit']['db_home']
cockpit_db_name = data['params']['cockpit']['db_name']
cockpit_db = f"{cockpit_db_home}/{cockpit_db_name}"

# instantiate db object
sqlitedb = MySQLite(cockpit_db)

sql = "SELECT id, name FROM jobs"
jobs = dict(sqlitedb.select(sql))
if len(jobs) > 0:
    # conform jobs dictionary to validation func
    jobs_dict = {}
    for k, v in jobs.items():
        jobs_dict[k] = [v]
    title = "Which jobs would you like to test?"
    choices = validate_option_select(jobs_dict, title)
    if choices == None: quit()  # if KeyboardInterrupt
    if choices[0] != -1:
        for choice in choices:
            print(f"[Testing Job]")
            print(f"{'   Job Name:':<16} {jobs[choice]}")
            job_file = f"{jobs[choice]}.py".lower()
            script = f'{jobs_home}/{job_file}'
            env_name = jobs[choice].split('_')[1]
            pivot = data['params'][env_name.lower()]['endpoints']['pivot']
            port = 22
            if check_connection(pivot, port):
                response = subprocess.run([script], shell=True, stdout=subprocess.PIPE).stdout.decode()
                # converting string to dictionary
                response = json.loads(response.replace("\'", "\""))
                for k,v in response.items():
                    if k != 'java.lang.Object':
                        print(f"{'   Object type:':<16} {k}")
                        print(f"{'   # of entries:':<16} {v['entries']}")
                        print('\n')
            else:
                pretty_print(f"ERROR: connection to {env_name} pivot ({pivot}:{port}) could not be established", 'red')
else:
    print("No jobs found")
press_any_key()
