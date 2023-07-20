#!/usr/bin/python3
# -*- coding: utf8 -*-

import os
import sys
import requests
import json
import yaml
from requests.packages.urllib3.exceptions import InsecureRequestWarning
from signal import SIGINT, signal
import subprocess
import argparse
import re
import time
from datetime import datetime
import colorama
from colorama import Fore, Back, Style
import logging
import random
import itertools
import threading
import pyfiglet
import socket


def argument_parser():
    '''
    argument parser function
    :return: arguments object/dictionary
    '''
    
    parser = argparse.ArgumentParser(
        description='description: kill space instances duo - primary and backup',
        epilog='* please report any issue to alon.segal@gigaspaces.com'
    )
    parser = argparse.ArgumentParser()
    required = parser.add_argument_group('required arguments')
    required.add_argument('space_name', action="store", help="target the named space")
    parser.add_argument('-c', action="store", dest="cycles", help="set number of iterations to execute")
    parser.add_argument('-d', action="store", dest="duration", help="set duration (in seconds) for execution")
    parser.add_argument('--info', action="store_true", help="show general grid information")
    parser.add_argument('--status', action="store_true", help="get processing units state for all services")
    parser.add_argument('--stats', action="store_true", help="show the total number of objects in the space")
    parser.add_argument('--stress', action="store_true", help="run a stress test on nt2cr")
    parser.add_argument('--poll', action="store", dest="service", help="poll named service data")
    parser.add_argument('-v', '--version', action='version', version='%(prog)s v1.6.2')

    the_arguments = {}
    ns = parser.parse_args()
    if ns.space_name:
        the_arguments['space_name'] = ns.space_name
    if ns.cycles:
        the_arguments['cycles'] = ns.cycles
    if ns.duration:
        the_arguments['duration'] = ns.duration
    if ns.info:
        the_arguments['info'] = True
    if ns.status:
        the_arguments['status'] = True
    if ns.stats:
        the_arguments['stats'] = True
    if ns.stress:
        the_arguments['stress'] = True
    if ns.service:
        the_arguments['service'] = ns.service
    return the_arguments


def get_host_yaml_servers(_cluster):
    """
    Get ODS hosts from host.yaml
    :_cluster: name of cluster to filter
    :return: list of hosts
    """
    with open(host_yaml, 'r', encoding='utf8') as cfile:
        ydata = yaml.safe_load(cfile)
    _hosts = [h for h in ydata['servers'][_cluster].values()]
    return _hosts


def load_microservices():
    """
    Get microservices from config.json
    :return: list of microservices names
    """
    with open(ms_config, 'r', encoding='utf8') as cfile:
        ydata = json.load(cfile)
    for y in ydata:
        yield(y)


def blink(_string):
    return f"\033[30;5m{_string}\033[0m"


def is_restful_ok(the_url):
    """
    send REST GET query and get the response [200 = OK]
    :param the_url: url for GET request
    :return: True / False
    """
    
    try:
        the_response = requests.get(
            the_url,
            auth=(auth['user'], auth['pass']),
            verify=False,
            timeout=3
            )
    except requests.exceptions.RequestException as e:
        return False
    else:
        if the_response.status_code == 200:
            return True
        else:
            return False


def print_title(_string):
    line = _string + '-' * (rw - len(_string)) + '\n'
    colorama.init(autoreset=True)
    print(f"{Fore.BLUE}{Style.BRIGHT}{line}")


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


