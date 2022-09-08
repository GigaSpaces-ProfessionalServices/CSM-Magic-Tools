#!/usr/bin/python3
# *-* coding: utf-8 *-*

import os
import yaml

config_yaml = f"{os.path.dirname(os.path.abspath(__file__))}/../config/config.yaml"
label = 'prd'
label_print = label.upper()

# load yaml
with open(config_yaml, 'r') as o:
    data = yaml.safe_load(o)

update_yaml = False
pivot_ep = data['params'][label]['endpoints']['pivot']
pivot_env_var = os.environ.get(data['params'][label]['variables']['pivot']) 
rest_ep = data['params'][label]['endpoints']['rest']
rest_env_var = os.environ.get(data['params'][label]['variables']['rest']) 


if pivot_ep == '' or pivot_ep is None:
    data['params'][label]['endpoints']['pivot'] = pivot_env_var
    pivot_ep = data['params'][label]['endpoints']['pivot']
    print(f"{label_print} pivot address set as: {pivot_ep}")
    update_yaml = True
if rest_ep == '' or rest_ep is None:
    if rest_env_var != None:
        data['params'][label]['endpoints']['rest'] = rest_env_var
        rest_ep = data['params'][label]['endpoints']['rest']
        print(f"{label_print} REST endpoint set as: {rest_ep}")
        update_yaml = True
    else:
        rest_ep = ''

if update_yaml:
    with open(config_yaml, 'w') as n:
        n.write(yaml.dump(data, default_flow_style=False))
else:
    print(f"{label_print} pivot address: {pivot_ep}")
    print(f"{label_print} REST endpoint: {rest_ep}")
    input("\nPress ENTER to continue")