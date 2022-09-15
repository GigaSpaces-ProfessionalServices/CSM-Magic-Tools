#!/usr/bin/python3
# *-* coding: utf-8 *-*

import os
import yaml
import json
from signal import SIGINT, signal
import subprocess
from functions import handler, create_connection, \
    list_tasks_by_policy_schedule, policy_schedule_exists, \
        list_jobs_by_task_uid, write_to_influx

### summary ###
#
# 1. go to related policy and get the tasks associated with it
# 2. for each task in the list run the associated jobs
# 3. report and log
#
#

# main
config_yaml = f"{os.path.dirname(os.path.realpath(__file__))}/../config/config.yaml"
jobs_home = f"{os.path.dirname(os.path.realpath(__file__))}/../jobs"


# SAMPLE FOR POLICY EXEC 
sched = 60


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
            job_dest_env= job[2]
            job_type = job[3]
            script = f'{jobs_home}/{job_file}'
            if job_type == 'counter':
                job_obj_type = job[4]
                response = subprocess.run([script], shell=True, stdout=subprocess.PIPE).stdout.decode()
                # converting string to dictionary
                response = json.loads(response.replace("\'", "\""))
                for k,v in response.items():
                    if k == job_obj_type:
                        influx_data = {'env': job_dest_env, 'type': job_obj_type, 'count': v['entries']}
            write_to_influx('mydb', influx_data)
else:
    print(f"ERROR: policy schedule {sched} does not exist!")
input("\nPress ENTER to continue to the main menu.")
