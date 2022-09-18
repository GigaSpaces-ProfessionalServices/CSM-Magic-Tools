#!/usr/bin/python3
# *-* coding: utf-8 *-*

import os
import yaml
import datetime
import subprocess
from signal import SIGINT, signal
from colorama import Fore, Style
from functions import handler, create_connection, list_tasks_by_policy_schedule, \
    list_tasks_grouped, parse_multi_select, policy_schedule_exists, pretty_print, \
        register_policy, sort_tuples_list, get_user_permission

# main
config_yaml = f"{os.path.dirname(os.path.realpath(__file__))}/../config/config.yaml"

# catch user CTRL+C key press
signal(SIGINT, handler)

# load config yaml
with open(config_yaml, 'r') as yf:
    data = yaml.safe_load(yf)
# db from yaml
cockpit_db_home = data['params']['cockpit']['db_home']
cockpit_db_name = data['params']['cockpit']['db_name']
cockpit_db = f"{cockpit_db_home}/{cockpit_db_name}"
p_name_len = 30
note = f"Choose a name for this policy (up to {p_name_len} characters)"
print(f"\n{note}")
name_ok = False
while not name_ok:
    p_name = input("Enter policy name [leave empty to auto-generate name]: ")
    if len(p_name) <= 30:
        name_ok = True
        if len(p_name) == 0:
            p_name = ""
    else:
        pretty_print(f"(!) policy name cannot exceed {p_name_len} characters", 'red')
# get the task(s) from db
conn = create_connection(cockpit_db)
tasks = sort_tuples_list(list_tasks_grouped(conn, 'id', 'uid', 'type'))
selected_tasks = []
if len(tasks) > 0:
    note = "Choose tasks to be associated with this policy"
    print(f"\n{note}")
    print("(!) collections are supported (i.e: 1,3,2-5)")
    print('-' * len(note))
    index = 1
    for task in tasks:
        i = f"{[index]}"
        print(f'{i:<4} - {task[2]} (uid: {task[1]}) ***more info required - TBD')
        index += 1
    print(f'{"[99]":<4} - {"ESC":<24}')
    for i in parse_multi_select(tasks):
        if i != -1:
            selected_tasks.append(i-1)
        else:
            selected_tasks.append(i)
else:
    pretty_print("\n(!) no tasks have been created yet!", 'yellow', 'bright')
    selected_tasks.append(-1)
user_abort = False
if selected_tasks[0] == -1:
    # if no tasks we ask the user
    if not get_user_permission("Would you like to continue policy creation with no tasks?"):
        user_abort = True
