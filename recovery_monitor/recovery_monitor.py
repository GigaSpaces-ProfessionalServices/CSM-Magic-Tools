#!/usr/bin/python3
# -*- coding: utf8 -*-

import os
import sys
import re
import uuid
import requests
import json
import yaml
from requests.packages.urllib3.exceptions import InsecureRequestWarning
import subprocess
import argparse
import time
import logging
from datetime import datetime
from colorama import Fore, Back, Style
import pyfiglet
import random
from math import floor
import itertools
import threading

def argument_parser():

    parser = argparse.ArgumentParser(
        description='description: \n   monitor the recovery process of space partitions',
        formatter_class=argparse.RawTextHelpFormatter
    )
    required = parser.add_argument_group('required arguments')
    required.add_argument(
        'space_name',
        action="store",
        help="The name of the space\n\n")
    parser.add_argument(
        '-l', '--list',
        action="store_true",
        help="List all registered types")
    parser.add_argument(
        '-u', '--unattended',
        action="store_true",
        help="Print table snapshot once - minimal output")
    parser.add_argument(
        '-i',
        action="store",
        dest="interval",
        help="Set polling interval in seconds (default = 5s)")
    parser.add_argument(
        '--debug',
        action="store_true",
        help="Print additional info")
    parser.add_argument(
        '-v', '--version',
        action='version', version='%(prog)s v2.0.4')

    the_arguments = {}
    ns = parser.parse_args()
    if ns.space_name:
        the_arguments['space_name'] = ns.space_name
    if ns.interval:
        the_arguments['interval'] = ns.interval
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


def is_backup_active():
    if os.environ['ENV_CONFIG'] is not None:
        with open(app_config, 'r', encoding='utf8') as appconf:
            for line in appconf:
                if re.search("app.tieredstorage.pu.backuprequired", line):
                    secure_flag = line.strip().replace('\n','').split('=')[1]
                    return secure_flag.casefold() == 'y'


def is_env_secured():
    if os.environ['ENV_CONFIG'] is not None:
        with open(app_config, 'r', encoding='utf8') as appconf:
            for line in appconf:
                if re.search("app.setup.profile", line):
                    secure_flag = line.strip().replace('\n','').split('=')[1]
                    return not secure_flag == '""'


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

 
def _test_get_cur_percent():
    the_file = "/tmp/testfile.txt"
    target_rows = 123
    count = 0
    with open(the_file) as fp:
        count = len(fp.readlines())
    if count == target_rows:
        return 100
    else:
        return int(count / target_rows * 100)


def check_space_exists(space):
    url = f"http://{manager}:{defualt_port}/v2/spaces"
    response_data = requests.get(url, auth=(auth['user'], auth['pass']), verify=False)
    for i in response_data.json():
        name = i['name']
        if space_name == name:
            return True
        else:
            return False


def is_primary(instance_id):
    """
    check if instance mode is primary
    """

    url = f"http://{manager}:{defualt_port}/v2/spaces/{space_name}/instances"
    headers = {'Accept': 'application/json'}
    response = requests.get(
        url,
        auth=(auth['user'], auth['pass']),
        headers=headers,
        verify=False
    )
    for item in response.json():
        if instance_id == item['id']:
            return item['mode'] == 'PRIMARY'


def check_type_exists():
    types = []
    for h in space_hosts:
        sh_cmd = f"ssh {h} 'for l in $(ls /dbagigadata/tiered-storage/{space_name}/*{space_name}); \
        do for t in $(sqlite3 $l \".tables\" | grep -v \"com.\");do echo $t ; done ; done'"
        the_response = subprocess.run([sh_cmd], shell=True, stdout=subprocess.PIPE).stdout.decode()
        data = the_response.replace('\n', ' ').split(' ')
        types.extend(data)
    _otypes = [t for t in list(set(types)) if t != '']
    return _otypes


