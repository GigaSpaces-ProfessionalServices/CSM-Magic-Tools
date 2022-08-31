#!/usr/bin/python3
# *-* coding: utf-8 *-*


import os
import yaml
import sqlite3
from sqlite3 import Error


def create_database_home(db_folder):
    '''
    create sqlite3 home directory if doesn't exists
    @param db_folder: sqlite3 path
    @return:
    '''
    if not os.path.exists(db_folder):
        try:
            os.makedirs(db_folder)
        except OSError as e:
            if 'Errno 13' in str(e):
                print(f"\n{e}\n *try changing the path in config.yaml")
            exit(1)

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


def create_table(conn, create_table_sql):
    '''
    create table(s) from the create_table_sql file
    @param conn: connection object
    @param create_table_sql: sqlite create table statement
    '''
    try:
        c = conn.cursor()
        c.execute(create_table_sql)
    except Error as e:
        print(e)

def main():
    config_yaml = f"{os.path.dirname(os.path.abspath(__file__))}/config/config.yaml"
    # load config yaml
    with open(config_yaml, 'r') as yf:
        data = yaml.safe_load(yf)
    cockpit_db = data['params']['cockpit']['db']
    if cockpit_db == '':
        print("ERROR: config.yaml is missing the path to cockpit.db database file!")
    else:
        cockpit_db_home = os.path.dirname(cockpit_db)
    create_jobs_table = """ CREATE TABLE IF NOT EXISTS jobs (
                                            id integer PRIMARY KEY,
                                            name text NOT NULL,
                                            metadata text,
                                            content text,
                                            command text,
                                            destination text,
                                            created text NOT NULL
                                        ); """

    create_tasks_table = """CREATE TABLE IF NOT EXISTS tasks (
                                        id integer PRIMARY KEY,
                                        uid text NOT NULL,
                                        type text NOT NULL,
                                        sn_type integer,
                                        job_id integer NOT NULL,
                                        metadata text,
                                        content text,
                                        state text,
                                        created text NOT NULL,
                                        FOREIGN KEY (job_id) REFERENCES jobs (id)
                                    );"""

    # drop database if drop_db is set
    if 'drop_db' in globals():
        os.remove(cockpit_db)

    # create sqlite3 home directory if it doesn't exists
    create_database_home(cockpit_db_home)
    
    # create database and/or tables from sql statements
    conn = create_connection(cockpit_db)
    if conn is not None:
        create_table(conn, create_tasks_table)
        create_table(conn, create_jobs_table)
        print("\nDatabase created successfully!\n")
    else:
        print("ERROR: unable to establish database connection.")


if __name__ == '__main__':
    
    ### uncomment to drop database and start from fresh ###
    #
#    drop_db = True
    #
    main()

exit()