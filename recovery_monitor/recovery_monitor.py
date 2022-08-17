#!/usr/bin/python3
# -*- coding: utf8 -*-

import requests
import json
import yaml
from requests.packages.urllib3.exceptions import InsecureRequestWarning
import subprocess
import argparse
import time
from colorama import Fore, Back, Style
import random
import pyfiglet
from math import floor
import os


def argument_parser():

    parser = argparse.ArgumentParser(
        description='description: monitor recovery process',
        epilog='* please report any issue to alon.segal2@bankleumi.co.il'
    )
    parser = argparse.ArgumentParser()
    required = parser.add_argument_group('required arguments')
    required.add_argument('space_name', action="store", help="The name of the space")
    required.add_argument('-t', '--type', action="store", dest="type", help="Query a specific type")
    required.add_argument('-l', '--list', action="store_true", help="List all registered types")
    required.add_argument('-u', '--unattended', action="store_true", help="Show table only - minimal output")
    required.add_argument('--debug', action="store_true", help="Print additional info")
    parser.add_argument('-v', '--version', action='version', version='%(prog)s v1.0')

    the_arguments = {}
    ns = parser.parse_args()
    if ns.space_name:
        the_arguments['space_name'] = ns.space_name
    if ns.type:
        the_arguments['type'] = ns.type
    if ns.list:
        the_arguments['list'] = True
    if ns.unattended:
        the_arguments['unattended'] = True
    if ns.debug:
        the_arguments['debug'] = True
    return the_arguments


def get_auth(host):
    auth_params = {}
    if os.environ['ODSXARTIFACTS'].split('/')[2].upper() in ['PRD','DR']:
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
    setenv_file = "/dbagiga/gigaspaces-smart-ods/bin/setenv-overrides.sh"
    catch_str = 'Dcom.gs.security.enabled=true'
    sh_cmd = f"ssh {the_manager} 'cat {setenv_file} | grep {catch_str} > /dev/null 2>&1' ; echo $?"
    the_response = str(subprocess.run([sh_cmd], shell=True, stdout=subprocess.PIPE).stdout).strip("b' \\n")
    if int(the_response) == 0:
        return True
    else:
        return False


def is_restful_ok(the_url):
    """
    send REST GET query and get the response [200 = OK]
    :param the_url: url for GET request
    :return: True / False
    """
    try:
        the_response = requests.get(the_url, verify=False, timeout=3)
        if the_response.status_code == 200:
            return True
    except requests.exceptions.RequestException as e:
        return False

 
def get_cur_percent():
    the_file = "/tmp/testfile.txt"
    target_rows = 123
    count = 0
    with open(the_file) as fp:
        count = len(fp.readlines())
    if count == target_rows:
        return 100
    else:
        return int(count / target_rows * 100)


def get_sqlite_object_count(the_host, instance_id, otype=None):
    sqlite_cmd = f"sqlite3 /dbagigadata/tiered-storage/bllspace/sqlite_db_{space_name}_container{instance_id}:{space_name}"
    if 'type' in arguments:
        select_cmd = f"'select count(*) from {obj_type}'"
    else:
        select_cmd = f"'{build_sqlite_query(list_types())}'"
    remote_cmd = f'"{sqlite_cmd} {select_cmd}"'
    sh_cmd = f"ssh {the_host} {remote_cmd}"
    the_response = str(subprocess.run([sh_cmd], shell=True, stdout=subprocess.PIPE).stdout)
    return int(the_response.strip("\\n'").strip("b'"))


def check_space_exists(space):
    url = f"http://{manager}:{defualt_port}/v2/spaces"
    response_data = requests.get(url, auth=(auth['user'], auth['pass']), verify=False)
    for i in response_data.json():
        name = i['name']
        if space_name == name:
            return True
        else:
            return False


def check_type_exists(otype):
    uri=f"{space_name}~1_1/query?typeName={otype}"
    url = f"http://{manager}:{defualt_port}/v2/spaces/{space_name}/instances/{uri}"
    response_data = requests.get(url, auth=(auth['user'], auth['pass']), verify=False)
    if response_data.status_code == 200:
        return True
    else:
        return False


def list_types():
    url = f"http://{manager}:{defualt_port}/v2/spaces/{space_name}/instances/{space_name}~1_1/statistics/types"
    response_data = requests.get(url, auth=(auth['user'], auth['pass']), verify=False)
    types = []
    excluded = ['java.lang.Object']
    for _t in response_data.json():
        if _t in excluded:
            continue
        types.append(_t)
    return types


