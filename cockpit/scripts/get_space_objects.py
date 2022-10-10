#!/usr/bin/python3
# *-* coding: utf-8 *-*

"""
get_space_objects: get objects' data from space
    *** executed remotely on the pivot(s) via ssh
"""

import os
import sys
import subprocess
import socket
import urllib3
import yaml
import requests

# import from requests to bypass requests ssl warnings
from urllib3.exceptions import InsecureRequestWarning

def check_connection(_server, _port):
    '''
    check connection to server on given port
    :param selections: the selections list
    :param dictionary: dictionary of menu items
    :return: True / False
    '''
    conn_timeout = 1    # adjust value for connection test
    a_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    a_socket.settimeout(conn_timeout)
    check_port = a_socket.connect_ex((_server, _port))
    a_socket.settimeout(None)
    return check_port == 0


def get_auth(_host):
    """
    get authentication parameters if available
    :param host: the host to connect
    :return: a dictionary of auth params
    """
    auth_params = {}
    opt_user = "PassProps.UserName"
    opt_pass = "Password"
    cmd = '/opt/CARKaim/sdk/clipasswordsdk GetPassword ' \
          '-p AppDescs.AppID=APPODSUSERSBLLPRD ' \
          '-p Query="Safe=AIMODSUSERSBLLPRD;Folder=;Object=ACCHQudkodsl;" -o'
    auth_params['user'] = subprocess.run(
        [f"ssh {_host} '{cmd} {opt_user}'"],
        shell=True,
        check=True,
        stdout=subprocess.PIPE
        ).stdout.decode().strip('\n')
    auth_params['pass'] = subprocess.run(
        [f"ssh {_host} '{cmd} {opt_pass}'"],
        shell=True,
        check=True,
        stdout=subprocess.PIPE
        ).stdout.decode().strip('\n')
    return auth_params


def is_env_secured(_manager):
    """
    determine if this environment is secured
    :param the_manager: the host to connect
    :return: True / False
    """
    setenv_file = "/dbagiga/gigaspaces-smart-ods/bin/setenv-overrides.sh"
    catch_str = 'Dcom.gs.security.enabled=true'
    if check_connection(_manager, 22):
        sh_cmd = f"ssh {_manager} 'cat {setenv_file} | \
            grep {catch_str} > /dev/null 2>&1' ; echo $?"
        the_response = int(subprocess.run(
            [sh_cmd],
            shell=True,
            check=True,
            stdout=subprocess.PIPE
            ).stdout.decode().strip('\n')
            )
        if the_response == 0:
            return True
    return False


def get_managers(_host_file):
    """
    get manager from host.yaml
    :param HOST_FILE: the host.yaml file
    :return: manager host
    """
    with open(_host_file, 'r', encoding="utf-8") as _yf:
        _data = yaml.safe_load(_yf)
    return _data['servers']['manager']


def is_restful_ok(_the_url):
    """
    send REST GET query and get the response [200 = OK]
    :param the_url: url for GET request
    :return: True / False
    """
    try:
        _the_response = requests.get(
            _the_url,
            auth=(auth['user'], auth['pass']),
            verify=False,
            timeout=3
            )
        return _the_response.status_code == 200
    except requests.exceptions.RequestException:
        return False


def get_space():
    """
    send REST GET query and get the space name
    :return: json object
    """
    _url = f"http://{ENDPOINT}:{REST_PORT}/v2/spaces"
    _headers = {'Accept': 'application/json'}
    _response_data = requests.get(
        _url,
        auth=(auth['user'], auth['pass']),
        headers=_headers,
        verify=False,
        timeout=10
        )
    return _response_data.json()


def get_object_count():
    """
    send REST GET query and get space objects and their respective number of entries
    :return: json object
    """
    _url = f"http://{ENDPOINT}:{REST_PORT}/v2/spaces/{the_space['name']}/statistics/types"
    _headers = {'Accept': 'application/json'}
    _response_data = requests.get(
        _url,
        auth=(auth['user'], auth['pass']),
        headers=_headers,
        verify=False,
        timeout=10
        )
    return _response_data.json()


### [ main ] ###

ENDPOINT = ""
REST_PORT = 8090
HOST_FILE = f"{os.environ['ODSXARTIFACTS']}/odsx/host.yaml"

# disable insecure request warning
urllib3.disable_warnings(InsecureRequestWarning)

# check REST status and set operational manager
managers = get_managers(HOST_FILE)

# configure authentication
auth = {}
if is_env_secured(managers['host1']):
    auth = get_auth(managers['host1'])
else: auth['user'], auth['pass'] = '', ''

# configure manager
manager = ""
for m in managers.values():
    url = f'http://{m}:{REST_PORT}/v2/index.html'
    if is_restful_ok(url):
        manager = m
        break
if manager == "":
    print('REST status: DOWN')
    sys.exit(1)
# configure ENDPOINT
if ENDPOINT == "":
    ENDPOINT = manager

### execute operations ###
s = get_space()
if len(s) == 0:
    print("ERROR: No space found")
else:
    the_space = s[0]
total_entries = get_object_count()

# print the results
print(total_entries)