def create_session_file(_file):
    lines = [
        '#!/bin/bash',
        'str=""',
        f'for db in $(ls /dbagigadata/tiered-storage/{space_name}/*{space_name}); do',
            'query=\'select (\'',
            'for t in $(sqlite3 $db "SELECT name FROM sqlite_master WHERE type = \'table\' AND name NOT LIKE \'com.%\';"); do',
                '[[ $t == "" ]] && continue',
                'query+="select count(*) from \\"${t}\\") + ("',
            'done',
            'if [[ $query != \'select (\' ]]; then',
                'query="$(echo $query | sed \'s/ + ($//g\')"',
                'c=$(sqlite3 $db "$query")',
            'else',
                 'c=0',
            'fi',
            'p=$(echo $db | grep -o "[0-9].*:" | cut -d: -f1)', 
            'str+="$(hostname):$p:$c "',
            'done',
        'echo $str',
        '',
    ]
    with open(_file, 'w', encoding='utf8') as tmp:
        tmp.writelines('\n'.join(lines))


def print_table_row(type, dl=[]):
    table_width = 116
    h_border = "=" * table_width
    r_border = "-" * table_width
    h1 = [
        'Part. #',
        'Expected Records',
        'Current Records',
        'Progress %',
        'Record Gap',
        'Status'
    ]
    h2 = ['Host', '# Records', 'Mode']
    if type == 'header':
        row = f"{h_border}\n|{h1[0]:^9}|" + \
            f"{h1[1]:^31}|{h1[2]:^31}|" + \
            f"{h1[3]:^12}|{h1[4]:^12}|{h1[5]:^14}|\n{h_border}\n"
        row += f"|{' ':^9}|" + \
            f"{h2[0]:^16}|{h2[1]:^9}|{h2[2]:^4}|" + \
            f"{h2[0]:^16}|{h2[1]:^9}|{h2[2]:^4}|" + \
            f"{'':^12}|{'':^12}|{'':^14}|\n{h_border}"
    if type == 'seperator':
        row = f"{h_border}"
    if type == 'row':
        if len(dl[1]) > 13:
            dl1 = f'{dl[1][0:13]}...'
        else:
            dl1 = dl[1]
        if len(dl[4]) > 13:
            dl4 = f'{dl[1][0:13]}...'
        else:
            dl4 = dl[4]
        row = f"|{dl[0]:^9}|" + \
            f"{dl1:^16}|{dl[2]:^9}|{dl[3]:^4}|" + \
            f"{dl4:^16}|{dl[5]:^9}|{dl[6]:^4}|" + \
            f"{dl[7]:^12}|{dl[8]:^12}|{dl[9]:<14}|"
        if debug:
            row += f"{dl[10]} ; {dl[11]} ; {dl[12]}"
    return row


def get_partition_counts(_partition_number, row_print=True):
    global table
    P = Back.BLUE + Fore.BLACK + " P  " + Style.RESET_ALL
    B = Back.LIGHTWHITE_EX + Fore.BLACK + " B  " + Style.RESET_ALL
    # primary instance
    _p_id = f"{space_name}~{_partition_number}_1"
    if str(_partition_number) not in partition_map:
        _p_count = 0
        _p_host_id = "NONE"
    else:
        _p_count = int(partition_map[str(_partition_number)][1])
        _p_host_id = partition_map[str(_partition_number)][0]
    # backup instance
    _b_id = f"{space_name}~{_partition_number}_2"
    if f"{str(_partition_number)}_1" not in partition_map.keys():
        _b_count = 0
        _b_host_id = "NONE"
    else: 
        _b_count = int(partition_map[f"{str(_partition_number)}_1"][1])
        _b_host_id = partition_map[f"{str(_partition_number)}_1"][0]
    
    if (_p_count < _b_count) or (_p_count == _b_count and is_primary(_b_id)):
        # if backup is the new primary we switch them
        _temp_id, _temp_count, _temp_host_id = _p_id, _p_count, _p_host_id
        _p_id, _p_count, _p_host_id = _b_id, _b_count, _b_host_id
        _b_id, _b_count, _b_host_id = _temp_id, _temp_count, _temp_host_id
    if _p_host_id == "NONE" or _b_host_id == "NONE":
        status = Fore.RED + "compromised   " + Style.RESET_ALL
    elif _p_count != _b_count:
        status = Fore.LIGHTYELLOW_EX + "in progress..." + Style.RESET_ALL
    elif _p_count == 0 and _b_count == 0:
        status = Fore.RED + "no data       " + Style.RESET_ALL
    else:
        status = Fore.GREEN + "synchronized  " + Style.RESET_ALL
    if row_print:
        if _p_host_id == "NONE" or _b_host_id == "NONE":
            progress_prct = "0%"
        elif _p_count > 0:
            progress_prct = f"{floor(_b_count / _p_count * 100)}%"
        else:
            progress_prct = "0%"
        progress_gap = _p_count - _b_count
        rdata = [
            _partition_number,
            _p_host_id,
            _p_count,
            P,
            _b_host_id,
            _b_count,
            B,
            progress_prct,
            progress_gap,
            status
        ]
        if debug:
            if _p_host_id == _b_host_id:
                ha_status = 'HA=broken'
            else:
                ha_status = ''
            if is_primary(_p_id):
                _p_id_str = f"P_ID:{_p_id.replace(f'{space_name}~',' ')}"
                _b_id_str = f"B_ID:{_b_id.replace(f'{space_name}~',' ')}"
            else:
                _p_id_str = f"P_ID:{_b_id.replace(f'{space_name}~',' ')}"
                _b_id_str = f"B_ID:{_p_id.replace(f'{space_name}~',' ')}"
            rdata.extend([_p_id_str, _b_id_str, ha_status])
        table += print_table_row('row', rdata) + '\n'
        rdata.clear()
    return (_p_count, _b_count)


