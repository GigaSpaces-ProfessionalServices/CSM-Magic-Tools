#!/usr/bin/python3
# *-* coding: utf-8 *-*

import os
import yaml
from functions import create_connection, \
    list_tables, list_types

# main #
config_yaml = f"{os.environ['COCKPIT_HOME']}/config/config.yaml"

# load yaml
with open(config_yaml, 'r') as o:
    data = yaml.safe_load(o)

cockpit_db_home = data['params']['cockpit']['db_home']
cockpit_db_name = data['params']['cockpit']['db_name']
cockpit_db = f"{cockpit_db_home}/{cockpit_db_name}"

title = "[ Cockpit DB Location ]"
print(f"{title}\n" + '-'*len(title))
print(f"   {cockpit_db}\n\n")
conn = create_connection(cockpit_db)
if conn is not None:
    title = "[ Cockpit Database Tables ]"
    print(f"{title}\n" + '-'*len(title))
    list_tables(conn)
    print("\n")
    title = "[ Space Object Types ]"
    print(f"{title}\n" + '-'*len(title))
    list_types(conn)
    print("\n")
    conn.close()
else:
    print("ERROR: unable to establish database connection.")
input("\nPress ENTER to return to main menu")
