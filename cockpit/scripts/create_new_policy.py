#!/usr/bin/python3
# *-* coding: utf-8 *-*

"""
create_new_policy: create a new cockpit policy
"""

import os
import sys
import datetime
import subprocess
import uuid
import yaml
from colorama import Fore, Style
from modules.classes import MySQLite
from modules.cp_print import pretty_print
from modules.cp_utils import (
    sort_tuples_list,
    create_file,
    execute_command
)
from modules.cp_inputs import (
    press_any_key,
    get_user_ok,
    get_keypress,
    validate_option_select
)


def deploy_systemd_components():
    """
    create systemd files
    """
    # generate policy manager script from template
    if not os.path.exists(f"{POLICIES_HOME}/{policy_manager_script}"):
        manager_template = f"{TEMPLATES_DIR}/policy_manager_template.py"
        try:
            subprocess.run(
                [f"cp {manager_template} {POLICIES_HOME}/{policy_manager_script}"],
                shell=True,
                check=True
                )
        except subprocess.CalledProcessError as _err:
            print(_err.output)
        else:
            try:    # set execution bit for policy manager script
                subprocess.run(
                    [f"chmod +x {POLICIES_HOME}/{policy_manager_script}"],
                    shell=True,
                    check=True
                    )
            except subprocess.CalledProcessError as _err:
                print(_err.output)
            print(f"policy manager '{policy_manager_script}' created successfully\n")
    else:
        print("policy manager already exists. skipping\n")

    # create systemd policy.service
    lines = [
        '[Unit]',
        f'Description={policy_desc}',
        f'Wants={suffix}_{policy_name}.timer\n',
        '[Service]',
        f'Environment="COCKPIT_HOME={os.environ["COCKPIT_HOME"]}"',
        'Type=oneshot',
        f'ExecStart={os.path.realpath(POLICIES_HOME)}/{policy_manager_script}\n',
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
    cmd = 'systemctl daemon-reload'.split(' ')
    execute_command(cmd, "reloading system daemons")

TEMPLATES_DIR = f"{os.environ['COCKPIT_HOME']}/templates"
POLICIES_HOME = f"{os.environ['COCKPIT_HOME']}/policies"
POLICIES_WORKERS_HOME = f"{POLICIES_HOME}/workers"
POLICIES_RETRY = 3

# load config yaml
CONFIG_YAML = f"{os.environ['COCKPIT_HOME']}/config/config.yaml"
with open(CONFIG_YAML, 'r', encoding="utf-8") as yml:
    data = yaml.safe_load(yml)

COCKPIT_DB_HOME = data['params']['cockpit']['db_home']
COCKPIT_DB_NAME = data['params']['cockpit']['db_name']
COCKPIT_DB = f"{COCKPIT_DB_HOME}/{COCKPIT_DB_NAME}"

# instantiate db object
sqlitedb = MySQLite(COCKPIT_DB)

# introduction
intro = [
    "A policy is defined by two types of files:",
    " - Policy manager: activated by systemd services and \
        is unique for each schedule defined.",
    " - Policy worker: is the element created for each \
        task(s) associated with a specific schedule.",
    "(!) A single manager can run multiple workers"
    ]
for line in intro:
    pretty_print(line, 'LIGHTBLUE_EX')

# get task(s) from database
tasks = sort_tuples_list(sqlitedb.select("SELECT id, uid, type FROM tasks GROUP BY uid"))
if len(tasks) == 0:
    pretty_print("\nERROR: tasks must be created first to add new policies!", 'red')
    press_any_key()
    sys.exit()

# create policies home if not exist
if not os.path.exists(POLICIES_HOME):
    try:
        # we create exec folder and its tree
        os.makedirs(f"{POLICIES_WORKERS_HOME}")
    except OSError as err:
        print(err)

# get policy schedule
schedule_exists = False
TITLE = "Enter a schedule (in seconds) for this policy"
note = '* Example: 80 means "run every 1m:20s"'
print(f"\n{TITLE}\n{note}\n" + '-'*len(TITLE))
try:
    while True:
        schedule = get_keypress()
        if schedule == 'esc':
            sys.exit()
        if not schedule.isdigit():
            pretty_print('ERROR: Input must be a number!', 'red')
            continue
        schedule = int(schedule)
        break
except (KeyboardInterrupt, SystemExit):
    os.system('stty sane')
    sys.exit()
sched_sec = schedule % 60
sched_min = int(schedule / 60)
sql = f"SELECT schedule_sec FROM policies WHERE schedule_sec = '{schedule}' GROUP BY schedule_sec;"
if len(sqlitedb.select(sql)) > 0:
    schedule_exists = True
    pretty_print("(!) a policy already exists for this schedule. \
        only workers will be added!", 'LIGHTYELLOW_EX')
print('\n')

# get policy name
if not schedule_exists:
    MAX_NAME_LENGTH = 30
    TITLE = "Enter a name for this policy or press ENTER to auto-generate"
    note = f"(!) max {MAX_NAME_LENGTH} characters allowed"
    print(f"\n{TITLE}\n{note}\n" + '-'*len(TITLE))
    NAME_OK = False
    try:
        while not NAME_OK:
            p_name = get_keypress()
            if p_name == 'esc':
                sys.exit()
            if len(p_name) <= 30:
                NAME_OK = True
                if len(p_name) == 0:
                    p_name = ""
            else:
                pretty_print(f"(!) policy name cannot exceed {MAX_NAME_LENGTH} characters", 'red')
            if p_name == "":
                policy_name = f"policy_{schedule}"
                auto_gen_name = True
            else:
                policy_name = p_name
                auto_gen_name = False
    except (KeyboardInterrupt, SystemExit):
        os.system('stty sane')
        sys.exit()
    if auto_gen_name:
        print("* auto generated\n\n")
else:
    policy_name = f"policy_{schedule}"
    auto_gen_name = True

# get task(s) selection
selected_tasks = []
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
        # we grab the task's object type
        task_obj = sqlitedb.select(sql)[0][4]
        # conform tasks to dictionary for validation func
        tasks_menu_dict[task_id] = [f"{task_type} of object type '{task_obj}'", task_uid]
        tasks_dict[task_id] = [task_type, task_uid]
    else:
        print("TBD - add procedures for tasks other than validation...")
title = "Choose tasks to be associated with this policy"
selected_tasks = validate_option_select(tasks_menu_dict, title)
if selected_tasks is None:
    sys.exit()
if -1 in selected_tasks:
    pretty_print("\n(!) no tasks have been selected. cannot continue!", 'red')
    press_any_key()
    sys.exit()

# confirm policy parameters
pretty_print("\n\n(!) Please confirm the following policy parameters:", 'yellow')
if schedule_exists:
    print(f"   {'Name:':<18}{policy_name} - already exists!")
else:
    if auto_gen_name:
        print(f"   {'Name:':<18}{policy_name} (name auto generated)")
    else:
        print(f"   {'Name:':<18}{policy_name}")
print(f"   {'Start every:':<18}{str(sched_min)}m:{str(sched_sec)}s")
print(f"   {'Retry:':<18}{str(POLICIES_RETRY)} times (* default)")
print(f"   {'Associated Tasks:':<18}")
for t in selected_tasks:
    task_type = tasks_dict[t][0]
    task_uid = tasks_dict[t][1]
    print(f"{' ':<6}Id: {t}, type: {task_type}, uid: {task_uid:<38}")

if get_user_ok("\nContinue with policy registration?"):
    suffix = 'cockpit'
    SYSTEMD_HOME = "/etc/systemd/system"
    policy_desc = f"{suffix}-{policy_name}"
    policy_manager_script = f"{suffix}_{policy_name}.py"
    policy_service = f"{SYSTEMD_HOME}/{suffix}_{policy_name}.service"
    policy_timer = f"{SYSTEMD_HOME}/{suffix}_{policy_name}.timer"
    print("\n")

    # if the schedule is new we create the policy manager and services
    if not schedule_exists:
        deploy_systemd_components()

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
        SQL = """ INSERT INTO policies(uid, name, schedule_sec, retry, task_id, \
            task_uid, metadata, content, active_state, created) 
        VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?) """
        policy_data = (p_uid, policy_name, schedule, POLICIES_RETRY, task_id, \
            task_uid, p_md, p_cont, p_active, p_created)
        r = sqlitedb.insert(SQL, policy_data)

        # generate policy worker script from template
        if not os.path.exists(f"{POLICIES_WORKERS_HOME}/{policy_worker_script}"):
            template = f"{TEMPLATES_DIR}/policy_worker_template.py"
            copy_template = f"cp {template} {POLICIES_WORKERS_HOME}/{policy_worker_script}"
            try:
                subprocess.run([copy_template], shell=True, check=True)
            except subprocess.CalledProcessError as err:
                print(err.output)
            else:
                try:    # set execution bit for policy exec script
                    subprocess.run(
                        [f"chmod +x {POLICIES_WORKERS_HOME}/{policy_worker_script}"],
                        shell=True,
                        check=True
                        )
                except subprocess.CalledProcessError as err:
                    print(err.output)
                finally:    # print summary
                    print(f"policy worker '{p_uid}' created successfully\n")
        else:
            print("policy worker script already exists. creation aborted\n")

        # if new policy manager we enable timer
        if not schedule_exists:
            sql = f"SELECT active_state FROM policies where name = '{policy_name}' GROUP BY name;"
            rows = sqlitedb.select(sql)
            if rows[0][0] == 'yes':
                cmd = f'systemctl enable --now {policy_timer}'.split(' ')
                execute_command(cmd, f"enabling cockpit policy '{policy_name}'")

press_any_key()
