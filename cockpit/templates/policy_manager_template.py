#!/usr/bin/python3
# *-* coding: utf-8 *-*

"""
policy_manager_template: template for creating manager scripts
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
config_yaml = f"{os.environ['COCKPIT_HOME']}/config/config.yaml"
policies_home = f"{os.environ['COCKPIT_HOME']}/policies"
policies_workers_home = f"{policies_home}/workers"

# get policy schedule from file name
policy_schedule = '.'.join(os.path.basename(__file__).split('.')[:-1]).split('_').pop()

# load config yaml
with open(config_yaml, 'r', encoding="utf-8") as yf:
    data = yaml.safe_load(yf)

cockpit_db_home = data['params']['cockpit']['db_home']
cockpit_db_name = data['params']['cockpit']['db_name']
cockpit_db = f"{cockpit_db_home}/{cockpit_db_name}"
conn = create_connection(cockpit_db)

# initialize policy workers
sql = f"SELECT schedule_sec FROM policies WHERE \
    schedule_sec = '{policy_schedule}' GROUP BY schedule_sec"
schedules = db_select(conn, sql)
if len(schedules) != 0:
    sql = f"SELECT uid, active_state FROM policies WHERE schedule_sec = '{policy_schedule}'"
    policies_to_run = db_select(conn,sql)
    for p in policies_to_run:
        p_uid = p[0]
        p_active = p[1]
        if p_active == 'yes':   # run only workers that have active_state='yes'
            try:
                subprocess.run([f"{policies_workers_home}/{p_uid}.py"], shell=True, check=True)
            except subprocess.CalledProcessError as e:
                print(e.output)
else:
    print(f"ERROR: policy schedule {policy_schedule} does not exist!")
