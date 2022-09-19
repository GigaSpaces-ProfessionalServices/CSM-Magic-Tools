#!/usr/bin/python3
# *-* coding: utf-8 *-*

import os
import yaml
import datetime
import subprocess
from signal import SIGINT, signal
from colorama import Fore, Style
from functions import handler, create_connection, \
    list_tasks_grouped, parse_multi_select, policy_schedule_exists, \
        pretty_print, register_policy, sort_tuples_list, \
            get_user_permission, create_file

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
    note = "Choose a schedule (in seconds) value for this policy"
    print(f"\n{note}\n" + '* Example: 80 means "run every 1m:20s"\n' + '-' * len(note))
    while True:
        schedule = input("Enter schedule: ")
        if not schedule.isdigit():
            print(f"{Fore.RED}ERROR: Input must be a number!{Fore.RESET}")
            continue
        if policy_schedule_exists(conn, schedule):
            pretty_print(f"(!) a policy with this schedule already exists.", 'red')
            continue
        else:
            schedule = int(schedule)
            break
    auto_gen_name = False
    if p_name == "":
        policy_name = f"policy_{schedule}"
        auto_gen_name = True
    else:
        policy_name = p_name
    
    # parse time from schedule
    sched_sec = schedule % 60
    if sched_sec < 10:
        sched_sec = f'0{sched_sec}'
    sched_min = str(int(schedule / 60))
    
    note = "The following policy will be created:"
    print(f"\n{note}")
    if auto_gen_name: print(f"   {'Name:':<12}{policy_name} (auto generated)")
    else: print(f"   {'Name:':<12}{policy_name}")
    print(f"   {'Run every:':<12}{sched_min}m:{sched_sec}s")
    if not get_user_permission("\nContinue with policy registration?"):
        exit()
    else:
        print("\n")
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
    policy_data = (policy_name,schedule,task_uid,p_metadata,p_content,p_state,p_created)
    r = register_policy(conn, policy_data)
    print(f"\nPolicy {policy_name} {Fore.GREEN}created successfully!{Style.RESET_ALL}")
    print(f"{'Schedule:':<12}\n   run every {sched_min}m:{sched_sec}s")
    if selected_tasks[0] == -1:
        warnning = "(!) policy has been registered as deactivated until task(s) are associated with it."
        pretty_print(f"\n{warnning}", 'yellow', 'bright')
    else:
        print("Associated Tasks:")
        for t in selected_tasks:
            task_uid = tasks[t][1]
            task_type = tasks[t][2]
            print(f"   uid: {task_uid:<38}, type: {task_type}")
    print()
    
    # create policy file system objects
    policies_home = f"{os.path.dirname(os.path.realpath(__file__))}/../policies"
    if not os.path.exists(policies_home):   # create policies home if not exist
        try:
            os.makedirs(policies_home)
        except OSError as e:
            print(e)
    policy_desc = f"Cockpit-{policy_name}"
    policy_script = f"{policy_name}.py"
    systemd_home = "/etc/systemd/system"
    policy_service = f"{systemd_home}/{policy_name}.service"
    policy_timer = f"{systemd_home}/{policy_name}.timer"
    
    # create systemd policy.service
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
    create_file(lines, policy_service)
    
    # create systemd policy.timer
    policy_schedule = f"*:0/{sched_min}:{sched_sec}"
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
    create_file(lines, policy_timer)
    
    # create policy script
    lines = [
        '#!/usr/bin/python3\n\n',
        'import subprocess\n\n',
        'subprocess.run([f"echo $(date) >> /tmp/{policy_name}.test"], shell=True)\n',
        ]
    create_file(lines, f"{policies_home}/{policy_script}")
    
    # set execution bit for policy script
    subprocess.run([f"chmod +x {policies_home}/{policy_script}"], shell=True)
    
    # reload daemons
    subprocess.run(["systemctl daemon-reload"], shell=True)
    
    # enable timer service if needed
    cur = conn.cursor()
    sql = f"SELECT active_state FROM policies where name = ? GROUP BY name;"
    cur.execute(sql, (policy_name,))
    rows = cur.fetchall()
    if rows[0][0] == 'yes':
        subprocess.run([f"systemctl enable --now {policy_timer}"], shell=True)
else:
    print(f"\nPolicy {Fore.RED}creation aborted!{Style.RESET_ALL}")
input("\nPress ENTER to go back to the menu")