class OdsServiceGrid:

    def __init__(self):
        self.Space = self.Space()
        self.Instance = self.Instance()
        self.Host = self.Host()
        self.ProcessingUnit = self.ProcessingUnit()

    def info(self):
        self.headers = {'Accept': 'application/json'}
        self.url = f"http://{manager}:{defualt_port}/v2/info"
        response_data = requests.get(self.url, auth=(auth['user'], auth['pass']), headers=self.headers, verify=False)
        return response_data.json()

    class Host:

        def __init__(self):
            self.headers = {'Accept': 'application/json'}
            self.url = f"http://{manager}:{defualt_port}/v2/hosts"

        def list(self):
            response_data = requests.get(
                self.url, auth=(auth['user'], auth['pass']), headers=self.headers, verify=False)
            hosts = []
            for _h in response_data.json():
                hosts.append(_h['name'])
            return hosts

    class Space:

        def __init__(self):
            self.headers = {'Accept': 'application/json'}
            self.url = f"http://{manager}:{defualt_port}/v2/spaces"

        def exist(self):
            response_data = requests.get(
                self.url, auth=(auth['user'], auth['pass']), headers=self.headers, verify=False)
            for space in response_data.json():
                if space['name'] == space_name:
                    return True
            return False

        def partition_count(self):
            response_data = requests.get(
                self.url, auth=(auth['user'], auth['pass']), headers=self.headers, verify=False)
            for space in response_data.json():
                if space['name'] == space_name:
                    return space['topology']['partitions']
        
        def total_object_count(self):
            the_url = self.url + f"/{space_name}/statistics/operations"
            response_data = requests.get(
                the_url, auth=(auth['user'], auth['pass']), headers=self.headers, verify=False)
            total_ram_objects = f"{response_data.json()['objectCount']:,}"
            return total_ram_objects

        def total_write_count(self):
            the_url = self.url + f"/{space_name}/statistics/operations"
            response_data = requests.get(
                the_url, auth=(auth['user'], auth['pass']), headers=self.headers, verify=False)
            total_hdd_objects = f"{response_data.json()['writeCount']:,}"
            return total_hdd_objects

    class ProcessingUnit:

        def __init__(self):
            self.headers = {'Accept': 'application/json'}
            self.url = f"http://{manager}:{defualt_port}/v2/pus"

        def mode(self, pu_name):
            response_data = requests.get(
                self.url, auth=(auth['user'], auth['pass']), headers=self.headers, verify=False)
            for pu in response_data.json():
                if pu_name == pu['name']:
                    return pu['status']

        def list(self):
            response_data = requests.get(
                self.url, auth=(auth['user'], auth['pass']), headers=self.headers, verify=False)
            return response_data.json()

    class Instance:

        def __init__(self):
            self.headers = {'Accept': 'application/json'}
            self.url = f"http://{manager}:{defualt_port}/v2/spaces/{space_name}/instances"

        def get(self, the_instance_id):
            response_data = requests.get(
                self.url, auth=(auth['user'], auth['pass']), headers=self.headers, verify=False)
            for instance in response_data.json():
                if the_instance_id == instance['id']:
                    return instance
            return ""

        def mode(self, the_instance_id):
            response_data = requests.get(
                self.url, auth=(auth['user'], auth['pass']), headers=self.headers, verify=False)
            for instance in response_data.json():
                if the_instance_id == instance['id']:
                    return instance['mode']

        def kill_pid(self, hostname, pid):
            sh_cmd = f"ssh {hostname} 'kill -9 {pid}'"
            the_response = str(subprocess.run([sh_cmd], shell=True, stdout=subprocess.PIPE).stdout)
            sh_cmd = f"ssh {hostname} 'kill -s 0 {pid}' > /dev/null 2>&1 ; echo $?"
            the_response = str(subprocess.run([sh_cmd], shell=True, stdout=subprocess.PIPE).stdout)
            if int(the_response.strip("\\n'").strip("b'")) != 0:
                return True
            else:
                return False

        def show_entries_stats(self, hostname, pid):
            sh_cmd = f"ssh {hostname} 'ls /dbagigalogs/*{pid}*' | grep gigaspaces"
            the_log = str(subprocess.run([sh_cmd], shell=True, stdout=subprocess.PIPE)
                          .stdout).strip("\\n'").strip("b'")
            sh_cmd = f"ssh {hostname} \"cat {the_log} | egrep '(Entries|Total Time)'\""
            the_stats = str(subprocess.run([sh_cmd], shell=True, stdout=subprocess.PIPE).stdout)\
                .replace('\\n', '').strip("b' ").split('\\t')
            the_stats = [x for x in the_stats if x != '']
            return the_stats

        def show_instantiation_stats(self, hostname, pid):
            sh_cmd = f"ssh {hostname} 'ls /dbagigalogs/*{pid}*' | grep gigaspaces"
            the_log = str(subprocess.run([sh_cmd], shell=True, stdout=subprocess.PIPE)
                          .stdout).strip("\\n'").strip("b'")
            sh_cmd = f"ssh {hostname} \"cat {the_log} | grep 'Instantiated' | cut -d- -f4 | sed 's/ //'\""
            the_stats = str(subprocess.run([sh_cmd], shell=True, stdout=subprocess.PIPE).stdout)\
                .strip("b' ").strip("\\n\\t")
            return the_stats

        def count_objects(self, the_instance_id):
            the_url = self.url + f"/{the_instance_id}/statistics/operations"
            response_data = requests.get(the_url, auth=(auth['user'], auth['pass']), headers=self.headers, verify=False)
            return response_data.json()['objectCount']
        
        def exist(self, the_instance_id):
            the_url = self.url + f"/{the_instance_id}"
            response_data = requests.get(
                the_url, auth=(auth['user'], auth['pass']), headers=self.headers, verify=False)
            if response_data.status_code == 200:
                return True
            else:
                return False


