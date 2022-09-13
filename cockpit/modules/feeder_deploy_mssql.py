#!/usr/bin/python3
# *-* coding: utf-8 *-*

# import os
# import yaml

from signal import SIGINT, signal
from functions import create_connection, handler, \
    check_connection

# main
config_yaml = f"{os.path.dirname(os.path.realpath(__file__))}/../config/config.yaml"

# catch user CTRL+C key press
signal(SIGINT, handler)

# load config yaml
with open(config_yaml, 'r') as yf:
    data = yaml.safe_load(yf)
cockpit_db_home = data['params']['cockpit']['db_home']
cockpit_db_name = data['params']['cockpit']['db_name']
cockpit_db = f"{cockpit_db_home}/{cockpit_db_name}"
conn = create_connection(cockpit_db)

print("### [ TBD ] ### DEPLOY FEEDER ###")

input("\nPress ENTER to go back to the main menu")


def get_object_types(yaml_data):
    """
    get object types from space
    :param yaml_data: the data from yaml file 
    :return: formatted dictionary as {key : [object_type, num_entries]}
    """
    import os
    import subprocess
    import json
    types = []
    connections_ok = []
    for env_name in yaml_data['params']:
        if env_name != 'cockpit':
            pivot = yaml_data['params'][env_name]['endpoints']['pivot']
            exec_script = f"{os.path.dirname(os.path.realpath(__file__))}/get_space_objects.py"
            if check_connection(pivot, 22):
                connections_ok.append(True)
                cmd = f"cat {exec_script} | ssh {pivot} python3 -"
                response = subprocess.run([cmd], shell=True, stdout=subprocess.PIPE).stdout.decode()
                response = json.loads(response.replace("\'", "\""))
                for k in response.keys():
                    if k != 'java.lang.Object':
                        types.append(k)
    if True in connections_ok:
        k = 1
        object_types = {}
        for the_type in set(types):
            v = [ the_type, response[the_type]['entries']]
            object_types[k] = v
            k += 1