#!/usr/bin/python3
# *-* coding: utf-8 *-*


import os
from influxdb import InfluxDBClient
import sqlite3
from sqlite3 import Error
import datetime
from signal import SIGINT, signal


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


def get_selected_type(types):
    # print menu
    q = "Which type would you like to validate?"
    print(q + "\n" + '=' * len(q))
    for k, v in types.items():
        index = f"[{k}]"
        print(f'{index:<4} - {v[0]:<24}')
    if len(types) > 1:
        index = f"[{k+1}]"
        print(f'{index:<4} - {"All Object Types":<24}')
    print(f'{"[99]":<4} - {"ESC":<24}')
    result = validate_input(types)
    if result != -1:
        return result
    

def get_selected_env(envs):
    # print menu
    q = "Which environment would you like to validate?"
    print(q + "\n" + '=' * len(q))
    for k, v in envs.items():
        index = f"[{k}]"
        print(f'{index:<4} - {v[0]:<24}')
    if len(envs) > 1:
        index = f"[{k+1}]"
        print(f'{index:<4} - {"All Object envs":<24}')
    print(f'{"[99]":<4} - {"ESC":<24}')
    result = validate_input(envs)
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


def generate_job(env_name, obj_type):
    env_name_low = env_name.lower()
    pivot = f"PIVOT_{env_name}"
    job_file_name = f"validation_{env_name}_{obj_type}.py".lower()
    job_file = f"jobs/{job_file_name}"
    cmd = "cat {exec_script} | ssh ${" + os.environ[pivot] + "} python3 -"
    sp_exec = 'subprocess.run([cmd], shell=True, stdout=subprocess.PIPE).stdout'
    lines = [
        '#!/usr/bin/python3\n\n',
        'import subprocess\n',
        f'exec_script = "{os.getcwd()}/scripts/get_obj_count_{env_name_low}.py"',
        f'cmd = f"{cmd}"',
        f'response = str({sp_exec}).strip(\'b"\').split(\'\\n\')',
        f'print(response)\n\n'
    ]
    with open(job_file, 'w') as j:
        j.writelines('\n'.join(lines))

# main

# catch user CTRL+C key press
signal(SIGINT, handler)

sqlite_home = '/tmp/sqlite'
cockpit_db = f"{sqlite_home}/cockpit.db"

environments = {1: ['PROD','PIVOT_PROD'], 2: ['DR', 'PIVOT_DR']}
space_types = get_object_types()
# choice env
choice = get_selected_env(environments)
envs = {}
if choice == 'ALL':
    envs = environments
else:
    envs = {1: [environments[int(choice)][0], environments[int(choice)][1]]}
# choice type
choice = get_selected_type(space_types)
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
        print(f"the type = {the_type}")
        generate_job(the_env, the_type)
        job_name = f"validation_{the_env}_{the_type}"
        job_metadata = ""
        job_content = ""
        job_command = f"validation_{the_env}_{the_type}.py".lower()
        job_dest = the_env
        job_creation_timestamp = datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        job = (job_name, job_metadata, job_content, job_command, job_dest, job_creation_timestamp)
        register_job(conn, job)

input("enter to continue")