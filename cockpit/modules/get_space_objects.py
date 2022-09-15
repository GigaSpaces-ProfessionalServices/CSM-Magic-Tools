#!/usr/bin/python3
# *-* coding: utf-8 *-*

#
# get objects data from space
# this script is executed remotely on the pivot(s)
#

import requests
from requests.packages.urllib3.exceptions import InsecureRequestWarning
import subprocess
import yaml
import os
import socket


def check_connection(server, port):
    '''
    check connection to server on given port
    :param selections: the selections list
    :param dictionary: dictionary of menu items
    :return:
    '''
    conn_timeout = 1    # adjust value for connection test
    a_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    a_socket.settimeout(conn_timeout)
    check_port = a_socket.connect_ex((server, port))
    a_socket.settimeout(None)
    if check_port == 0:
        return True
    else:
        return False


def get_auth(host):
    auth_params = {}
    opt_user = "PassProps.UserName"
    opt_pass = "Password"
    cmd = f'/opt/CARKaim/sdk/clipasswordsdk GetPassword ' \
          f'-p AppDescs.AppID=APPODSUSERSBLLPRD ' \
          f'-p Query="Safe=AIMODSUSERSBLLPRD;Folder=;Object=ACCHQudkodsl;" -o'
    auth_params['user'] = subprocess.run([f"ssh {host} '{cmd} {opt_user}'"], \
        shell=True, stdout=subprocess.PIPE).stdout.decode().strip('\n')
    auth_params['pass'] = subprocess.run([f"ssh {host} '{cmd} {opt_pass}'"], \
        shell=True, stdout=subprocess.PIPE).stdout.decode().strip('\n')
    return auth_params


def is_env_secured(the_manager):
    setenv_file = "/dbagiga/gigaspaces-smart-ods/bin/setenv-overrides.sh"
    catch_str = 'Dcom.gs.security.enabled=true'
    if check_connection(the_manager, 22):
        sh_cmd = f"ssh {the_manager} 'cat {setenv_file} | grep {catch_str} > /dev/null 2>&1' ; echo $?"
        the_response = int(subprocess.run([sh_cmd], shell=True, stdout=subprocess.PIPE).stdout.decode().strip('\n'))
        if the_response == 0:
            return True
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
    return response_data.json()


def get_object_count():
    url = f"http://{endpoint}:{defualt_port}/v2/spaces/{the_space['name']}/statistics/types"
    headers = {'Accept': 'application/json'}
    response_data = requests.get(url, auth=(auth['user'], auth['pass']), headers=headers, verify=False)
    return response_data.json()


### [ main ] ###

endpoint = ""
defualt_port = 8090
host_file = f"{os.environ['ODSXARTIFACTS']}/odsx/host.yaml"

# disable insecure request warning
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)

# check REST status and set operational manager
managers = get_managers(host_file)

# configure authentication
auth = {}
if is_env_secured(managers['host1']): auth = get_auth(managers['host1'])
else: auth['user'], auth['pass'] = '', ''

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
# configure endpoint
if endpoint == "": endpoint = manager

### execute operations ###
s = get_space()
if len(s) == 0:
    print("ERROR: No space found")
else:
    the_space = s[0]
total_entries = get_object_count()

print(total_entries)