if not user_abort:
    # get the schedule for this task
    note = "Choose schedule (in seconds) and retry values for this policy"
    print(f"\n{note}")
    print('   * schedule = 210 means "run every 3m:30s"')
    print("   * retry    = number of times to retry on failure")
    print("   * wait     = will be calculated automatically")
    print('-' * len(note))
    schedule_ok = False
    while not schedule_ok:
        schedule = input("Enter schedule: ")
        while True:
            if policy_schedule_exists(conn, schedule):
                pretty_print(f"(!) a policy with this schedule already exists. use " )
            if not schedule.isdigit():
                schedule = input(f"{Fore.RED}ERROR: Input must be a number!{Fore.RESET}\nEnter schedule: ")
            else:
                schedule = int(schedule)
                break
        repeat = input("Enter retry: ")
        while True:
            if not repeat.isdigit():
                repeat = input(f"{Fore.RED}ERROR: Input must be a number!{Fore.RESET}\nEnter retry: ")
            else:
                repeat = int(repeat)
                break
        # calculate wait time between retries
        wait_repeat = int(schedule / (repeat + 1))
        print("\nA policy will be created with the following parameters:")
        print(f"   {'run every: ':<22}{schedule} seconds")
        print(f"   {'times to retry : ':<22}{repeat} times")
        print(f"   {'wait between retries: ':<22}{wait_repeat} seconds")
        if p_name == "":
            print(f"   {'policy name: ':<22}policy{schedule}_{repeat} (auto-generated)")
        else:
            print(f"   {'policy name: ':<22}{p_name}")
        if get_user_permission("\nContinue with policy registration?"):
            break
        else:
            print("\n")
    if p_name == "": p_name = f"policy{schedule}_{repeat}"
    p_metadata = 'NULL'
    p_content = 'NULL'
    p_created = datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')
    for task in selected_tasks:
        if task == -1:
            task_uid = 'NULL'
            p_state = 'no'
        else:
            p_state = 'yes'
            task_uid = tasks[task][1]
            task_type = tasks[task][2]
    policy_data = (p_name,schedule,repeat, wait_repeat,task_uid,p_metadata,p_content,p_state,p_created)
    r = register_policy(conn, policy_data)
    print(f"\nPolicy {p_name} {Fore.GREEN}created successfully!{Style.RESET_ALL}")
    print(f"Parameters:")
    print(f"   {'schedule:':<12}{schedule}s\n   {'repeat':<12}{repeat} times\n   {'every:':<12}{wait_repeat}s")
    if selected_tasks[0] == -1:
        pretty_print(f"\n(!) policy has been registered as deactivated since no task(s) are associated with it.", 'yellow', 'bright')
    else:
        print("Associated Tasks:")
        for t in selected_tasks:
            task_uid = tasks[t][1]
            task_type = tasks[t][2]
            print(f"   uid: {task_uid:<38}, type: {task_type}")
    print()
    # create systemd service, timer and policy scripts
    policies_home = f"{os.path.dirname(os.path.realpath(__file__))}/../policies"
    if not os.path.exists(policies_home):   # create policies home if not exist
        try:
            os.makedirs(policies_home)
        except OSError as e:
            print(e)
    policy_desc = f"Repeat {repeat} times over a {schedule} sec period"
    policy_name = f"policy{schedule}_{repeat}"
    policy_script = f"policy{schedule}_{repeat}.py"
    systemd_home = "/etc/systemd/system"
    policy_service = f"{systemd_home}/policy{schedule}_{repeat}.service"
    policy_timer = f"{systemd_home}/policy{schedule}_{repeat}.timer"
    
    # create policy.service
    lines = [
        '[Unit]',
        f'Description={policy_desc}',
        f'Wants={policy_name}.timer\n',
        '[Service]',
        'Type=oneshot',
        f'ExecStart={os.path.realpath(policies_home)}/{policy_script}\n',
        '[Install]',
        'WantedBy=multi-user.target\n'
        ]
    try:
        with open(f"{policy_service}", 'w') as f:
            f.writelines('\n'.join(lines))
    except IOError as e:
        print(f"Policy service {Fore.RED}creation failed!{Style.RESET_ALL}")    
        print(e)
    else:
        print(f"Policy service {Fore.GREEN}created successfully!{Style.RESET_ALL}")
    
    # create policy.timer
    sched_sec = schedule % 60
    if sched_sec < 10:
        sched_sec = f'0{sched_sec}'
    sched_min = str(int(schedule / 60))
    policy_schedule = f"*:0/{sched_min}:{sched_sec}"
    # Every Minute	*-*-* *:*:00
    # Every 2 minute	*-*-* *:*/2:00
    lines = [
        '[Unit]',
        f'Description={policy_desc}',
        f'Requires={policy_name}.service\n',
        '[Timer]',
        f'Unit={policy_name}',
        f'OnCalendar={policy_schedule}\n',
        '[Install]',
        'WantedBy=timers.target\n'
        ]
    try:
        with open(f"{policy_timer}", 'w') as f:
            f.writelines('\n'.join(lines))
    except IOError as e:
        print(f"Policy timer {Fore.RED}creation failed!{Style.RESET_ALL}")    
        print(e)
    else:
        print(f"Policy timer {Fore.GREEN}created successfully!{Style.RESET_ALL}")    
    
    # create policy script
    lines = [
        '#!/usr/bin/python3\n\n',
        'echo $(date +"%s) >> /tmp/{policy_name}.test\n',
        ]
    try:
        with open(f"{policies_home}/{policy_script}", 'w') as f:
            f.writelines('\n'.join(lines))
    except IOError as e:
        print(f"Policy script {Fore.RED}creation failed!{Style.RESET_ALL}")    
        print(e)
    else:
        print(f"Policy script {Fore.GREEN}created successfully!{Style.RESET_ALL}")    
    # set execution bit for job file
    subprocess.run([f"chmod +x {policies_home}/{policy_script}"], shell=True)    
else:
    print(f"\nPolicy {Fore.RED}creation aborted!{Style.RESET_ALL}")
input("\nPress ENTER to go back to the menu")
