#!/usr/bin/python3
# *-* coding: utf-8 *-*

import os
import yaml

config_yaml = f"{os.path.dirname(os.path.abspath(__file__))}/../config/config.yaml"
label = 'dr'
pivot_env_var = 'PIVOT_DR'
label_print = label.upper()

# load yaml
with open(config_yaml, 'r') as o:
    data = yaml.safe_load(o)

update_yaml = False

if data['params'][label]['pivot_addr'] == '' or data['params'][label]['pivot_addr'] is None:
    data['params'][label]['pivot_addr'] = os.environ[pivot_env_var]
    print(f"\nupdated {label_print} pivot as: {data['params'][label]['pivot_addr']}")
    update_yaml = True
if data['params'][label]['REST_endpoint'] == '' or data['params'][label]['REST_endpoint'] is None:
    data['params'][label]['REST_endpoint'] = f'ENDPOINT_{label_print}'
    print(f"updated {label_print} REST endpoint as: {data['params'][label]['REST_endpoint']}")
    update_yaml = True

if update_yaml:
    with open(config_yaml, 'w') as n:
        n.write(yaml.dump(data, default_flow_style=False))
else:
    print(f"{label_print} pivot address: {data['params'][label]['pivot_addr']}")
    print(f"{label_print} REST address: {data['params'][label]['REST_endpoint']}")
    input("\nPress ENTER to continue")