def get_auth(host):
    auth_params = {}
    if THIS_ENV.upper() in ['PRD', 'DR']:
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


def is_env_secured():
    with open(app_config, 'r', encoding='utf8') as appconf:
        for line in appconf:
            if re.search("app.setup.profile", line):
                secure_flag = line.strip().replace('\n','').split('=')[1]
                if secure_flag == '""':
                    return False
                return True


def test_microservice_e2e(_the_service_name, _the_host, _the_port, _the_json):
    the_data = _the_json['data']
    the_headers = _the_json['headers']
    _debug_ = False
    if _debug_:
        print(f"\n\n{_the_service_name}".upper() + f" :: http://{_the_host}:{_the_port}/v1/u1")
    the_url = f"http://{_the_host}:{_the_port}/v1/u1"
    #the_url = "https://odsgs-app-stg.hq.il.bleumi:8443/nt2cr/v1/u1"
    
    if _the_json['method'].lower() == "json":
        response = requests.get(
            the_url,
            json=the_data,
            auth=(auth['user'], auth['pass']),
            headers=the_headers
        )
    elif _the_json['method'].lower() == "params":
        response = requests.get(
            the_url,
            params=the_data,
            auth=(auth['user'], auth['pass']),
            headers=the_headers
        )
    if _debug_:
        print(response.json())
    return response.status_code


def get_service_space_from_nb(the_service_name):
    # get northbound host
    sh_cmd = "/dbagiga/utils/runall/runall.sh -na -l | grep -v '===' | head -1"
    the_response = str(subprocess.run([sh_cmd], shell=True, stdout=subprocess.PIPE).stdout)
    nb_host = the_response.strip("\\n'").strip("b'")
    ms_conf = "/etc/nginx/conf.d/microservices.conf"
    sh_cmd = "ssh " + nb_host + " cat " + ms_conf + \
             " | sed -n '/upstream " + the_service_name + "/,/server/p' | grep -Po '(?<=server).*?(?=max)'"
    the_response = str(subprocess.run([sh_cmd], shell=True, stdout=subprocess.PIPE).stdout)
    return the_response.strip("b' \\n").split(':')


def show_grid_info(_step=None):
    if interactive_mode:
        os.system('clear')
        print(pyfiglet.figlet_format("     ODS Sanity", font='slant'))
    else:
        print('\n' * 3)
    _title = f'-- [ STEP {_step} ] --- ODS INFORMATION '
    print_title(_title)
    logger = logging.getLogger()
    # display ODS grid information
    the_info = osg.info()
    for key, val in the_info.items():
        print(f"{key:<14}: {val}")
        logger.info(f"{key:<14}: {val}")
    spaces_servers = []
    hosts = osg.Host.list()
    for _h in hosts:
        if _h not in the_info['managers']:
            spaces_servers.append(_h)
    if len(spaces_servers) == 0:
        spaces_servers = the_info['managers']
    print(f"{'space servers':<14}: {spaces_servers}")
    print(f"{'partitions':<14}: {osg.Space.partition_count()}")
    print(f"{'space name':<14}: {space_name}")
    print('\n')
    oc = osg.Space.total_object_count()
    print(f"{'total number of objects in RAM:':<45}{oc}")
    wc = osg.Space.total_write_count()
    print(f"{'total number of objects in Tiered Storage:':<45}{wc}")
    logger.info(f"total number of objects in RAM: {oc}")
    logger.info(f"total number of objects in Tiered Storage: {wc}")
    logging.shutdown()


