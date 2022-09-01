#!/usr/bin/python3
# *-* coding: utf-8 *-*

import os
import yaml
import sqlite3


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
        print("cockpit database tables: ")
        for table_name in tables:
            print(f" - {table_name[0]}")
        c.close()
    conn.close()

config_yaml = f"{os.path.dirname(os.path.abspath(__file__))}/../config/config.yaml"

# load yaml
with open(config_yaml, 'r') as o:
    data = yaml.safe_load(o)

db_home = data['params']['cockpit']['db_home']
db_file = f"{db_home}/cockpit.db"

print(f"Cockpit DB location: {db_file}")
list_db_tables(create_connection(db_file))
input("\nPress ENTER to continue")
