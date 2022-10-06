#!/usr/bin/python3
# *-* coding: utf-8 *-*

import os
import yaml
from classes import MySQLite
from functions import press_any_key

# main #
config_yaml = f"{os.environ['COCKPIT_HOME']}/config/config.yaml"

# load yaml
with open(config_yaml, 'r') as o:
    data = yaml.safe_load(o)

cockpit_db_home = data['params']['cockpit']['db_home']
cockpit_db_name = data['params']['cockpit']['db_name']
cockpit_db = f"{cockpit_db_home}/{cockpit_db_name}"

# instantiate db object
sqlitedb = MySQLite(cockpit_db)

title = "[ Cockpit DB Location ]"
print(f"{title}\n" + '-'*len(title))
print(f"   {cockpit_db}\n\n")
if sqlitedb.connect() is not None:
    title = "[ Cockpit Database Tables ]"
    print(f"{title}\n" + '-'*len(title))
    sql = "SELECT name FROM sqlite_master WHERE type='table';"
    tables = sqlitedb.select(sql)   
    if len(tables) > 0:
        for table_name in tables:
            num = sqlitedb.select(f"SELECT count(*) FROM {table_name[0]};")
            num_records = f"{num[0][0]} record(s)"
            print(f"   {table_name[0]:<10} : {num_records:<10}")
    print("\n")
    title = "[ Space Object Types ]"
    print(f"{title}\n" + '-'*len(title))    
    sql = "SELECT * FROM types;"
    rows = sqlitedb.select(sql)
    if len(rows) > 0:
        for t in rows:
            print(f"   {t[0]:<10}")
    print("\n")
else:
    print("ERROR: unable to establish database connection.")
press_any_key()