def show_pu_status(_step=None):
    if interactive_mode:
        os.system('clear')
        print(pyfiglet.figlet_format("     ODS Sanity", font='slant'))
    else:
        print('\n' * 3)
    _title = f'-- [ STEP {_step} ] --- OVERALL SERVICES STATUS '
    print_title(_title)
    logger = logging.getLogger()
    the_pu_list = osg.ProcessingUnit.list()
    colorama.init(autoreset=True)
    for pu in the_pu_list:
        time.sleep(0.1)
        the_status = str(pu['status']).upper()
        if the_status == "INTACT":
            print(f"{pu['name']:<70}{'status:':<10}{f'{Fore.GREEN}{the_status}':<20}" + u'[\u2713]')
        if the_status == "SCHEDULED":
            print(f"{pu['name']:<70}{'status:':<10}{f'{Fore.YELLOW}{the_status}':<20}")
        if the_status == "BROKEN":
            print(f"{pu['name']:<70}{'status:':<10}{f'{Fore.RED}{the_status}':<20}" + u'[\u2717]')
        logger.info(f"{pu['name']:<70} status: {pu['status']}")
    logging.shutdown()


def run_services_polling(_step=None):
    logger = logging.getLogger()
    if interactive_mode:
        os.system('clear')
        print(pyfiglet.figlet_format("     ODS Sanity", font='slant'))
    else:
        print('\n' * 3)
    _title = f'-- [ STEP {_step} ] --- DIGITAL SERVICES POLLING '
    print_title(_title)
    if ms_config_data is None:
        print(f"service polling is unavailable. configuration data required ({ms_config})")
        logger.info(f"service polling unavailable. configuration data required ({ms_config})")
        logging.shutdown()
        return
    for s in load_microservices():
        time.sleep(random.random())
        show_service_polling(s)
    logging.shutdown()


def show_service_polling(the_service_name):
    logger = logging.getLogger()
    if ms_config_data is None:
        print(f"service polling is unavailable. configuration data required ({ms_config})")
        logger.info(f"service polling unavailable. configuration data required ({ms_config})")
        logging.shutdown()
        return
    connection_params = get_service_space_from_nb(the_service_name)        
    colorama.init(autoreset=True)
    svc_status = f"{f'{Fore.RED}Failed':<20}"  + u'[\u2717]'
    svc_log_status = 'Failed'
    if connection_params == [''] or connection_params[0] == "127.0.0.1":
        print_line = f"connection to '{the_service_name}'"
    else:
        a_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        server = connection_params[0]
        port = int(connection_params[1])
        a_socket.settimeout(5)
        check_port = a_socket.connect_ex((server, port))
        a_socket.settimeout(None)
        if check_port == 0:
            try:
                response = test_microservice_e2e(the_service_name, connection_params[0], connection_params[1], ms_config_data[the_service_name])
            except:
                response = 444
        if response == 200:
            svc_status = f"{f'{Fore.GREEN}Successful':<20}"  + u'[\u2713]'
            svc_log_status = 'Successful'
        print_line = f"polling service '{the_service_name}':"
    print(f"{print_line:<70}{svc_status}")
    logger.info(f"{print_line:<70}{svc_log_status}")
    logging.shutdown()


def show_cdc_status(_step=None):
    if interactive_mode:
        os.system('clear')
        print(pyfiglet.figlet_format("     ODS Sanity", font='slant'))
    else:
        print('\n' * 3)
    _title = f'-- [ STEP {_step} ] --- CDC HEALTH AND FRESHNESS '
    print_title(_title)
    show_iidr_subscriptions()
    show_di_pipeline_info()
    shob_update()
    

