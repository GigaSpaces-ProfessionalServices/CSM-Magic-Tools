#!/usr/bin/python3
# *-* coding: utf-8 *-*

import os
import yaml
from signal import SIGINT, signal
import subprocess


def handler(signal_recieved, frame):
    """
    catch CTRL+C keybaord press
    :param signal_recieved: caught by signal class
    :param frame:
    :return:
    """
    print('\n\nOperation aborted by user!')
    exit(0)


def create_connection(db_file):
    """
    establish a database connection (or create a new db file)
    :param db_file: path to db file
    :return: connection object
    """
    import sqlite3
    from sqlite3 import Error
    conn = None
    try:
        conn = sqlite3.connect(db_file)
    except Error as e:
        print(e)
    return conn


def policy_schedule_exists(conn, policy_schedule):
    """
    check if policy schedule exists
    :param conn: database connection object
    :param policy_schedule: the policy schedule to check
    :return Boolean: True/False
    """
    c = conn.cursor()
    c.execute("SELECT schedule_sec FROM policies GROUP BY schedule_sec;")
    schedules = c.fetchall()
    for s in schedules:
        if s[0] == int(policy_schedule):
            return True
    return False


def list_policies(conn, filter_by, *columns):
    """
    list registered policies in database
    :param conn: connection object
    :param filter_by: a dictionary for sql WHERE clause
    :param *columns: list of table columns
    :return: list of rows
    """
    cur = conn.cursor()
    # parse fields from columns
    fields = ','.join(columns)
    if len(columns) == 0:
        fields = '*'
    # build query
    if filter_by == '':
        sql = f"SELECT {fields} FROM policies"
    else:
        for field, value in filter_by.items(): pass
        sql = f"SELECT {fields} FROM policies WHERE {field} = {value}"
    cur.execute(sql)
    rows = cur.fetchall()
    return rows


# main
config_yaml = f"{os.environ['COCKPIT_HOME']}/config/config.yaml"
policies_home = f"{os.environ['COCKPIT_HOME']}/policies"
policies_exec_home = f"{policies_home}/exec"

# get policy schedule from file name
policy_schedule = '.'.join(os.path.basename(__file__).split('.')[:-1]).split('_').pop()

# catch user CTRL+C key press
signal(SIGINT, handler)

# load config yaml
with open(config_yaml, 'r') as yf:
    data = yaml.safe_load(yf)

cockpit_db_home = data['params']['cockpit']['db_home']
cockpit_db_name = data['params']['cockpit']['db_name']
cockpit_db = f"{cockpit_db_home}/{cockpit_db_name}"
conn = create_connection(cockpit_db)

# execute all policies associated with this schedule
if policy_schedule_exists(conn, policy_schedule):
    policies_to_run = list_policies(conn, {'schedule_sec': policy_schedule}, 'uid')
    for policy_uid in policies_to_run:
        p_uid = policy_uid[0]
        try:
            subprocess.run([f"{policies_exec_home}/{p_uid}.py"], shell=True, check=True)
        except subprocess.CalledProcessError as e:
            print(e.output)
else:
    print(f"ERROR: policy schedule {policy_schedule} does not exist!")
