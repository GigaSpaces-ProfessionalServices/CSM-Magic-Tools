#!/usr/bin/python3
# *-* coding: utf-8 *-*


import os
import yaml
from influxdb import InfluxDBClient
import sqlite3
from sqlite3 import Error
import datetime
from signal import SIGINT, signal
import subprocess


def handler(signal_recieved, frame):
    print('\n\nOperation aborted by user!')
    exit(0)


def get_object_types():
    types = {
        1: ['msgPojo-1', 1001], 
        2: ['msgPojo-2', 1002], 
        3: ['msgPojo-3', 1003], 
        4: ['msgPojo-4', 1004], 
        5: ['msgPojo-5', 1005]
        }
    return types


def validate_input(items_dict):
    from colorama import Fore
    choice = input("\nEnter your choice: ")
    while True:
        if choice == '99':
            return -1
        if len(items_dict) > 1:
            if choice == str(len(items_dict) + 1): # if 'ALL' is selected
                return "ALL"
        if not choice.isdigit() or int(choice) not in items_dict.keys():
            choice = input(f"{Fore.RED}ERROR: Input must be a menu index!{Fore.RESET}\nEnter you choice: ")
        else:
            return int(choice)


def get_selection(the_dict, description):
    # print menu
    q = f"Which {description} would you like to validate?"
    print(q + "\n" + '=' * len(q))
    for k, v in the_dict.items():
        index = f"[{k}]"
        print(f'{index:<4} - {v[0]:<24}')
    if len(the_dict) > 1:
        index = f"[{k+1}]"
        print(f'{index:<4} - {"All Environments":<24}')
    print(f'{"[99]":<4} - {"ESC":<24}')
    result = validate_input(the_dict)
    if result != -1:
        return result


def create_connection(db_file):
    '''
    create a database connection or a new 
    file-based database if it doesn't exist
    @param db_file: path to db file
    @return: connection object
    '''
    conn = None
    try:
        conn = sqlite3.connect(db_file)
    except Error as e:
        print(e)
    return conn


def jobs_exist(conn, new_job_name):
    job_exists = False
    c = conn.cursor()
    c.execute("SELECT name FROM jobs;")
    registered_job_names = c.fetchall()
    for name in registered_job_names:
        if new_job_name in name:
            job_exists = True
            break
    return job_exists


def register_job(conn, job):
    """
    Register a new job
    @param conn: database connection object
    @param job: job data
    @return: job id
    """
    sql = ''' INSERT INTO jobs(name,metadata,content,command,destination,created)
              VALUES(?,?,?,?,?,?) '''
    cur = conn.cursor()
    cur.execute(sql, job)
    conn.commit()
    return cur.lastrowid


def generate_job(env_name, obj_type, yaml_data):
    env_name_low = env_name.lower()
    pivot = f"PIVOT_{env_name}"
    jobs_home = f"{os.path.dirname(os.path.abspath(__file__))}/../jobs"
    job_file_name = f"validation_{env_name}_{obj_type}.py".lower()
    job_file = f"{jobs_home}/{job_file_name}"
    pivot = yaml_data['params'][env_name_low]['endpoints']['pivot']
    cmd = "cat {exec_script} | ssh " + pivot + " python3 -"
    sp_exec = 'subprocess.run([cmd], shell=True, stdout=subprocess.PIPE).stdout'
    lines = [
        '#!/usr/bin/python3\n\n',
        'import subprocess\n',
        f'exec_script = "{os.path.dirname(os.path.abspath(__file__))}/get_obj_count_{env_name_low}.py"',
        f'cmd = f"{cmd}"',
        f'response = {sp_exec}',
        f'print(response.decode())\n\n'
    ]
    # create jobs home folder if not exists
    if not os.path.exists(jobs_home):
        try:
            os.makedirs(jobs_home)
        except OSError as e:
            print(e)
    with open(job_file, 'w') as j:
        j.writelines('\n'.join(lines))
    # set execution bit for job file
    subprocess.run([f"chmod +x {job_file}"], shell=True)


# main
config_yaml = f"{os.path.dirname(os.path.abspath(__file__))}/../config/config.yaml"

# catch user CTRL+C key press
signal(SIGINT, handler)

# load config yaml
with open(config_yaml, 'r') as yf:
    data = yaml.safe_load(yf)
cockpit_db_home = data['params']['cockpit']['db_home']
cockpit_db_name = data['params']['cockpit']['db_name']
cockpit_db = f"{cockpit_db_home}/{cockpit_db_name}"
environments = {
    1: ['DR',data['params']['dr']['variables']['pivot']], 
    2: ['PRD', data['params']['prd']['variables']['pivot']]
    }
space_types = get_object_types()
# choice env
choice = get_selection(environments, 'environments')
envs = {}
if choice == 'ALL':
    envs = environments
else:
    envs = {1: [environments[int(choice)][0], environments[int(choice)][1]]}
# choice type
choice = get_selection(space_types, 'types')
types = {}
if choice == 'ALL':
    types = space_types
else:
    types = {1: [space_types[int(choice)][0], space_types[int(choice)][1]]}
for e in envs.values():
    the_env = e[0]
    for t in types.values():
        the_type = t[0]
        conn = create_connection(cockpit_db)
        job_name = f"validation_{the_env}_{the_type}"
        job_metadata = ""
        job_content = ""
        job_command = f"validation_{the_env}_{the_type}.py".lower()
        job_dest = the_env
        job_creation_timestamp = datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        job = (job_name, job_metadata, job_content, job_command, job_dest, job_creation_timestamp)
        if jobs_exist(conn, job_name):
            print(f"Job: {job_name} already exists.")
        else:
            generate_job(the_env, the_type, data)
            r = register_job(conn, job)
            print(f"Job: {job_name} with Id: {r} created successfully")
input("\nPress ENTER to continue to the main menu.")