class Spinner:

    def __init__(self, message, delay=0.1):
        self.spinner = itertools.cycle(['\\', '|', '-', '/'])
        self.delay = delay
        self.busy = False
        self.spinner_visible = False
        sys.stdout.write(message)

    def write_next(self):
        with self._screen_lock:
            if not self.spinner_visible:
                sys.stdout.write(next(self.spinner))
                self.spinner_visible = True
                sys.stdout.flush()

    def remove_spinner(self, cleanup=False):
        with self._screen_lock:
            if self.spinner_visible:
                sys.stdout.write('\b')
                self.spinner_visible = False
                if cleanup:
                    sys.stdout.write(' ')       # overwrite spinner with blank
                    sys.stdout.write('\r')      # move to next line
                sys.stdout.flush()

    def spinner_task(self):
        while self.busy:
            self.write_next()
            time.sleep(self.delay)
            self.remove_spinner()

    def __enter__(self):
        if sys.stdout.isatty():
            self._screen_lock = threading.Lock()
            self.busy = True
            self.thread = threading.Thread(target=self.spinner_task)
            self.thread.start()

    def __exit__(self, exception, value, tb):
        if sys.stdout.isatty():
            self.busy = False
            self.remove_spinner(cleanup=True)
        else:
            sys.stdout.write('\r')


def collect_data():
    global partition_map
    global table
    global p_count
    global b_count
    global p_total_count
    global b_total_count
    global exit_event
    table = ""
    partition_map = {}
    
    def get_sqlite_object_count(_lock, the_host,):
        if not exit_event:
            global partition_map
            sh_cmd = f'cat {temp_file} | ssh {the_host} bash -'
            the_response = subprocess.run(
                [sh_cmd],
                shell=True,
                stdout=subprocess.PIPE
            ).stdout.decode()
            data = the_response.replace('\n', '').split(' ')
            partitions = {}
            time.sleep(interval)
            with _lock:
                if data != ['']:
                    for item in data:
                        params = item.split(':')    # params = [HOST, PARTITION, COUNT]
                        partition_map[params[1]] = [params[0], int(params[2])]
    if not exit_event:
        threads = [
            threading.Thread(
                target=get_sqlite_object_count,
                args=(lock, h)
                ) for h in space_hosts
        ]
        for thread in threads:
            thread.start()
        for thread in threads:
            thread.join()

        # get number of partitions
        ha_enabled = is_backup_active()
        if ha_enabled:
            num_partitions = int(len(partition_map) / 2) + (len(partition_map) % 2)
        else:
            num_partitions = int(len(partition_map)) + (len(partition_map) % 2)

        table += print_table_row('header', "") + '\n'
        p_total_count, b_total_count = 0, 0
        for p_num in range(1, num_partitions + 1):
            p_count, b_count = get_partition_counts(p_num)
            p_total_count += p_count
            b_total_count += b_count
        table += print_table_row('seperator', "")
        return True
    return False


