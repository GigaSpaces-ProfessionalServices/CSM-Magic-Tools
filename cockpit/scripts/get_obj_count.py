#!/usr/bin/python3
# *-* coding: utf-8 *-*


import requests
from requests.packages.urllib3.exceptions import InsecureRequestWarning
import subprocess
import argparse
import yaml
import os

def get_auth(host):
    auth_params = {}
    if os.environ['ODSXARTIFACTS'].split('/')[2].upper() in ['PRD', 'DR']:
        odsx_env = 'PRD'
    else:
        odsx_env = 'STG'
    opt_user = "PassProps.UserName"
    opt_pass = "Password"
    cmd = f'/opt/CARKaim/sdk/clipasswordsdk GetPassword ' \
          f'-p AppDescs.AppID=APPODSUSERSBLL{odsx_env} ' \
          f'-p Query="Safe=AIMODSUSERSBLL{odsx_env};Folder=;Object=ACCHQudkodsl;" -o PassProps.UserName'
    sh_cmd = f"ssh {host} '{cmd}'"
    the_response = str(subprocess.run([sh_cmd], shell=True, stdout=subprocess.PIPE).stdout)
    auth_params['user'] = the_response.strip("\\n'").strip("b'")
    cmd = f'/opt/CARKaim/sdk/clipasswordsdk GetPassword ' \
          f'-p AppDescs.AppID=APPODSUSERSBLL{odsx_env} ' \
          f'-p Query="Safe=AIMODSUSERSBLL{odsx_env};Folder=;Object=ACCHQudkodsl;" -o Password'
    sh_cmd = f"ssh {host} '{cmd}'"
    the_response = str(subprocess.run([sh_cmd], shell=True, stdout=subprocess.PIPE).stdout)
    auth_params['pass'] = the_response.strip("\\n'").strip("b'")
    return auth_params


def is_env_secured(the_manager):
    # setenv_file = "/dbagiga/gigaspaces-smart-ods/bin/setenv-overrides.sh"
    # catch_str = 'Dcom.gs.security.enabled=true'
    # sh_cmd = f"ssh {the_manager} 'cat {setenv_file} | grep {catch_str} > /dev/null 2>&1' ; echo $?"
    # the_response = str(subprocess.run([sh_cmd], shell=True, stdout=subprocess.PIPE).stdout).strip("b' \\n")
    # if int(the_response) == 0:
    #     return True
    # else:
    #     return False
    return False


def get_managers(host_file):
    with open(host_file, 'r') as hf:
        yd = yaml.safe_load(hf)
    return yd['servers']['manager']


def is_restful_ok(the_url):
    """
    send REST GET query and get the response [200 = OK]
    :param the_url: url for GET request
    :return: True / False
    """
    try:
        the_response = requests.get(the_url, auth=(auth['user'], auth['pass']), verify=False, timeout=3)
        if the_response.status_code == 200:
            return True
    except requests.exceptions.RequestException as e:
        return False


def get_space():
    url = f"http://{endpoint}:{defualt_port}/v2/spaces"
    headers = {'Accept': 'application/json'}
    response_data = requests.get(url, auth=(auth['user'], auth['pass']), headers=headers, verify=False)
    return response_data.json()[0]


def get_object_count():
    url = f"http://{endpoint}:{defualt_port}/v2/spaces/{the_space['name']}/statistics/types"
    headers = {'Accept': 'application/json'}
    response_data = requests.get(url, auth=(auth['user'], auth['pass']), headers=headers, verify=False)
    return response_data.json()

endpoint = "<SET ENDPOINT FOR REST API>"
defualt_port = 8090
host_file = f"{os.environ['ODSXARTIFACTS']}/odsx/host.yaml"


# main
# disable insecure request warning
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)

# check REST status and set operational manager
managers = get_managers(host_file)

# configure authentication
auth = {}
if is_env_secured(managers['host1']):
    auth = get_auth(managers['host1'])
else:
    auth['user'], auth['pass'] = '', ''

# configure manager
manager = ""
for m in managers.values():
    url = f'http://{m}:{defualt_port}/v2/index.html'
    if is_restful_ok(url):
        manager = m
        break
if manager == "":
    print('REST status: DOWN')
    exit(1)

#######################################
endpoint = manager
#######################################

### execute operations ###    
the_space = get_space()
total_entries = get_object_count()
print(total_entries)
