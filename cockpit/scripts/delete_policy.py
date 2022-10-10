#!/usr/bin/python3
# *-* coding: utf-8 *-*

"""
delete_policy: delete cockpit policy
"""

import os
import sys
import yaml
from modules.classes import MySQLite
from modules.cp_utils import execute_command
from modules.cp_inputs import (
    press_any_key,
    validate_option_select
)

# main
CONFIG_YAML = f"{os.environ['COCKPIT_HOME']}/config/config.yaml"
POLICIES_HOME = f"{os.environ['COCKPIT_HOME']}/policies"
POLICIES_WORKERS_HOME = f"{POLICIES_HOME}/workers"

# load config yaml
with open(CONFIG_YAML, 'r', encoding="utf-8") as y:
    data = yaml.safe_load(y)

COCKPIT_DB_HOME = data['params']['cockpit']['db_home']
COCKPIT_DB_NAME = data['params']['cockpit']['db_name']
COCKPIT_DB = f"{COCKPIT_DB_HOME}/{COCKPIT_DB_NAME}"

# instantiate db object
sqlitedb = MySQLite(COCKPIT_DB)

# get policies and conform to dictionary for validation func
policies = {}
i = 1
rows = sqlitedb.select("SELECT * FROM policies GROUP BY name;")
for p in rows:
    pname = p[2]
    policies.update({i: [f'{pname}']})
    i += 1


# choice policy
if len(policies) > 0:
    choices = validate_option_select(policies, "Which policies would you like to delete?")
    if choices is not None:
        pol_selected = {}
        for choice in choices:
            pol_selected[int(choice)] = [policies[int(choice)][0]]
    else:
        sys.exit()

    # delete selected policies
    print('\n\n')
    for val in pol_selected.values():
        policy_name = val[0]
        policy_timer = f"cockpit_{policy_name}.timer"
        policy_service = f"cockpit_{policy_name}.service"
        print(f"removing {policy_name}")

        # stop timer
        cmd = f'systemctl stop {policy_timer}'.split(' ')
        execute_command(cmd, "stopping policy timer", indent=3)

        # stop service
        cmd = f'systemctl stop {policy_service}'.split(' ')
        execute_command(cmd, "stopping policy service", indent=3)

        # disable timer
        cmd = f'systemctl disable {policy_timer}'.split(' ')
        execute_command(cmd, "disabling policy timer", indent=3)

        # disable service
        cmd = f'systemctl disable {policy_service}'.split(' ')
        execute_command(cmd, "disabling policy service", indent=3)

        # delete systemd files
        cmd = f'rm -f /etc/systemd/system/cockpit_{policy_name}.*'.split(' ')
        execute_command(cmd, "deleting systemd files", indent=3)

        # reloading system daemon
        cmd = 'systemctl daemon-reload'.split(' ')
        execute_command(cmd, "reloading systemd daemons", indent=3)

        # remove policy workers
        for row in sqlitedb.select(f"SELECT uid FROM policies WHERE name = '{policy_name}';"):
            worker_name = row[0]
            cmd = f'rm -f {POLICIES_WORKERS_HOME}/{worker_name}.py'.split(' ')
            execute_command(cmd, f"removing policy worker {worker_name}", indent=3)

        # delete from database
        r = sqlitedb.delete(f"DELETE from policies WHERE name = '{policy_name}';")
        print("   policy deregistered successfully!")
else:
    print("No policies found")

press_any_key()
