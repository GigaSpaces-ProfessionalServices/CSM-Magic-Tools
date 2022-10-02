#!/usr/bin/python3
# *-* coding: utf-8 *-*

def main():
    import os
    import yaml
    from functions import (
        create_database_home, 
        create_connection, 
        create_table
        )
    
    config_yaml = f"{os.environ['COCKPIT_HOME']}/config/config.yaml"
    
    # load config yaml
    with open(config_yaml, 'r') as yf:
        data = yaml.safe_load(yf)

    cockpit_db_home = data['params']['cockpit']['db_home']
    cockpit_db_name = data['params']['cockpit']['db_name']
    cockpit_db = f"{cockpit_db_home}/{cockpit_db_name}"

    if cockpit_db == '':
        print("ERROR: config.yaml is missing the path to cockpit.db database file!")
    
    create_jobs_table = """ CREATE TABLE IF NOT EXISTS jobs (
        id          INTEGER PRIMARY KEY,
        name        TEXT NOT NULL,
        metadata    TEXT,
        content     TEXT,
        command     TEXT,
        destination TEXT,
        created     TEXT NOT NULL
        ); """

    create_tasks_table = """CREATE TABLE IF NOT EXISTS tasks (
        id          INTEGER PRIMARY KEY,
        uid         TEXT,
        type        TEXT NOT NULL,
        sn_type     INTEGER,
        job_id      INTEGER,
        metadata    TEXT,
        content     TEXT,
        state       TEXT,
        created     TEXT NOT NULL,
        FOREIGN KEY (job_id) REFERENCES jobs (id)
        );"""

    create_policies_table = """CREATE TABLE IF NOT EXISTS policies (
        id              INTEGER PRIMARY KEY,
        uid             TEXT NOT NULL,
        name            TEXT,
        schedule_sec    INTEGER NOT NULL,
        task_id         INTEGER,
        task_uid        TEXT,
        metadata        TEXT,
        content         TEXT,
        active_state    TEXT NOT NULL,
        created         TEXT NOT NULL,
        FOREIGN KEY (task_id) REFERENCES tasks (id)
        );"""

    create_types_table = """CREATE TABLE IF NOT EXISTS types (
                                        name text
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
        create_table(conn, create_policies_table)
        create_table(conn, create_types_table)
        print("Database created successfully!\n")
    else:
        print("ERROR: unable to establish database connection.")


if __name__ == '__main__':
    ### uncomment to drop database and start from fresh ###
    #drop_db = True

    main()

exit()