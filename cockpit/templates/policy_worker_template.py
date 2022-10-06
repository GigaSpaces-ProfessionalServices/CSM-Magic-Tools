#!/usr/bin/python3
# *-* coding: utf-8 *-*

import os
import yaml
import json
from random import randrange
import subprocess
from time import sleep
import datetime
import multiprocessing
import sqlite3
from sqlite3 import Error

def create_connection(db_file):
    """
    establish a database connection (or create a new db file)
    :param db_file: path to db file
    :return: connection object
    """
    conn = None
    try:
        conn = sqlite3.connect(db_file)
    except Error as e:
        print(e)
    return conn


def db_select(conn, sql):
    """
    execute a select query on the database
    :param conn: database connection object
    :param sql: the query to execute
    :return:
    """
    try:
        c = conn.cursor()
        c.execute(sql)
    except Error as e:
        print(e)
    else:
        result = c.fetchall()
        return result


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


def calculate_retries(conn, puid):
    """
    calculate retry cycles and intervals accordding to database setting
    :param conn: database connection object
    :return: tuple of (retry, wait)
    """
    sql = f"SELECT schedule_sec, retry FROM policies WHERE uid = '{puid}'"
    _sch, _rtr = db_select(conn, sql)[0]
    _wait = int(_sch / (_rtr + 1))   # the interval between retries
    return (_rtr,_wait)


def exec_task_routine(jobs_home, db, task_uid, _retry, _wait):
    
    # alert handeling
    def raise_alert(_string):
        print(_string)  # DEBUG

    i = 1
    results_ok = False
    conn = create_connection(db)
    while i < _retry + 1:
        print(f"\n[ Task {task_uid[0]}: run # {i} ]")
        sql = f""" SELECT j.name, j.id, j.destination, j.metadata, j.content 
                FROM tasks t INNER JOIN jobs j 
                ON j.id = t.job_id 
                WHERE t.uid = '{task_uid}'; """
        job_results = []
        for job in db_select(conn, sql):
            job_file = f"{job[0]}.py".lower()
            job_dest_env= job[2]
            job_type = job[3]
            job_obj_type = job[4]
            script = f'{jobs_home}/{job_file}'
            print(f"   Executing job: {job_file}")
            if job_type == 'counter':
                response = subprocess.run([script], shell=True, stdout=subprocess.PIPE).stdout.decode()
                response = json.loads(response.replace("\'", "\""))   # converting string to dictionary
                for k,v in response.items():
                    if k == job_obj_type:
                        job_results.append(int(v['entries']))
                        influx_data = {'env': job_dest_env, 'obj_type': job_obj_type, 'count': v['entries']}
                        write_to_influx('mydb', influx_data)
                print(f"job_results --> {job_results}")
        # check if results vary to raise an alert    
        if job_results.count(job_results[0]) == len(job_results):
            results_ok = True
            break
        if results_ok: break
        i += 1
        if i < _retry + 1: sleep(_wait)
        
    # if results not ok we raise an alert
    if not results_ok:    
        raise_alert(f"{datetime.datetime.now()} object type: {job_obj_type} validation failed")
    else:
         print(f"{datetime.datetime.now()} object type: {job_obj_type} validation ok")


if __name__ == '__main__':
    config_yaml = f"{os.environ['COCKPIT_HOME']}/config/config.yaml"
    jobs_home = f"{os.environ['COCKPIT_HOME']}/jobs"
    multiprocessing.set_start_method('spawn')

    # get policy schedule from file name
    policy_uid = '.'.join(os.path.basename(__file__).split('.')[:-1])

    # load config yaml
    with open(config_yaml, 'r') as yf:
        data = yaml.safe_load(yf)

    cockpit_db_home = data['params']['cockpit']['db_home']
    cockpit_db_name = data['params']['cockpit']['db_name']
    cockpit_db = f"{cockpit_db_home}/{cockpit_db_name}"
    conn = create_connection(cockpit_db)

    # get retry and wait params
    _retry, _wait = calculate_retries(conn, policy_uid)
    rand_wait = _wait + randrange(-2,2)  # we randomize wait time by offset

    # creating child processes by task_uid
    sql = f"SELECT task_uid FROM policies WHERE uid = '{policy_uid}';"
    processes = [
        multiprocessing.Process(
            target=exec_task_routine, 
            args=(jobs_home, cockpit_db, task_uid, _retry, rand_wait), 
            daemon=True
            ) for task_uid in db_select(conn, sql)[0]
    ]
    # start child processes
    for p in processes:
        p.start()
    # wait for child processes to finish
    for p in processes:
        p.join()
