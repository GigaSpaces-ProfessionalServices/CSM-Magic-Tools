#!/usr/bin/python3
# *-* coding: utf-8 *-*


import os
import yaml
import json
# import sqlite3
# from sqlite3 import Error
from signal import SIGINT, signal
import datetime
import subprocess
from influxdb import InfluxDBClient
from functions import handler, create_connection, \
    list_tasks_by_policy_schedule, policy_schedule_exists, \
        list_jobs_by_task_uid


### summary ###
#
# 1. go to related policy and get the tasks associated with it
# 2. for each task in the list run the associated jobs
# 3. report and log
#
#

# main
config_yaml = f"{os.path.dirname(os.path.abspath(__file__))}/../config/config.yaml"
jobs_home = f"{os.path.dirname(os.path.abspath(__file__))}/../jobs"


# SAMPLE FOR POLICY EXEC 
sched = 1


# catch user CTRL+C key press
signal(SIGINT, handler)

# load config yaml
with open(config_yaml, 'r') as yf:
    data = yaml.safe_load(yf)

cockpit_db_home = data['params']['cockpit']['db_home']
cockpit_db_name = data['params']['cockpit']['db_name']
cockpit_db = f"{cockpit_db_home}/{cockpit_db_name}"
conn = create_connection(cockpit_db)

if policy_schedule_exists(conn, sched):
    tasks = list_tasks_by_policy_schedule(conn, str(sched))
    for task_uid in tasks:
        print(f"\n[ Task {task_uid[0]} ]")
        for job in list_jobs_by_task_uid(conn, task_uid):
            job_file = f"{job[0]}.py".lower()
            script = f'{jobs_home}/{job_file}'
            response = subprocess.run([script], shell=True, stdout=subprocess.PIPE).stdout.decode()
            # converting string to dictionary
            response = json.loads(response.replace("\'", "\""))
            print(f"   executing job {job[0]}")
            for k,v in response.items():
               if k != 'java.lang.Object':
                   print(f"   {'Object type:':<14} {k}")
                   print(f"   {'# of entries:':<14} {v['entries']}")            
else:
    print(f"ERROR: policy schedule {sched} does not exist!")

input("\nPress ENTER to continue to the main menu.")
