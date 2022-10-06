#!/usr/bin/python3
# *-* coding: utf-8 *-*

import os
from secrets import choice
import yaml
import datetime
import subprocess
import uuid
from signal import signal, SIGINT
from colorama import Fore, Style
from classes import MySQLite
from functions import (
    get_user_ok,
    press_any_key, 
    validate_option_select, 
    pretty_print, 
    sort_tuples_list, 
    create_file,  
    get_keypress
    )

def deploy_systemd_components():
    # generate policy manager script from template
    if not os.path.exists(f"{policies_home}/{policy_manager_script}"):
        template = f"{templates_dir}/policy_manager_template.py"
        copy_template = f"cp {template} {policies_home}/{policy_manager_script}"
        try:
            subprocess.run([copy_template], shell=True, check=True)
        except subprocess.CalledProcessError as e:
            print(e.output)
        else:
            try:    # set execution bit for policy manager script
                subprocess.run([f"chmod +x {policies_home}/{policy_manager_script}"], shell=True, check=True)
            except subprocess.CalledProcessError as e:
                print(e.output)
            print(f"policy manager '{policy_manager_script}' created successfully")
    else:
        print(f"policy manager already exists. skipping")
    
    # create systemd policy.service
    lines = [
        '[Unit]',
        f'Description={policy_desc}',
        f'Wants={suffix}_{policy_name}.timer\n',
        '[Service]',
        f'Environment="COCKPIT_HOME={os.environ["COCKPIT_HOME"]}"',
        'Type=oneshot',
        f'ExecStart={os.path.realpath(policies_home)}/{policy_manager_script}\n',
        '[Install]',
        'WantedBy=multi-user.target\n'
        ]
    create_file(lines, policy_service)
    
    # create systemd policy.timer    
    lines = [
        '[Unit]',
        f'Description={policy_desc}',
        f'Requires={suffix}_{policy_name}.service\n',
        '[Timer]',
        f'Unit={suffix}_{policy_name}',
        'AccuracySec=1us',
        f'OnUnitActiveSec={schedule}\n',
        '[Install]',
        'WantedBy=timers.target\n'
        ]
    create_file(lines, policy_timer)
    
    # reload daemons
    subprocess.run(["systemctl daemon-reload"], shell=True)


# main
config_yaml = f"{os.environ['COCKPIT_HOME']}/config/config.yaml"
templates_dir = f"{os.environ['COCKPIT_HOME']}/templates"
policies_home = f"{os.environ['COCKPIT_HOME']}/policies"
policies_workers_home = f"{policies_home}/workers"
p_retry_default = 3

# load config yaml
with open(config_yaml, 'r') as yf:
    data = yaml.safe_load(yf)

# db from yaml
cockpit_db_home = data['params']['cockpit']['db_home']
cockpit_db_name = data['params']['cockpit']['db_name']
cockpit_db = f"{cockpit_db_home}/{cockpit_db_name}"

# instantiate db object
sqlitedb = MySQLite(cockpit_db)

# introduction
intro = [
    "A policy is defined by two types of files:",
    " - Policy manager: activated by systemd services and is unique for each schedule defined.",
    " - Policy worker: is the element created for each task(s) associated with a specific schedule.",
    "(!) A single manager can run multiple workers"
    ]
for line in intro: pretty_print(line, 'LIGHTBLUE_EX')

# get task(s) from database
sql = f"SELECT id, uid, type FROM tasks GROUP BY uid"
tasks = sort_tuples_list(sqlitedb.select(sql))
if len(tasks) == 0:
    pretty_print("\nERROR: tasks must be created first to add new policies!", 'red')
    press_any_key()
    exit()

# create policies home if not exist
if not os.path.exists(policies_home):
    try:
        # we create exec folder and its tree
        os.makedirs(f"{policies_workers_home}")
    except OSError as e:
        print(e)

# get policy schedule
schedule_exists = False
title = "Enter a schedule (in seconds) for this policy"
note = '* Example: 80 means "run every 1m:20s"'
print(f"\n{title}\n{note}\n" + '-'*len(title))
try:
    while True:
        schedule = get_keypress()
        if schedule == 'esc':
            quit()
        if not schedule.isdigit():
            pretty_print('ERROR: Input must be a number!', 'red')
            continue
        else:
            schedule = int(schedule)
            break
except (KeyboardInterrupt, SystemExit):
    os.system('stty sane')
    quit()
sched_sec = schedule % 60
sched_min = int(schedule / 60)
sql = f"SELECT schedule_sec FROM policies WHERE schedule_sec = '{schedule}' GROUP BY schedule_sec;"
if len(sqlitedb.select(sql)) > 0:
    schedule_exists = True
    pretty_print("(!) a policy already exists for this schedule. only workers will be added!", 'LIGHTYELLOW_EX')
print('\n')

# get policy name
if not schedule_exists:
    p_name_len = 30
    title = f"Enter a name for this policy or press ENTER to auto-generate"
    note = f"(!) max {p_name_len} characters allowed"
    print(f"\n{title}\n{note}\n" + '-'*len(title))
    name_ok = False
    try:
        while not name_ok:
            p_name = get_keypress()
            if p_name == 'esc':
                quit()
            if len(p_name) <= 30:
                name_ok = True
                if len(p_name) == 0:
                    p_name = ""
            else:
                pretty_print(f"(!) policy name cannot exceed {p_name_len} characters", 'red')
            if p_name == "":
                policy_name = f"policy_{schedule}"
                auto_gen_name = True
            else:
                policy_name = p_name
                auto_gen_name = False
    except (KeyboardInterrupt, SystemExit):
        os.system('stty sane')
        quit()
    if auto_gen_name:
        print("* auto generated\n\n")
