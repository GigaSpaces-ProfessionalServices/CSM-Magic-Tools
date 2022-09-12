#!/usr/bin/python3
# *-* coding: utf-8 *-*

def main():
    import os
    import yaml
    from functions import create_database_home, create_connection, create_table
    config_yaml = f"{os.path.dirname(os.path.realpath(__file__))}/../config/config.yaml"
    # load config yaml
    with open(config_yaml, 'r') as yf:
        data = yaml.safe_load(yf)
    cockpit_db_home = data['params']['cockpit']['db_home']
    cockpit_db_name = data['params']['cockpit']['db_name']
    cockpit_db = f"{cockpit_db_home}/{cockpit_db_name}"
    if cockpit_db == '':
        print("ERROR: config.yaml is missing the path to cockpit.db database file!")
    
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
                                        job_id integer,
                                        metadata text,
                                        content text,
                                        state text,
                                        created text NOT NULL,
                                        FOREIGN KEY (job_id) REFERENCES jobs (id)
                                    );"""

    create_policies_table = """CREATE TABLE IF NOT EXISTS policies (
                                        id integer PRIMARY KEY,
                                        schedule integer NOT NULL,
                                        repeat integer NOT NULL,
                                        task_uid text,
                                        metadata text,
                                        content text,
                                        state text,
                                        created text NOT NULL,
                                        FOREIGN KEY (task_uid) REFERENCES tasks (uid)
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
        print("Database created successfully!\n")
    else:
        print("ERROR: unable to establish database connection.")


if __name__ == '__main__':
    ### uncomment to drop database and start from fresh ###
    #drop_db = True

    main()

exit()