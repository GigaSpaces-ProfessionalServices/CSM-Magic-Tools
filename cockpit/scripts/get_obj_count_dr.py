#!/usr/bin/python3
# *-* coding: utf-8 *-*

#
# get entry count for dr
#

import requests
from requests.packages.urllib3.exceptions import InsecureRequestWarning
import subprocess
import yaml
import os
from influxdb import InfluxDBClient
import datetime

def get_auth(host):
    auth_params = {}
    opt_user = "PassProps.UserName"
    opt_pass = "Password"
    cmd = f'/opt/CARKaim/sdk/clipasswordsdk GetPassword ' \
          f'-p AppDescs.AppID=APPODSUSERSBLLPRD ' \
          f'-p Query="Safe=AIMODSUSERSBLLPRD;Folder=;Object=ACCHQudkodsl;" -o'
    sh_cmd = f"ssh {host} '{cmd} {opt_user}'"
    the_response = str(subprocess.run([sh_cmd], shell=True, stdout=subprocess.PIPE).stdout)
    auth_params['user'] = the_response.strip("\\n'").strip("b'")
    sh_cmd = f"ssh {host} '{cmd} {opt_pass}'"
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


def inject_to_influx(data):
    # data = {env: 'prod/dr', type: 'the_object', count: num_of_entries}
    client = InfluxDBClient(host='localhost', port=8086)
    dbname = 'mydb'
    if dbname not in str(client.get_list_database()):
        client.create_database('mydb')
    else:
        client.switch_database('mydb')
    timestamp = (datetime.datetime.now()).strftime('%Y-%m-%dT%H:%M:%SZ')
    json_body = [
        {
            "measurement": "type_validation",
            "tags": {
                "env": data['env'],
                "obj_type": data['type']
            },
            "time": timestamp,
            "fields": {
                "count": data['count']
            }
        }
    ]
    client.write_points(json_body)


# main

endpoint = ""
defualt_port = 8090
host_file = f"{os.environ['ODSXARTIFACTS']}/odsx/host.yaml"

test = {'env': 'prod', 'type': 'POJO', 'count': 1000}

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

if endpoint == "":
    endpoint = manager

### execute operations ###
the_space = get_space()
total_entries = get_object_count()

#inject_to_influx(test)

print(total_entries)
