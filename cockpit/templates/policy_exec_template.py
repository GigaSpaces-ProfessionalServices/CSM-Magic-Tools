#!/usr/bin/python3
# *-* coding: utf-8 *-*

import os
import yaml
import json
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


def list_tasks_by_policy_uid(conn, policy_uid):
    """
    list tasks associated with a policy id
    :param conn: connection object
    :param policy_id: the policy id
    :return: list of rows
    """
    cur = conn.cursor()
    sql = "SELECT task_uid FROM policies WHERE policies.uid = ?;"
    cur.execute(sql, (policy_uid,))
    rows = cur.fetchall()
    return rows


def list_jobs_by_task_uid(conn, task_uid):
    """
    list jobs associated with a task uid
    :param conn: connection object
    :param task_uid: the task uid
    :return: list of rows
    """
    cur = conn.cursor()
    sql = """ SELECT j.name, j.id, j.destination, j.metadata, j.content 
              FROM tasks t INNER JOIN jobs j 
              ON j.id = t.job_id 
              WHERE t.uid = ?; """
    cur.execute(sql, task_uid)
    rows = cur.fetchall()
    return rows


def write_to_influx(dbname, data):
    """
    write data to influx database
    :param dbname: the name of the target database
    :param data: dictionary of the data payload
    e.g: data = {env: 'prod/dr', type: 'the_object', count: num_of_entries}
    :return:
    """
    import datetime
    from influxdb import InfluxDBClient

    client = InfluxDBClient(host='localhost', port=8086)
    if dbname not in str(client.get_list_database()):
        client.create_database(dbname)
    client.switch_database(dbname)
    timestamp = (datetime.datetime.now()).strftime('%Y-%m-%dT%H:%M:%SZ')
    json_body = [
        {
            "measurement": "validation",
            "tags": {
                "env": data['env'],
                "obj_type": data['obj_type']
            },
            "time": timestamp,
            "fields": {
                "count": data['count']
            }
        }
    ]
    client.write_points(json_body)


# main
os.environ['COCKPIT_HOME']
config_yaml = f"{os.environ['COCKPIT_HOME']}/config/config.yaml"
policies_home = f"{os.environ['COCKPIT_HOME']}/policies"
policies_exec_home = f"{policies_home}/exec"
jobs_home = f"{os.environ['COCKPIT_HOME']}/jobs"

# get policy schedule from file name
policy_uid = '.'.join(os.path.basename(__file__).split('.')[:-1])

# catch user CTRL+C key press
signal(SIGINT, handler)

# load config yaml
with open(config_yaml, 'r') as yf:
    data = yaml.safe_load(yf)

cockpit_db_home = data['params']['cockpit']['db_home']
cockpit_db_name = data['params']['cockpit']['db_name']
cockpit_db = f"{cockpit_db_home}/{cockpit_db_name}"
conn = create_connection(cockpit_db)

tasks = list_tasks_by_policy_uid(conn, policy_uid)
for task_uid in tasks:
    print(f"\n[ Task {task_uid[0]} ]")
    for job in list_jobs_by_task_uid(conn, task_uid):
        job_file = f"{job[0]}.py".lower()
        job_dest_env= job[2]
        job_type = job[3]
        script = f'{jobs_home}/{job_file}'
        print(f"   Executing job: {job_file}")
        if job_type == 'counter':
            job_obj_type = job[4]
            response = subprocess.run([script], shell=True, stdout=subprocess.PIPE).stdout.decode()
            response = json.loads(response.replace("\'", "\""))   # converting string to dictionary
            for k,v in response.items():
                if k == job_obj_type:
                    influx_data = {'env': job_dest_env, 'obj_type': job_obj_type, 'count': v['entries']}
        write_to_influx('mydb', influx_data)