else:
    policy_name = f"policy_{schedule}"
    auto_gen_name = True

# get task(s) selection
selected_tasks = []
index = 1
tasks_dict = {}
tasks_menu_dict = {}
for task in tasks:
    task_id = task[0]
    task_uid = task[1]
    task_type = task[2]
    if task_type.lower() == 'Validation'.lower():
        # we extract the target object type from the task
        sql = f""" SELECT j.name, j.id, j.destination, j.metadata, j.content 
              FROM tasks t INNER JOIN jobs j 
              ON j.id = t.job_id 
              WHERE t.uid = '{task_uid}'; """
        task_obj = ''.join(set([job[4] for job in sqlitedb.select(sql)]))
        # conform tasks dictionary to validation func
        tasks_menu_dict[task_id] = [f"{task_type} of object type '{task_obj}'", task_uid]
        tasks_dict[task_id] = [task_type, task_uid]
    else:
        print("TBD - add procedures for tasks other than validation...")
title = "Choose tasks to be associated with this policy"
selected_tasks = validate_option_select(tasks_menu_dict, title)
if selected_tasks == None: quit()
if -1 in selected_tasks:
    pretty_print("\n(!) no tasks have been selected. cannot continue!", 'red')
    press_any_key()
    exit()

# confirm policy parameters
pretty_print("\n\n(!) Please confirm the following policy parameters:", 'yellow')
if schedule_exists:
    print(f"   {'Name:':<18}{policy_name} - already exists!")
else:
    if auto_gen_name: print(f"   {'Name:':<18}{policy_name} (name auto generated)")
    else: print(f"   {'Name:':<18}{policy_name}")
print(f"   {'Start every:':<18}{str(sched_min)}m:{str(sched_sec)}s")
print(f"   {'Retry:':<18}{str(p_retry_default)} times (* default)")
print(f"   {'Associated Tasks:':<18}")
for t in selected_tasks:
    task_type = tasks_dict[t][0]
    task_uid = tasks_dict[t][1]
    print(f"{' ':<6}Id: {t}, type: {task_type}, uid: {task_uid:<38}")

if get_user_ok("\nContinue with policy registration?"):
    suffix = 'cockpit'
    systemd_home = "/etc/systemd/system"
    policy_desc = f"{suffix}-{policy_name}"
    policy_manager_script = f"{suffix}_{policy_name}.py"
    policy_service = f"{systemd_home}/{suffix}_{policy_name}.service"
    policy_timer = f"{systemd_home}/{suffix}_{policy_name}.timer"
    print("\n")
    
    # if the schedule is new we create the policy manager and services
    if not schedule_exists: deploy_systemd_components()
    
    # register and generate policy worker(s)
    for task_id in selected_tasks:
        task_type = tasks_dict[task_id][0]
        task_uid = tasks_dict[task_id][1]
        p_uid = str(uuid.uuid4())
        policy_worker_script = f"{p_uid}.py"
        p_md = 'NULL'
        p_cont = 'NULL'
        p_created = datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        p_active = 'yes'
        
        # register worker in database
        sql = """ INSERT INTO policies(uid, name, schedule_sec, retry, task_id, task_uid, metadata, content, active_state, created) 
        VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?) """
        policy_data = (p_uid, policy_name, schedule, p_retry_default, task_id, task_uid, p_md, p_cont, p_active, p_created)
        r = sqlitedb.insert(sql, policy_data)
        
        # generate policy worker script from template
        if not os.path.exists(f"{policies_workers_home}/{policy_worker_script}"):
            template = f"{templates_dir}/policy_worker_template.py"
            copy_template = f"cp {template} {policies_workers_home}/{policy_worker_script}"
            try:
                subprocess.run([copy_template], shell=True, check=True)
            except subprocess.CalledProcessError as e:
                print(e.output)
            else:
                try:    # set execution bit for policy exec script
                    subprocess.run([f"chmod +x {policies_workers_home}/{policy_worker_script}"], shell=True, check=True)
                except subprocess.CalledProcessError as e:
                    print(e.output)
                finally:    # print summary
                    print(f"policy worker '{p_uid}' created successfully")
        else:
            print(f"policy worker script already exists. creation aborted")
        
        
        # if new policy manager we enable timer
        if not schedule_exists:
            sql = f"SELECT active_state FROM policies where name = '{policy_name}' GROUP BY name;"
            rows = sqlitedb.select(sql)
            if rows[0][0] == 'yes':
                try:
                    cmd = f'systemctl enable --now {policy_timer}'.split(' ')
                    r = subprocess.run(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=True)
                    if r.returncode == 0:
                        print(f"policy '{policy_name}' {Fore.GREEN}enabled!{Style.RESET_ALL}")
                    else:
                        print(f"policy '{policy_name}' {Fore.RED}could not be enabled{Style.RESET_ALL}")
                except subprocess.CalledProcessError as e:
                    print(e.output)
        

press_any_key()
