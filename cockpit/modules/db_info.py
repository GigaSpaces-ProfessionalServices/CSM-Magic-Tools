#!/usr/bin/python3
# *-* coding: utf-8 *-*

import os
import yaml
import sqlite3
from signal import SIGINT, signal
from functions import handler, create_connection, \
    list_tables



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
    list_tables(conn)
else:
    print("ERROR: unable to establish database connection.")
input("\nPress ENTER to return to main menu")