# globals
gs_root = "/dbagiga"
hosts_config = f"{os.environ['ENV_CONFIG']}/host.yaml"
app_config = f"{os.environ['ENV_CONFIG']}/app.config"
defualt_port = 8090
partition_map = {}
temp_file = f'/tmp/recmon-{uuid.uuid4()}'

if __name__ == '__main__':
    # disable insecure request warning
    requests.packages.urllib3.disable_warnings(InsecureRequestWarning)
    arguments = argument_parser()
    
    if arguments:
        try:
            space_name = arguments['space_name']
            # check REST status and set operational manager
            with open(hosts_config, 'r') as y:
                hosts = yaml.safe_load(y)
            manager_hosts = hosts['servers']['manager']
            managers = []
            for mgr in manager_hosts.values():
                managers.append(mgr)
            # configure authentication
            auth = {}
            if is_env_secured():
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
            
            # check if space name is ok
            if not check_space_exists(space_name):
                print(f"ERROR: space name '{space_name}' could not be found.")
                exit(1)
            
            # get space hosts
            with open(hosts_config, 'r', encoding='utf8') as _hy:
                data = yaml.safe_load(_hy)
            space_hosts = [ h for h in data['servers']['space'].values()]

            # instantiate spinner
            spinner = Spinner
            
            # parse arguments
            if 'list' in arguments:
                with spinner("querying object types... ", delay=0.05):
                    otypes = check_type_exists()
                    total_str = f"Available types in space:"
                    subprocess.run(['clear'])
                    print(pyfiglet.figlet_format("Recovery Monitor"))
                    if len(otypes) > 0:
                        print(f"\n{total_str}\n" + "=" * (len(total_str) + 1))
                        for o in sorted(otypes):
                            print(o)
                    else:
                        print("No types found!")
                exit(0)
            
            # unattended flag
            unattended = 'unattended' in arguments
            
            if 'interval' in arguments:
                interval = float(arguments['interval'])
            else:
                interval = 5
            if interval < 0.1:
                print("interval cannot be less than 0.1")
                exit()
            
            # creating logger
            log_format = "%(asctime)s %(levelname)s %(message)s"
            log_file = "/dbagigalogs/sanity/ods_sanity.log"
            logging.basicConfig(filename=log_file,
                                filemode="a",
                                format=log_format,
                                datefmt='%Y-%m-%d %H:%M:%S',
                                level=logging.INFO)
            logger = logging.getLogger()

            # debug flag
            debug = 'debug' in arguments

            create_session_file(temp_file)
            lock = threading.Lock()
            exit_event = False
            if not unattended:
                subprocess.run(['clear'])
                with spinner("collecting recovery data... ", delay=0.1):
                    if not collect_data():
                        if not debug:
                            os.system('rm -f /tmp/recmon-*')
                        exit()
            else:
                if not collect_data():
                    if not debug:
                        os.system('rm -f /tmp/recmon-*')
                    exit()
            while True:
                if not unattended:
                    for _ in range(2): os.system('clear')
                for line in table.split('\n'):
                    if unattended:
                        logger.info(line)
                    print(line)
                p_total_display = f"{Fore.MAGENTA}{p_total_count:,}{Style.RESET_ALL}"
                b_total_display = f"{Fore.MAGENTA}{b_total_count:,}{Style.RESET_ALL}"
                print(f"Total number of records: PRIMARY = {p_total_display} | BACKUP = {b_total_display}")
                if unattended:
                    logging.shutdown()
                    break
                if not collect_data():
                    if not debug:
                        os.system('rm -f /tmp/recmon-*')
                    exit()
        except (KeyboardInterrupt, SystemExit):
            exit_event = True
            if not debug:
                os.system('rm -f /tmp/recmon-*')
            print('\n')