def show_iidr_subscriptions(_step=None):
    logger = logging.getLogger()
    a_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    print('\n-- [ IIDR SUBSCRIPTIONS ]')
    servers = get_host_yaml_servers('dataIntegration')
    port = 10101
    user = "xxxxx"
    service_active = False
    for server in servers:
        a_socket.settimeout(5)
        check_port = a_socket.connect_ex((server, port))
        a_socket.settimeout(None)
        if check_port == 0:
            service_active = True
            break
    if not service_active:
        print(f"ERROR: unable to connect to any DI server on port {port}")
        logger.error(f"[IIDR] unable to connect to any DI server on port {port}")
    else:
        as_home = f"/home/{user}/iidr_cdc/as"
        monitor_home = "/dbagiga/di-iidr-watchdog"
        ss_file = "status_subscription.chcclp"
        exclude = "sed -n '/SUBSCRIPTION/,/Repl/p' | egrep -iv '(^$|Repl|---|Demo|LEUMI)' | sed 's/Inactive/Ready/g'"
        sh_cmd = f'ssh {server} "su - {user} -c \\"{as_home}/bin/chcclp -f {monitor_home}/{ss_file} | {exclude}\\""'
        response = subprocess.run([sh_cmd], shell=True, stdout=subprocess.PIPE).stdout.decode()
        if response == '':
            print(f"   no subscriptions found!")
        else:
            lnum = 1
            for line in response.splitlines():
                f = line.strip().split()
                if 'mirror' in f[1].lower():
                    print(f"   {f[0]:<15} | {f[1]} {f[2]}")
                    continue
                else:
                    print(f"   {f[0]:<15} | {f[1]:<30}")
                if lnum == 1: print("="*36)
                lnum += 1
    logging.shutdown()


def show_di_pipeline_info():
    logger = logging.getLogger()
    a_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    print('\n-- [ DI PIPELINES ]')
    servers = get_host_yaml_servers('dataIntegration')
    port = 6080
    port_ok, service_ok = False, False
    for server in servers:
        a_socket.settimeout(1)
        check_port = a_socket.connect_ex((server, port))
        a_socket.settimeout(None)
        if check_port == 0:
            port_ok = True
            break
    if not port_ok:
        print(f"ERROR: unable to connect to DI server(s) on port {port}.")
        logger.error("[IIDR] unable to connect to DI server(s) on port {port}.")
    else:
        try:
            for server in servers:
                url = f"http://{server}:{port}/api/v1/pipeline/"
                response_data = requests.get(
                    url,
                    auth=(auth['user'], auth['pass']),
                    verify=False
                ).json()
                if len(response_data) != 0:
                    if type(response_data).__name__ != 'list':
                        break   # if not a list then pipeline is not installed, we break
                    service_ok = True
                    break
        except:
            pass
        if service_ok:
            r = response_data[0]
            for k,v in r.items():
                key = f"{k}:"
                print(f"   {k:<18} {v}")
                logger.info(f"[IIDR] {k:<18} {v}")
        else:
            print(f"ERROR: unable to connect to API on DI server(s).")
            logger.error("[IIDR] unable to connect to API on DI server(s).")
    logging.shutdown()


def shob_update():
    logger = logging.getLogger()
    print('\n-- [ SHOB STATUS ]')
    _env = f'{THIS_ENV}'.lower()
    if _env == 'grg': _env = 'garage'
    if _env == 'prd': _env = 'lod-prd'
    if _env == 'dr': _env = 'tlv-prd'
    server = f"odsgs-mng-{_env}.hq.il.{DC}"
    port = 8090
    the_base_url = f'https://{server}:{port}/v2/spaces/{space_name}'
    the_query = 'query?typeName=D2TBD201_SHOB_ODS&columns=D201_ODS_TIMESTAMP'
    the_url = f'{the_base_url}/{the_query}'
    the_headers = {'Content-Type': 'application/json'}
    a_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    a_socket.settimeout(5)
    check_port = a_socket.connect_ex((server, port))
    a_socket.settimeout(None)
    if check_port != 0:
        print(f"ERROR: unable to establish a connection to '{server}'")
        logger.error(f"[SHOB] unable to establish a connection to '{server}'")
        logging.shutdown()
        return
    try:
        response = requests.get(the_url, auth=(auth['user'], auth['pass']), headers=the_headers, verify=False)
    except:
        print("ERROR: unable to retrieve timestamp!")
        logger.error("[SHOB] unable to retrieve timestamp")
    else:
        if response.status_code > 299:
            print("ERROR: unable to retrieve timestamp!")
            logger.error("[SHOB] unable to retrieve timestamp")
            logging.shutdown()
            return
        if len(response.json()['results']) > 0:
            _ts = response.json()['results'][0]['values'][0]
            d = _ts.split()[0].split('-')
            t = _ts.split()[1].split('.')[0].split(':')
            _ts_epoch = int(datetime(int(d[0]), int(d[1]), int(d[2]), int(t[0]), int(t[1]), int(t[2])).timestamp())
            time_diff = int(time.time()) - _ts_epoch
            _ts = f"{d[0]}-{d[1]}-{d[2]} {t[0]}:{t[1]}:{t[2]}"
            if time_diff < 65:
                _BG = Back.GREEN
            else:
                _BG = Back.RED
            _shob_status = f"{_BG}{'   Last updated:':<19}{_ts:<23}({time_diff} seconds ago)"
            print(f"{_shob_status:<70}{Style.RESET_ALL}")
        else:
            print("(!) unable to retrieve status.")
    logging.shutdown()


