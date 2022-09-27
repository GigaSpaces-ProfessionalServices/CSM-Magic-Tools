#!/usr/bin/python3
# *-* coding: utf-8 *-*

import os
import yaml
import datetime
import subprocess
import uuid
from colorama import Fore, Style
from functions import create_connection, list_tasks_grouped, \
    parse_multi_select, pretty_print, register_policy, sort_tuples_list, \
        get_user_ok, create_file, list_jobs_by_task_uid, \
            policy_schedule_exists

# main
config_yaml = f"{os.environ['COCKPIT_HOME']}/config/config.yaml"
templates_dir = f"{os.environ['COCKPIT_HOME']}/templates"
policies_home = f"{os.environ['COCKPIT_HOME']}/policies"
policies_workers_home = f"{policies_home}/workers"

# load config yaml
with open(config_yaml, 'r') as yf:
    data = yaml.safe_load(yf)

# db from yaml
cockpit_db_home = data['params']['cockpit']['db_home']
cockpit_db_name = data['params']['cockpit']['db_name']
cockpit_db = f"{cockpit_db_home}/{cockpit_db_name}"
conn = create_connection(cockpit_db)

# introduction
intro = [
    "[ INTRO ]",
    "A policy is comprised of two elements (i.e: files and/or database records):",
    " - Policy manager: has corresponding systemd services and is unique for each schedule defined.",
    " - Policy worker: is the element created for each task(s) associated with a specific schedule.",
    " * A single manager can run multiple workers"
    ]
for line in intro: pretty_print(line, 'LIGHTBLUE_EX')

# get task(s) from database
tasks = sort_tuples_list(list_tasks_grouped(conn, 'id', 'uid', 'type'))
if len(tasks) == 0:
    pretty_print("\nERROR: tasks must be created first to add new policies!", 'red')
    input("\nPress ENTER to go back to the menu")
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
note = "Choose a schedule (in seconds) for this policy"
print(f"\n{note}\n" + '* Example: 80 means "run every 1m:20s"\n' + '-'*len(note))
while True:
    schedule = input("Enter schedule: ")
    if not schedule.isdigit():
        print(f"{Fore.RED}ERROR: Input must be a number!{Fore.RESET}")
        continue
    else:
        schedule = int(schedule)
        break
sched_sec = schedule % 60
sched_min = int(schedule / 60)
if policy_schedule_exists(conn, schedule):
    schedule_exists = True
    pretty_print("(!) a manager already exists for this schedule. only workers will be added!", 'LIGHTYELLOW_EX')

# get policy name
p_name_len = 30
note = f"Choose a name for this policy (max {p_name_len} chars) or leave blank to auto-generate"
print(f"\n{note}\n"  + '-'*len(note))
name_ok = False
while not name_ok:
    p_name = input("Enter name: ")
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

# get task(s) selection
selected_tasks = []
note = "Choose tasks to be associated with this policy"
print(f"\n{note}")
print("(!) collections are supported (i.e: 1,3,2-5)")
print('-' * len(note))
index = 1
for task in tasks:
    task_uid = task[1]
    task_type = task[2]
    if task_type == data['tasks'][1]['name']:
        # we extract the target object type from the task
        task_obj = ''.join(set([job[4] for job in list_jobs_by_task_uid(conn, (task_uid,))]))
    i = f"{[index]}"
    print(f"{i:<4} - {task_type} of object type: '{task_obj}'")
    index += 1
print(f'{"[99]":<4} - {"ESC":<24}')
for i in parse_multi_select(tasks):
    if i != -1:
        selected_tasks.append(i-1)
    else:
        pretty_print("\n(!) no tasks have been selected. cannot continue!", 'red')
        input("\nPress ENTER to go back to the menu")
        exit()
    
# confirm policy parameters
note = "Please confirm the following policy creation:"
print(f"\n{note}")
if auto_gen_name: print(f"   {'Name:':<12}{policy_name} (auto generated)")
else: print(f"   {'Name:':<18}{policy_name}")
print(f"   {'Run every:':<18}{str(sched_min)}m:{str(sched_sec)}s")
print(f"   {'Associated Tasks:':<18}")
for t in selected_tasks:
    task_uid = tasks[t][1]
    task_type = tasks[t][2]
    print(f"   uid: {task_uid:<38}, type: {task_type}")

if get_user_permission("\nContinue with policy registration?"):
    suffix = 'cockpit'
    systemd_home = "/etc/systemd/system"
    policy_desc = f"{suffix}-{policy_name}"
    policy_manager_script = f"{suffix}_{policy_name}.py"
    policy_service = f"{systemd_home}/{suffix}_{policy_name}.service"
    policy_timer = f"{systemd_home}/{suffix}_{policy_name}.timer"
    print("\n")
    
    # if the schedule is new we create the policy manager and services
    if not schedule_exists:
        # generate policy manager script from template
        if not os.path.exists(f"{policies_home}/{policy_manager_script}"):
            template = f"{templates_dir}/policy_manager_template.py"
            copy_template = f"cp {template} {policies_home}/{policy_manager_script}"
            try:
                subprocess.run([copy_template], shell=True, check=True)
            except subprocess.CalledProcessError as e:
                print(e.output)
            else:
                try:    # set execution bit for policy init script
                    subprocess.run([f"chmod +x {policies_home}/{policy_manager_script}"], shell=True, check=True)
                except subprocess.CalledProcessError as e:
                    print(e.output)
                print(f"policy manager script '{policy_manager_script}'{Fore.GREEN}created successfully!{Style.RESET_ALL}")
        else:
            print(f"policy manager script already exists. {Fore.RED}creation aborted!{Style.RESET_ALL}")
        
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
        
        # enable timer service if needed
        cur = conn.cursor()
        sql = f"SELECT active_state FROM policies where name = ? GROUP BY name;"
        cur.execute(sql, (policy_name,))
        rows = cur.fetchall()
        if rows[0][0] == 'yes':
            try:
                subprocess.run([f"systemctl enable --now {policy_timer}"], shell=True, check=True)
            except subprocess.CalledProcessError as e:
                print(e.output)
        
        print("CREATE POLICY MANAGER SUCCESSFULLY!")
        print(f"{'Schedule:':<12}\n   run every {str(sched_min)}m:{str(sched_sec)}s")
    # register and generate policy worker(s)
    for task in selected_tasks:     
        task_uid = tasks[task][1]
        task_type = tasks[task][2]
        pol_uid = str(uuid.uuid4())
        policy_worker_script = f"{pol_uid}.py"
        p_metadata = 'NULL'
        p_content = 'NULL'
        p_created = datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        p_state = 'yes'
        # register worker in database
        policy_data = (pol_uid,policy_name,schedule,task_uid,p_metadata,p_content,p_state,p_created)
        r = register_policy(conn, policy_data)
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
                print(f"policy worker script '{policy_worker_script}'{Fore.GREEN}created successfully!{Style.RESET_ALL}")
        else:
            print(f"policy worker script already exists. {Fore.RED}creation aborted!{Style.RESET_ALL}")    
        # print summary
        print(f"\nPolicy worker '{pol_uid}' {Fore.GREEN}created successfully!{Style.RESET_ALL}")
        print("Associated Tasks:")
        print(f"   uid: {task_uid:<38}, type: {task_type}")
    print()    
else:
    print(f"\nPolicy {Fore.RED}creation aborted by user!{Style.RESET_ALL}")
input("\nPress ENTER to go back to the menu")
