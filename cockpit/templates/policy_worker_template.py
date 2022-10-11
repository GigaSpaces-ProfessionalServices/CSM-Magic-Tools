#!/usr/bin/python3
# *-* coding: utf-8 *-*

"""
policy_worker_template: template for creating worker scripts
"""

import os
import json
from random import randrange
import subprocess
from time import sleep
import datetime
import multiprocessing
import sqlite3
from sqlite3 import Error
from influxdb import InfluxDBClient
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


def write_to_influx(_dbname, _data):
    """
    write data to influx database
    :param dbname: the name of the target database
    :param data: dictionary of the data payload
                 e.g: data = {env: 'prod/dr', type: 'the_object', count: num_of_entries}
    :return:
    """
    client = InfluxDBClient(host='localhost', port=8086)
    if _dbname not in str(client.get_list_database()):
        client.create_database(_dbname)
    client.switch_database(_dbname)
    timestamp = (datetime.datetime.now()).strftime('%Y-%m-%dT%H:%M:%SZ')
    json_body = [
        {
            "measurement": "validation",
            "tags": {
                "env": _data['env'],
                "obj_type": _data['obj_type']
            },
            "time": timestamp,
            "fields": {
                "count": _data['count']
            }
        }
    ]
    client.write_points(json_body)


def calculate_retries(_conn, _puid):
    """
    calculate retry cycles and intervals accordding to database setting
    :param conn: database connection object
    :return: tuple of (retry, wait)
    """
    _sql = f"SELECT schedule_sec, retry FROM policies WHERE uid = '{_puid}'"
    _sch, _rtr = db_select(_conn, _sql)[0]
    _wait = int(_sch / (_rtr + 1))   # the interval between retries
    return (_rtr,_wait)


def exec_task_routine(_cockpit_db, _jobs_home, _task_uid, _retry, _wait):
    """
    execute task
    :param _jobs_home: jobs home folder
    :param _db: path to sqlite database
    :param _task_uid: the task uid
    :param _retry: # of retries
    :param _wait: interval between retris
    :return:
    """
    # alert handeling
    def raise_alert(_string):
        print(_string)  # DEBUG

    i = 1
    results_ok = False
    _conn = create_connection(_cockpit_db)
    while i < _retry + 1:
        print(f"\n[ Task {_task_uid[0]}: run # {i} ]")
        _sql = f""" SELECT j.name, j.id, j.destination, j.metadata, j.content
                FROM tasks t INNER JOIN jobs j
                ON j.id = t.job_id
                WHERE t.uid = '{_task_uid}'; """
        job_results = []
        for job in db_select(_conn, _sql):
            job_file = f"{job[0]}.py".lower()
            job_dest_env= job[2]
            job_type = job[3]
            job_obj_type = job[4]
            script = f'{_jobs_home}/{job_file}'
            print(f"   Executing job: {job_file}")
            if job_type == 'counter':
                response = subprocess.run(
                    [script],
                    shell=True,
                    check=True,
                    stdout=subprocess.PIPE
                    ).stdout.decode()
                # converting string to dictionary
                response = json.loads(response.replace("\'", "\""))
                for key, val in response.items():
                    if key == job_obj_type:
                        job_results.append(int(val['entries']))
                        influx_data = {
                            'env': job_dest_env,
                            'obj_type': job_obj_type,
                            'count': val['entries']
                            }
                        write_to_influx('mydb', influx_data)
                print(f"job_results --> {job_results}")
        # check if results vary to raise an alert
        if job_results.count(job_results[0]) == len(job_results):
            results_ok = True
            break
        if results_ok:
            break
        i += 1
        if i < _retry + 1:
            sleep(_wait)
    # if results not ok we raise an alert
    if not results_ok:
        raise_alert(f"{datetime.datetime.now()} object type: {job_obj_type} status: validation failed")
    else:
        print(f"{datetime.datetime.now()} object type: {job_obj_type} status: validation ok")


if __name__ == '__main__':
    CONFIG_YAML = f"{os.environ['COCKPIT_HOME']}/config/config.yaml"
    JOBS_HOME = f"{os.environ['COCKPIT_HOME']}/jobs"
    multiprocessing.set_start_method('spawn')

    # get policy schedule from file name
    POLICY_UID = '.'.join(os.path.basename(__file__).split('.')[:-1])

    # load config yaml
    with open(CONFIG_YAML, 'r', encoding="utf-8") as yf:
        data = yaml.safe_load(yf)

    cockpit_db_home = data['params']['cockpit']['db_home']
    cockpit_db_name = data['params']['cockpit']['db_name']
    cockpit_db = f"{cockpit_db_home}/{cockpit_db_name}"
    conn = create_connection(cockpit_db)

    # get retry and wait params
    _retry, _wait = calculate_retries(conn, POLICY_UID)
    rand_wait = _wait + randrange(-2,2)  # we randomize wait time by offset

    # creating child processes by task_uid
    sql = f"SELECT task_uid FROM policies WHERE uid = '{POLICY_UID}';"
    processes = [
        multiprocessing.Process(
            target=exec_task_routine,
            args=(cockpit_db, JOBS_HOME, task_uid, _retry, rand_wait),
            daemon=True
            ) for task_uid in db_select(conn, sql)[0]
    ]
    # start child processes
    for p in processes:
        p.start()
    # wait for child processes to finish
    for p in processes:
        p.join()