def print_types(types):
    counter = 0
    for t in types:
        print(t)
        counter += 1
    total_str = f"Total number of types available: {counter}"
    print("=" * (len(total_str) + 1) + f"\n{total_str}\n")


def check_sqlite_exists(the_host, instance_id):
    sqlite_file = f"/dbagigadata/tiered-storage/bllspace/sqlite_db_{space_name}_container{instance_id}:{space_name}"
    remote_cmd = f'[[ -e {sqlite_file} ]] && echo 0 || echo 1'
    sh_cmd = f"ssh {the_host} {remote_cmd}"
    the_response = str(subprocess.run([sh_cmd], shell=True, stdout=subprocess.PIPE).stdout)
    return int(the_response.strip("\\n'").strip("b'"))


def build_sqlite_query(types):
    query = "select ("
    for t in types:
        query += f"select count(*) from '{t}') + ("
    return query.rstrip(' + (')


def print_table_row(type, dl=[]):
    table_width = 112
    h_border = "=" * table_width
    r_border = "-" * table_width
    h1 = ['Partition #', 'Expected Records', 'Current Records', 'Progress %', 'Record Gap', 'Status']
    h2 = ['Host', '# Records', f"{Back.BLUE}{Fore.BLACK}P{Style.RESET_ALL}/{Back.LIGHTWHITE_EX}{Fore.BLACK}B{Style.RESET_ALL}"]
    if type == 'header':
        row = f"{h_border}\n|{h1[0]:^13}|{h1[1]:^27}|{h1[2]:^27}|{h1[3]:^12}|{h1[4]:^12}|{h1[5]:^14}|\n{h_border}\n"
        row += f"|{' ':^13}|{h2[0]:^13}|{h2[1]:^9}|{h2[2]:^3}|{h2[0]:^13}|{h2[1]:^9}|{h2[2]:^3}|{'':^12}|{'':^12}|{'':^14}|\n{h_border}"
    if type == 'seperator':
        row = f"{h_border}"
    if type == 'row':
        row = f"|{dl[0]:^13}|{dl[1]:^13}|{dl[2]:^9}|{dl[3]:^3}|{dl[4]:^13}|{dl[5]:^9}|{dl[6]:^3}|{dl[7]:^12}|{dl[8]:^12}|{dl[9]:<14}|"
        if debug:
            row += f"{dl[10]} ; {dl[11]}"
    print(row)
    

def get_sqlite_id(item_id):
    if item_id == f"{space_name}~{num}_1":
        return f"{num}"
    if item['id'] == f"{space_name}~{num}_2":
        return f"{num}_1"


# globals
gs_root = "/dbagiga"
odsx_hosts_config = f"{os.environ['ODSXARTIFACTS']}/odsx/host.yaml"
utils_dir = gs_root + "/utils"
runall_exe = utils_dir + "/runall/runall.sh"
runall_conf = utils_dir + "/runall/runall.conf"
defualt_port = 8090

