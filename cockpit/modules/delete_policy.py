#!/usr/bin/python3
# *-* coding: utf-8 *-*

import os
import yaml
from functions import (
    create_connection,
    db_delete,
    db_select,
    execute_command,
    press_any_key, 
    validate_option_select, 
    )


# main
config_yaml = f"{os.environ['COCKPIT_HOME']}/config/config.yaml"
policies_home = f"{os.environ['COCKPIT_HOME']}/policies"
policies_workers_home = f"{policies_home}/workers"

# load config yaml
with open(config_yaml, 'r') as yf:
    data = yaml.safe_load(yf)
cockpit_db_home = data['params']['cockpit']['db_home']
cockpit_db_name = data['params']['cockpit']['db_name']
cockpit_db = f"{cockpit_db_home}/{cockpit_db_name}"
conn = create_connection(cockpit_db)

# get policies and conform to validation func
policies = {}
_index = 1
sql = f"SELECT * FROM policies GROUP BY name;"
rows = db_select(conn, sql)
for p in rows:
    pname = p[2]
    policies.update({_index: [f'{pname}']})
    _index += 1


# choice policy
if len(policies) > 0:
    title = f"Which policies would you like to delete?"
    choices = validate_option_select(policies, title)
    if choices != None:
        pol_selected = {}
        for choice in choices:
            pol_selected[int(choice)] = [policies[int(choice)][0]]
    else: quit()    

    # delete selected policies
    print('\n\n')
    for v in pol_selected.values():
        policy_name = v[0]
        policy_timer = f"cockpit_{policy_name}.timer"
        policy_service = f"cockpit_{policy_name}.service"
        
        print(f"removing {policy_name}")

        # stop timer
        title = "stopping policy timer"
        cmd = f'systemctl stop {policy_timer}'.split(' ')
        execute_command(cmd, title)
        
        # stop service
        title = "stopping policy service"
        cmd = f'systemctl stop {policy_service}'.split(' ')
        execute_command(cmd, title)
        
        # disable timer
        title = "disabling policy timer"
        cmd = f'systemctl disable {policy_timer}'.split(' ')
        execute_command(cmd, title)
        
        # disable service
        title = "disabling policy service"
        cmd = f'systemctl disable {policy_service}'.split(' ')
        execute_command(cmd, title)

        # delete systemd files
        title = "deleting systemd files"
        cmd = f'rm -f /etc/systemd/system/cockpit_{policy_name}.*'.split(' ')
        execute_command(cmd, title)

        # reloading system daemon
        title = "reloading systemd daemons"
        cmd = 'systemctl daemon-reload'.split(' ')
        execute_command(cmd, title)
        
        # remove policy workers
        sql = f"SELECT uid FROM policies WHERE name = '{policy_name}';"
        for row in db_select(conn, sql):
            worker_name = row[0]
            title = f"removing policy worker {worker_name}"
            cmd = f'rm -f {policies_workers_home}/{worker_name}.py'.split(' ')
            execute_command(cmd, title)

        # delete from database
        sql = f"DELETE from policies WHERE name = '{policy_name}';"
        r = db_delete(conn, sql)
        print(f"   policy deregistered successfully!")
else:
    print("No policies found")

press_any_key()