def show_hardware_info(_step=None):
    if interactive_mode:
        os.system('clear')
        print(pyfiglet.figlet_format("     ODS Sanity", font='slant'))
    else:
        print('\n' * 3)
    _title = f'-- [ STEP {_step} ] --- ODS HARDWARE STATUS '
    print_title(_title)
    sh_cmd = f"{runall_exe} -hw.cpu-count -hw.cpu-load -hw.mem-count \
        -hw.capacity='/' -hw.capacity='/dbagiga' -hw.capacity='/dbagigalogs'"
    subprocess.call([sh_cmd], shell=True)


def show_health_info(_step=None):
    if interactive_mode:
        os.system('clear')
        print(pyfiglet.figlet_format("     ODS Sanity", font='slant'))
    else:
        print('\n' * 3)
    _title = f'-- [ STEP {_step} ] --- MODULES CONNECTIVITY STATUS '
    print_title(_title)
    sh_cmd = f"{runall_exe} -a -hc -q"
    subprocess.call([sh_cmd], shell=True)
    sh_cmd = f"{runall_exe} -n -hc -q"
    subprocess.call([sh_cmd], shell=True)
    sh_cmd = f"{runall_exe} -p -hc -q"
    subprocess.call([sh_cmd], shell=True)


def show_total_objects():
    oc = osg.Space.total_object_count()
    print(f"{'total number of objects in RAM:':<45}{oc}")
    wc = osg.Space.total_write_count()
    print(f"{'total number of objects in Tiered Storage:':<45}{wc}")


def run_stress_test(_step=None):
    if interactive_mode:
        os.system('clear')
        print(pyfiglet.figlet_format("     ODS Sanity", font='slant'))
    else:
        print('\n' * 3)
    _title = f'-- [ STEP {_step} ] --- SERVICE LOAD TEST '
    print_title(_title)
    spinner = Spinner
    logger = logging.getLogger()
    rand_id = random.randrange(10000, 99999)
    report_file = f"{utils_dir}/sanity/k6-{rand_id}.out.report"
    subprocess.run(f"{k6_test} {rand_id} &", shell=True)
    print("(!) run '/dbagiga/utils/sanity/k6/k6control.py' in a separate terminal to monitor progress.")
    with spinner(f'Stress test in progress... ', delay=0.1):
        while not os.path.exists(report_file):
            time.sleep(1)
    print("Stress test completed successfully!\n")
    with open(report_file, 'r') as r:
        lines = r.readlines()
        colorama.init(autoreset=True)
        for line in lines:
            line = line.strip()
            logger.info(line)
            if "is status" in line:
                #if '✗' in line:
                if '\u2717' in line:
                    print(f"{Fore.RED}{Style.BRIGHT}{line}")
                else:
                    print(f"{Fore.GREEN}{Style.BRIGHT}{line}")
            elif "service:" in line:
                print(f"{Back.BLUE}{line}")
            elif "checks" in line:
                print(f"{Fore.BLUE}{Style.BRIGHT}{line}")
            elif "http_reqs" in line:
                print(f"{Fore.BLUE}{Style.BRIGHT}{line}")
            elif "vus_max" in line:
                print(f"{Fore.BLUE}{Style.BRIGHT}{line}")
            else:
                print(line)
    os.remove(report_file)
    logging.shutdown()


def show_recovery_report(script, _step=None):
    if interactive_mode:
        os.system('clear')
        print(pyfiglet.figlet_format("     ODS Sanity", font='slant'))
    else:
        print('\n' * 3)
    _title = f'-- [ STEP {_step} ] --- PARTITIONS INTEGRITY REPORT '
    print_title(_title)
    sh_cmd = f"{script} {space_name} -u -i 0.1"
    subprocess.call([sh_cmd], shell=True)



