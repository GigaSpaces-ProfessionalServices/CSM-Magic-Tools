#!/usr/bin/python3
# *-* coding: utf-8 *-*


import os
import sqlite3
from sqlite3 import Error


def create_database_home(db_folder):
    '''
    create sqlite3 home directory if doesn't exists
    @param db_folder: sqlite3 path
    @return:
    '''
    if not os.path.exists(db_folder): os.mkdir(db_folder)


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
        print(f"sqlite module {sqlite3.version}")
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
    sqlite_home = '/tmp/sqlite'
    cockpit_db = f"{sqlite_home}/cockpit.db"
    create_jobs_table = """ CREATE TABLE IF NOT EXISTS jobs (
                                            id integer PRIMARY KEY,
                                            uid text NOT NULL,
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
    create_database_home(sqlite_home)
    
    # create database and/or tables from sql statements
    conn = create_connection(cockpit_db)
    if conn is not None:
        create_table(conn, create_tasks_table)
        create_table(conn, create_jobs_table)
    else:
        print("ERROR: unable to establish database connection.")


if __name__ == '__main__':
    
    ### uncomment to drop database and start from fresh ###
    #
#    drop_db = True
    #
    main()

exit()