if __name__ == '__main__':
    # disable insecure request warning
    requests.packages.urllib3.disable_warnings(InsecureRequestWarning)
    arguments = argument_parser()
    if arguments:
        space_name = arguments['space_name']
        # check REST status and set operational manager
        with open(odsx_hosts_config, 'r') as y:
            hosts = yaml.safe_load(y)
        manager_hosts = hosts['servers']['manager']
        managers = []
        for mgr in manager_hosts.values():
            managers.append(mgr)
        # configure authentication
        auth = {}
        if is_env_secured(managers[0]):
            auth = get_auth(managers[0])
        else:
            auth['user'], auth['pass'] = '', ''
        rest_ok_hosts = []
        for mgr in managers:
            url = f'http://{mgr}:{defualt_port}/v2/index.html'
            if is_restful_ok(url):
                rest_ok_hosts.append(mgr)
        if len(rest_ok_hosts) == 0:
            print('REST status: DOWN')
            exit(1)
        else:
            # we use 1st host from rest_ok_hosts
            manager = rest_ok_hosts[0]
        if 'unattended' in arguments:
            unattended = True
        else:
            unattended = False
            subprocess.run(['clear'])
            print(pyfiglet.figlet_format("Recovery Monitor"))
        # check if space name is ok
        if not check_space_exists(space_name):
            print(f"ERROR: space name '{space_name}' could not be found.")
            exit(1)
        if 'list' in arguments:
            print_types(list_types())
            exit(0)
        if 'type' in arguments:
            obj_type = arguments['type']
            # check if type is ok
            if not check_type_exists(obj_type):
                print(f"[ ERROR ]\nselected type '{obj_type}' does exist. Please choose one of the following available types:\n")
                print_types(list_types())
                exit(0)
            else:
                print(f"Genarating report for type: {obj_type}")
        if 'debug' in arguments:
            debug = True
        else:
            debug = False

        # print table header
        print_table_row('header', "")

        # get number of partitions in space
        headers = {'Accept': 'application/json'}
        url = f"http://{manager}:{defualt_port}/v2/spaces"
        response_data = requests.get(url, auth=(auth['user'], auth['pass']), headers=headers, verify=False)
        for space in response_data.json():
            if space['name'] == space_name:
                num_partitions = space['topology']['partitions']
        url = f"http://{manager}:{defualt_port}/v2/spaces/{space_name}/instances"
        response = requests.get(url, auth=(auth['user'], auth['pass']), headers=headers, verify=False)
        p_total_count, b_total_count = 0, 0
        for num in range(1,num_partitions + 1):
            # get instances params
            primary, backup = False, False
            P = Back.BLUE + Fore.BLACK + " P " + Style.RESET_ALL
            B = Back.LIGHTWHITE_EX + Fore.BLACK + " B " + Style.RESET_ALL
            for item in response.json():
                if f"{space_name}~{num}_" in item['id']:
                    if item['mode'] == 'PRIMARY':
                        p_id = item['id']
                        p_host_id = item['hostId']
                        p_sqlite_id = get_sqlite_id(item['id'])
                        if 'obj_type' in locals():
                            p_count = get_sqlite_object_count(p_host_id, p_sqlite_id, obj_type)
                            p_total_count += p_count
                        else:
                            p_count = get_sqlite_object_count(p_host_id, p_sqlite_id)
                            p_total_count += p_count
                        primary = True
                    if item['mode'] == 'BACKUP':
                        b_id = item['id']
                        b_host_id = item['hostId']
                        b_sqlite_id = get_sqlite_id(item['id'])
                        if 'obj_type' in locals():
                            b_count = get_sqlite_object_count(b_host_id, b_sqlite_id, obj_type)
                            b_total_count += b_count
                        else:
                            b_count = get_sqlite_object_count(b_host_id, b_sqlite_id)
                            b_total_count += b_count
                        backup = True
                if primary and backup:
                    break
            if not primary:
                p_id = 'NONE'
            if not backup:
                b_id = 'NONE'
            if p_count < b_count:
                progress_prct = f"{floor(p_count / b_count * 100)}%"
                progress_gap = b_count - p_count
                status = Fore.LIGHTYELLOW_EX + "In Progress..." + Style.RESET_ALL
                if debug:
                    rdata = [num, b_host_id, b_count, B, p_host_id, p_count, P, progress_prct, progress_gap,status, f"B_ID:{b_id}", f"P_ID:{p_id}"]
                else:
                    rdata = [num, b_host_id, b_count, B, p_host_id, p_count, P, progress_prct, progress_gap, status]
            else:
                if p_count > 0:
                    progress_prct = f"{floor(b_count / p_count * 100)}%"
                    progress_gap = p_count - b_count
                    if floor(b_count / p_count * 100) < 100:
                        status = Fore.YELLOW + "In Progress..." + Style.RESET_ALL
                    else:
                        status = Fore.GREEN + "Synchronized  " + Style.RESET_ALL
                else:
                    progress_prct = "100%"
                    progress_gap = 0
                    status = Fore.GREEN + "Synchronized  " + Style.RESET_ALL
                if debug:
                    rdata = [num, p_host_id, p_count, P, b_host_id, b_count, B, progress_prct, progress_gap, status, f"P_ID:{p_id}", f"B_ID:{b_id}"]
                else:
                    rdata = [num, p_host_id, p_count, P, b_host_id, b_count, B, progress_prct, progress_gap, status]
            print_table_row('row', rdata)
            rdata.clear()
        print_table_row('seperator', "")
        p_total_display = f"{Fore.MAGENTA}{p_total_count:,}{Style.RESET_ALL}"
        b_total_display = f"{Fore.MAGENTA}{b_total_count:,}{Style.RESET_ALL}"
        if 'obj_type' in locals():
            print(f"{'Total number of records in PRIMARY instances:':<46}{p_total_display:<10} (for type: '{obj_type}')")
            print(f"{'Total number of records in BACKUP instances:':<46}{b_total_display:<10} (for type: '{obj_type}')")
        else:
            print(f"{'Total number of records in PRIMARY instances:':<46}{p_total_display:<10}")
            print(f"{'Total number of records in BACKUP instances:':<46}{b_total_display:<10}")
exit(0)

