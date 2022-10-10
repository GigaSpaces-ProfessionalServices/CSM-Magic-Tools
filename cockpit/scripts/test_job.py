#!/usr/bin/python3
# *-* coding: utf-8 *-*

"""
test_job: interactively test a job
"""

import os
import sys
import json
import subprocess
import sys
import yaml
from modules.classes import MySQLite
from modules.cp_print import pretty_print
from modules.cp_utils import check_connection
from modules.cp_inputs import (
    press_any_key,
    validate_option_select
)

JOBS_HOME = f"{os.environ['COCKPIT_HOME']}/jobs"

# load config yaml
CONFIG_YAML = f"{os.environ['COCKPIT_HOME']}/config/config.yaml"
with open(CONFIG_YAML, 'r', encoding="utf-8") as yf:
    data = yaml.safe_load(yf)

COCKPIT_DB_HOME = data['params']['cockpit']['db_home']
COCKPIT_DB_NAME = data['params']['cockpit']['db_name']
COCKPIT_DB = f"{COCKPIT_DB_HOME}/{COCKPIT_DB_NAME}"

# instantiate db object
sqlitedb = MySQLite(COCKPIT_DB)

jobs = dict(sqlitedb.select("SELECT id, name FROM jobs"))
if len(jobs) > 0:
    # conform jobs to dictionary for validation func
    jobs_dict = {}
    for k, v in jobs.items():
        jobs_dict[k] = [v]
    choices = validate_option_select(jobs_dict, "Which jobs would you like to test?")
    if choices is None:
        sys.exit()
    if choices[0] != -1:
        for choice in choices:
            print("[Testing Job]")
            print(f"{'   Job Name:':<16} {jobs[choice]}")
            job_file = f"{jobs[choice]}.py".lower()
            script = f'{JOBS_HOME}/{job_file}'
            env_name = jobs[choice].split('_')[1]
            pivot = data['params'][env_name.lower()]['endpoints']['pivot']
            PORT = 22
            if check_connection(pivot, PORT):
                response = subprocess.run(
                    [script],
                    shell=True, check=True,
                    stdout=subprocess.PIPE
                    ).stdout.decode()
                # converting string to dictionary
                response = json.loads(response.replace("\'", "\""))
                for k, v in response.items():
                    if k != 'java.lang.Object':
                        print(f"{'   Object type:':<16} {k}")
                        print(f"{'   # of entries:':<16} {v['entries']}")
                        print('\n')
            else:
                pretty_print(f"ERROR: connection to {env_name} pivot ({pivot}:{PORT}) \
                    could not be established", 'red')
                print()
else:
    print("No jobs found")

press_any_key()
