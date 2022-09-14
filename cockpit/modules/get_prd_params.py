#!/usr/bin/python3
# *-* coding: utf-8 *-*

import os
import yaml

config_yaml = f"{os.path.dirname(os.path.realpath(__file__))}/../config/config.yaml"
label = 'prd'

# load yaml
with open(config_yaml, 'r') as o:
    data = yaml.safe_load(o)

update_yaml = False
pivot_value = data['params'][label]['endpoints']['pivot']
pivot_envar = os.environ.get(data['params'][label]['variables']['pivot']) 
rest_value = data['params'][label]['endpoints']['rest']
rest_envar = os.environ.get(data['params'][label]['variables']['rest']) 

print(f"[ {label} ]".upper())
if pivot_value == '' or pivot_value is None:
    data['params'][label]['endpoints']['pivot'] = pivot_envar
    pivot_value = data['params'][label]['endpoints']['pivot']
    print(f"  pivot address set as: {pivot_value}")
    update_yaml = True
if rest_value == '' or rest_value is None:
    if rest_envar != None:
        data['params'][label]['endpoints']['rest'] = rest_envar
        rest_value = data['params'][label]['endpoints']['rest']
        print(f"  REST endpoint set as: {rest_value}")
        update_yaml = True
    else:
        rest_value = ''

if update_yaml:
    with open(config_yaml, 'w') as n:
        n.write(yaml.dump(data, default_flow_style=False))
else:
    print(f"  pivot address: {pivot_value}")
    print(f"  REST endpoint: {rest_value}")
    input("\nPress ENTER to continue")