def run_sanity_routine():
    index = 1
    for func in exec_funcs:
        if func == 'show_recovery_report':
            exec(f"{func}(recmon_script, _step=f'{index} of {len(exec_funcs)}')")
        else:
            exec(f"{func}(_step=f'{index} of {len(exec_funcs)}')")
        index += 1
        if len(exec_funcs) == (index - 1):
            break
        if interactive_mode:
            input("\n\nPress Enter to continue...")


# globals
# ENV_NAME
if os.environ.get('ENV_NAME') is None:
    print("ERROR: missing ENV_NAME environment variable. cannot continue!")
    exit()
else:
    THIS_ENV = os.environ['ENV_NAME']
    if THIS_ENV in ('GRG', 'DEV'):
        DC = 'tleumi'
    elif THIS_ENV == 'STG':
        DC = 'bleumi'
    else:
        DC = 'leumi'

# ENV_CONFIG
ENV_CONFIG_BACKUP = "/dbagiga/env_config"
if os.environ.get('ENV_CONFIG') is None:
    print("ERROR: missing ENV_CONFIG environment variable. cannot continue!")
    exit()
if os.path.exists(os.environ['ENV_CONFIG']):
    host_yaml = f"{os.environ['ENV_CONFIG']}/host.yaml"
    app_config = f"{os.environ['ENV_CONFIG']}/app.config"
elif os.path.exists(ENV_CONFIG_BACKUP):
    host_yaml = f"{ENV_CONFIG_BACKUP}/host.yaml"
    app_config = f"{ENV_CONFIG_BACKUP}/app.config"
    print("(!) NFS mount is not accessible. using backup location for 'host.yaml' and 'app.config'.")
else:
    print("ERROR: no 'host.yaml' and 'app.config' source is available. cannnot continue!")
    exit()

gs_root = "/dbagiga"
utils_dir = gs_root + "/utils"
lib_dir = utils_dir + "/mega_loader/lib"
config_dir = utils_dir + "/mega_loader/config"
mega_loader_exec = utils_dir + "/mega_loader/mega_loader.py"
runall_exe = utils_dir + "/runall/runall.sh"
runall_conf = utils_dir + "/runall/runall.conf"
recmon_script = f"{utils_dir}/recovery_monitor/recovery_monitor.py"
defualt_port = 8090
k6_test = f"{utils_dir}/sanity/run_k6.sh"
ms_config = f"{gs_root}/microservices/config.json"

# set report width here 
rw = 100

### add routine functions here! ###
exec_funcs = [
    'show_grid_info',
    'show_pu_status',
    'run_services_polling',
    'show_cdc_status',
    'show_hardware_info',
    'show_health_info',
    'run_stress_test',
    'show_recovery_report',
    ]

