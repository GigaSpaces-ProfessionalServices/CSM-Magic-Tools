#!/usr/bin/python3
# *-* coding: utf-8 *-*

import os
import yaml
import sqlite3
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


def list_db_tables(conn):
    c = conn.cursor()
    c.execute("SELECT name FROM sqlite_master WHERE type='table';")
    tables = c.fetchall()
    if len(tables) > 0:
        print("\n[Cockpit database tables]")
        for table_name in tables:
            c.execute(f"SELECT count(*) FROM {table_name[0]};")
            num_records = f"{len(c.fetchall())} record(s)"
            print(f"   {table_name[0]:<6} : {num_records:<10}")
        c.close()
    conn.close()


# main #
config_yaml = f"{os.path.dirname(os.path.abspath(__file__))}/../config/config.yaml"

# catch user CTRL+C key press
signal(SIGINT, handler)

# load yaml
with open(config_yaml, 'r') as o:
    data = yaml.safe_load(o)

cockpit_db_home = data['params']['cockpit']['db_home']
cockpit_db_name = data['params']['cockpit']['db_name']
cockpit_db = f"{cockpit_db_home}/{cockpit_db_name}"

print(f"[Cockpit DB location]\n   {cockpit_db}\n")
conn = create_connection(cockpit_db)
if conn is not None:
    list_db_tables(conn)
else:
    print("ERROR: unable to establish database connection.")
input("\nPress ENTER to return to main menu")
