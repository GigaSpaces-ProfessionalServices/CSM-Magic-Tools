#!/usr/bin/python3
# *-* coding: utf-8 *-*


import os
import yaml
import sqlite3
from sqlite3 import Error
from signal import SIGINT, signal


def handler(signal_recieved, frame):
    print('\n\nOperation aborted by user!')
    exit(0)


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


def list_policies(conn):
    c = conn.cursor()
    c.execute("SELECT * FROM policies;")
    registered_policies = c.fetchall()
    return registered_policies


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
conn = create_connection(cockpit_db)
policies = list_policies(conn)
if len(policies) > 0:
    w = 62
    print("-"*w + f'\n| {"Id":^4} | {"UID":^40} | {"Type":^8} |\n' + "-"*w)
    for p_id, p_uid, p_type in policies:
        print(f'| {p_id:<4} | {p_uid:<40} | {p_type:<8} |')
    print("-"*w)
else:
    print("No policies found")

input("\nPress ENTER to go back to the main menu")
