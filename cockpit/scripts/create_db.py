#!/usr/bin/python3
# *-* coding: utf-8 *-*

"""
create_db: create the cockpit database and tables
"""

import os
import yaml
from modules.classes import MySQLite

# load configuration yaml data
CONFIG_YAML = f"{os.environ['COCKPIT_HOME']}/config/config.yaml"
with open(CONFIG_YAML, 'r', encoding="utf-8") as _yf:
    data = yaml.safe_load(_yf)

COCKPIT_DB_HOME = data['params']['cockpit']['db_home']
COCKPIT_DB_NAME = data['params']['cockpit']['db_name']
COCKPIT_DB = f"{COCKPIT_DB_HOME}/{COCKPIT_DB_NAME}"

# instantiate db object
sqlitedb = MySQLite(COCKPIT_DB)

CREATE_JOBS_TABLE = """ CREATE TABLE IF NOT EXISTS jobs (
    id          INTEGER PRIMARY KEY,
    name        TEXT NOT NULL,
    metadata    TEXT,
    content     TEXT,
    command     TEXT,
    destination TEXT,
    created     TEXT NOT NULL
    ); """

CREATE_TASKS_TABLE = """CREATE TABLE IF NOT EXISTS tasks (
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

CREATE_POLICIES_TABLE = """CREATE TABLE IF NOT EXISTS policies (
    id              INTEGER PRIMARY KEY,
    uid             TEXT NOT NULL,
    name            TEXT,
    schedule_sec    INTEGER NOT NULL,
    retry           INTEGER NOT NULL,
    task_id         INTEGER,
    task_uid        TEXT,
    metadata        TEXT,
    content         TEXT,
    active_state    TEXT NOT NULL,
    created         TEXT NOT NULL,
    FOREIGN KEY (task_id) REFERENCES tasks (id)
    );"""

CREATE_TYPES_TABLE = """CREATE TABLE IF NOT EXISTS types (
                                    name text
                                );"""

# drop database if drop_db is set
if 'drop_db' in globals():
    os.remove(COCKPIT_DB)

if sqlitedb.connect() is not None:
    sqlitedb.create(CREATE_TASKS_TABLE)
    sqlitedb.create(CREATE_JOBS_TABLE)
    sqlitedb.create(CREATE_POLICIES_TABLE)
    sqlitedb.create(CREATE_TYPES_TABLE)
    print("Database created successfully!\n")
else:
    print("ERROR: unable to establish database connection.")
