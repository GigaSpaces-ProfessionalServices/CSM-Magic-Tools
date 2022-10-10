#!/usr/bin/python3
# *-* coding: utf-8 *-*

"""
get_prd_params; get attributes/parameters of set environment
"""

import os
import sys
# adding modules to path for imports
sys.path.insert(1, os.path.realpath(f"{os.path.dirname(__file__)}/../modules"))

import yaml
from cp_inputs import press_any_key

CONFIG_YAML = f"{os.environ['COCKPIT_HOME']}/config/config.yaml"
LABEL = 'prd'

# load yaml
with open(CONFIG_YAML, 'r', encoding="utf-8") as y:
    data = yaml.safe_load(y)

update_yaml = False
pivot_value = data['params'][LABEL]['endpoints']['pivot']
pivot_envar = os.environ.get(data['params'][LABEL]['variables']['pivot'])
rest_value = data['params'][LABEL]['endpoints']['rest']
rest_envar = os.environ.get(data['params'][LABEL]['variables']['rest'])

print(f"[ {LABEL} ]".upper())
if pivot_value == '' or pivot_value is None:
    data['params'][LABEL]['endpoints']['pivot'] = pivot_envar
    pivot_value = data['params'][LABEL]['endpoints']['pivot']
    print(f"  pivot address set as: {pivot_value}")
    update_yaml = True
if rest_value == '' or rest_value is None:
    if rest_envar is not None:
        data['params'][LABEL]['endpoints']['rest'] = rest_envar
        rest_value = data['params'][LABEL]['endpoints']['rest']
        print(f"  REST endpoint set as: {rest_value}")
        update_yaml = True
    else:
        rest_value = ''

if update_yaml:
    with open(CONFIG_YAML, 'w', encoding="utf-8") as y:
        y.write(yaml.dump(data, default_flow_style=False))
else:
    print(f"  pivot address: {pivot_value}")
    print(f"  REST endpoint: {rest_value}")
    press_any_key()
