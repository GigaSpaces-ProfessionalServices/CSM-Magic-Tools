#!/usr/bin/python3
# *-* coding: utf-8 *-*

"""
flush: flush script
"""

import os
import yaml

def press_any_key():
    """
    pause until any key is pressed
    """

    _title = "Press any key to continue..."
    cmd = f"/bin/bash -c 'read -s -n 1 -p \"{_title}\"'"
    print('\n')
    os.system(cmd)
    print('\n')

# #from modules.classes import MySQLite
# from ..modules.cp_inputs import press_any_key

# # main #
# BASE_DIR = os.path.dirname(os.path.abspath(__file__))
# CONFIG_YAML = f"{BASE_DIR}/../config/config.yaml"

# # load yaml
# with open(CONFIG_YAML, 'r', encoding="utf-8") as y:
#     data = yaml.safe_load(y)


print(f"Executing {os.path.basename(__file__)} ...")
press_any_key()

# print(f"   {COCKPIT_DB}\n\n")
# if sqlitedb.connect() is not None:
#     title = "[ Cockpit Database Tables ]"
#     print(f"{title}\n" + '-'*len(title))
#     tables = sqlitedb.select("SELECT name FROM sqlite_master WHERE type='table';")
#     if len(tables) > 0:
#         for table_name in tables:
#             num = sqlitedb.select(f"SELECT count(*) FROM {table_name[0]};")
#             num_records = f"{num[0][0]} record(s)"
#             print(f"   {table_name[0]:<10} : {num_records:<10}")
#     print("\n")
#     title = "[ Space Object Types ]"
#     print(f"{title}\n" + '-'*len(title))
#     rows = sqlitedb.select("SELECT * FROM types;")
#     if len(rows) > 0:
#         for t in rows:
#             print(f"   {t[0]:<10}")
#     print("\n")
# else:
#     print("ERROR: unable to establish database connection.")
# press_any_key()