if __name__ == '__main__':
    try: 
        # creating logger
        log_format = "%(asctime)s %(levelname)s %(message)s"
        log_file = "/dbagigalogs/sanity/ods_sanity.log"
        logging.basicConfig(filename=log_file,
                            filemode="a",
                            format=log_format,
                            datefmt='%Y-%m-%d %H:%M:%S',
                            level=logging.INFO)
        logger = logging.getLogger()
        logger.info('Sanity started.')
        # disable insecure request warning
        requests.packages.urllib3.disable_warnings(InsecureRequestWarning)
        arguments = argument_parser()
        if arguments:
            subprocess.run(['clear'])
            # present title
            print(pyfiglet.figlet_format("     ODS Sanity", font='slant'))
            # check REST status and set operational manager
            managers = get_host_yaml_servers('manager')
            # configure authentication
            auth = {}
            if is_env_secured():
                auth = get_auth(managers[0])
            else:
                auth['user'], auth['pass'] = '', ''
            rest_ok_hosts = []
            for mgr in managers:
                url = f'http://{mgr}:{defualt_port}/v2/spaces'
                if is_restful_ok(url):
                    rest_ok_hosts.append(mgr)
            if len(rest_ok_hosts) == 0:
                print('REST status: DOWN')
                logger.error('REST status: DOWN')
                logger.info('Sanity complete.')
                logging.shutdown()
                exit(1)
            else:
                # we use 1st host from rest_ok_hosts
                manager = rest_ok_hosts[0]
            # flags
            interactive_mode = False
            info = False
            cycles = False
            duration = False
            status = False
            stats = False
            stress = False
            polling = False
            cycles_passed = 0
            total_cycles = 1
            ### parse arguments ###
            # check if 'space_name' is valid
            space_name = arguments['space_name']
            osg = OdsServiceGrid()
            if not osg.Space.exist():
                print(f"space {space_name} does not exist!\n")
                logger.error(f"space {space_name} does not exist!")
                logger.info('Sanity complete.')
                logging.shutdown()
                exit(1)
            # load microservice config
            if os.path.exists(ms_config):
                with open(ms_config, 'r', encoding='utf8') as msc:
                    ms_config_data = json.load(msc)
            else:
                ms_config_data = None
            if 'cycles' in arguments:
                cycles = True
                total_cycles = int(arguments['cycles'])
            if 'duration' in arguments:
                duration = True
                time_passed = 0
                duration_sec = int(arguments['duration'])
            if 'info' in arguments:
                info = True
            if 'status' in arguments:
                status = True
            if 'stats' in arguments:
                stats = True
            if 'stress' in arguments:
                stress = True
            if 'service' in arguments:
                polling = True
                service_name = arguments['service']
            ### execute operations ###
            # if both cycles and duration requested we abort
            if cycles and duration:
                print("ERROR: count and duration are mutually exclusive. choose one or the other.\n")
                logger.info('Sanity complete.')
                logging.shutdown()
                exit(1)
            started = int(time.time())
            if info:
                show_grid_info()
                print('\n'*2)
                logger.info('Sanity complete.')
                logging.shutdown()
                exit(0)
            if polling:
                if duration:
                    while time_passed < duration_sec:
                        if service_name.lower() == 'all':
                            run_services_polling()
                        else:
                            show_service_polling(service_name)
                        print()
                        time_passed = int(time.time() - started)
                    logger.info('Sanity complete.')
                    logging.shutdown()
                    exit(0)
                else:
                    while cycles_passed < total_cycles:
                        if service_name.lower() == 'all':
                            run_services_polling()
                        else:
                            show_service_polling(service_name)
                        print()
                        cycles_passed += 1
                    logger.info('Sanity complete.')
                    logging.shutdown()
                    exit(0)
            if status:
                the_pu_list = osg.ProcessingUnit.list()
                if duration:
                    while time_passed < duration_sec:
                        show_pu_status()
                        time_passed = int(time.time() - started)
                    logger.info('Sanity complete.')
                    logging.shutdown()
                    exit(0)
                else:
                    while cycles_passed < total_cycles:
                        show_pu_status()
                        cycles_passed += 1
                    logger.info('Sanity complete.')
                    logging.shutdown()
                    exit(0)
            if stats:
                if duration:
                    while time_passed < duration_sec:
                        time.sleep(0.2)
                        show_total_objects()
                        time_passed = int(time.time() - started)
                    logger.info('Sanity complete.')
                    logging.shutdown()
                    exit(0)
                else:
                    while cycles_passed < total_cycles:
                        time.sleep(0.2)
                        show_total_objects()
                        cycles_passed += 1
                    logger.info('Sanity complete.')
                    logging.shutdown()
                    exit(0)
            if stress:
                if duration:
                    while time_passed < duration_sec:
                        time.sleep(0.2)
                        run_stress_test()
                        time_passed = int(time.time() - started)
                    logger.info('Sanity complete.')
                    logging.shutdown()
                    exit(0)
                else:
                    while cycles_passed < total_cycles:
                        time.sleep(0.2)
                        run_stress_test()
                        cycles_passed += 1
                    logger.info('Sanity complete.')
                    logging.shutdown()
                    exit(0)
            if duration:
                while time_passed < duration_sec:
                    run_sanity_routine()
                    time_passed = int(time.time() - started)
                logger.info('Sanity complete.')
                logging.shutdown()
                exit(0)
            if cycles:
                while cycles_passed < total_cycles:
                    run_sanity_routine()
                    cycles_passed += 1
                logger.info('Sanity complete.')
                logging.shutdown()
                exit(0)
            else:
                interactive_mode = True
                run_sanity_routine()
                logger.info('Sanity complete.')
                logging.shutdown()
                exit(0)
        else:
            print('\nmissing option(s). use [mega_loader.py -h] for help.\n')
            logger.error("missing option(s). use [mega_loader.py -h] for help.\n")
            logger.info('Sanity complete.')
            logging.shutdown()
            exit(1)
    except (KeyboardInterrupt, SystemExit):
        print("\n")
