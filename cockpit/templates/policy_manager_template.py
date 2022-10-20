#!/usr/bin/python3
# *-* coding: utf-8 *-*

"""
policy_manager: policy manager script based on schedule
"""

import os
import subprocess
import sqlite3
from sqlite3 import Error
import yaml

def create_connection(db_file):
    """
    establish a database connection (or create a new db file)
    :param db_file: path to db file
    :return: connection object
    """
    _c = None
    try:
        _c = sqlite3.connect(db_file)
    except Error as err:
        print(err)
    return _c


def db_select(_conn, _sql):
    """
    execute a select query on the database
    :param conn: database connection object
    :param sql: the query to execute
    :return: list of rows
    """
    try:
        _c = _conn.cursor()
        _c.execute(_sql)
    except Error as err:
        print(err)
        return None
    else:
        return _c.fetchall()


# main
CONFIG_YAML = f"{os.environ['COCKPIT_HOME']}/config/config.yaml"
POLICIES_HOME = f"{os.environ['COCKPIT_HOME']}/policies"
POLICIES_WORKERS_HOME = f"{POLICIES_HOME}/workers"

# get policy schedule from file name
POLICY_SCHEDULE = '.'.join(os.path.basename(__file__).split('.')[:-1]).split('_').pop()

# load config yaml
with open(CONFIG_YAML, 'r', encoding="utf-8") as yf:
    data = yaml.safe_load(yf)

COCKPIT_DB_HOME = data['params']['cockpit']['db_home']
COCKPIT_DB_NAME = data['params']['cockpit']['db_name']
COCKPIT_DB = f"{COCKPIT_DB_HOME}/{COCKPIT_DB_NAME}"
conn = create_connection(COCKPIT_DB)

# initialize policy workers
sql = f"SELECT schedule_sec FROM policies WHERE \
    schedule_sec = '{POLICY_SCHEDULE}' GROUP BY schedule_sec"
schedules = db_select(conn, sql)
if len(schedules) != 0:
    sql = f"SELECT uid, active_state FROM policies WHERE schedule_sec = '{POLICY_SCHEDULE}'"
    policies_to_run = db_select(conn,sql)
    for p in policies_to_run:
        p_uid = p[0]
        p_active = p[1]
        if p_active == 'yes':   # run only workers that have active_state='yes'
            try:
                subprocess.run([f"{POLICIES_WORKERS_HOME}/{p_uid}.py"], shell=True, check=True)
            except subprocess.CalledProcessError as e:
                print(e.output)
else:
    print(f"ERROR: policy schedule {POLICY_SCHEDULE} does not exist!")
