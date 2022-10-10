#!/usr/bin/python3
# *-* coding: utf-8 *-*

"""
db_info: show database information
"""

import os
import yaml
from modules.classes import MySQLite
from modules.cp_inputs import press_any_key

# main #
CONFIG_YAML = f"{os.environ['COCKPIT_HOME']}/config/config.yaml"

# load yaml
with open(CONFIG_YAML, 'r', encoding="utf-8") as y:
    data = yaml.safe_load(y)

COCKPIT_DB_HOME = data['params']['cockpit']['db_home']
COCKPIT_DB_NAME = data['params']['cockpit']['db_name']
COCKPIT_DB = f"{COCKPIT_DB_HOME}/{COCKPIT_DB_NAME}"

# instantiate db object
sqlitedb = MySQLite(COCKPIT_DB)

title = "[ Cockpit DB Location ]"
print(f"{title}\n" + '-'*len(title))
print(f"   {COCKPIT_DB}\n\n")
if sqlitedb.connect() is not None:
    title = "[ Cockpit Database Tables ]"
    print(f"{title}\n" + '-'*len(title))
    tables = sqlitedb.select("SELECT name FROM sqlite_master WHERE type='table';")
    if len(tables) > 0:
        for table_name in tables:
            num = sqlitedb.select(f"SELECT count(*) FROM {table_name[0]};")
            num_records = f"{num[0][0]} record(s)"
            print(f"   {table_name[0]:<10} : {num_records:<10}")
    print("\n")
    title = "[ Space Object Types ]"
    print(f"{title}\n" + '-'*len(title))
    rows = sqlitedb.select("SELECT * FROM types;")
    if len(rows) > 0:
        for t in rows:
            print(f"   {t[0]:<10}")
    print("\n")
else:
    print("ERROR: unable to establish database connection.")
press_any_key()
