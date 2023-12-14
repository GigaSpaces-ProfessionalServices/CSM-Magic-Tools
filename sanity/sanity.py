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
from glob import glob
from pathlib import Path


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
        
        def total_ram_count(self):
            the_url = self.url + f"/{space_name}/statistics/operations"
            response_data = requests.get(
                the_url, auth=(auth['user'], auth['pass']), headers=self.headers, verify=False).json()
            total_ram_objects = f"{response_data['objectCount']:,}"
            return total_ram_objects

        def total_ts_count(self):
            total_entries = 0
            the_url = f"http://{manager}:{defualt_port}/v2/internal/spaces/{space_name}/utilization"
            response_data = requests.get(
                the_url, auth=(auth['user'], auth['pass']), headers=self.headers, verify=False).json()
            if response_data["tiered"]:
                for o_name, o_attr in response_data['objectTypes'].items():
                    rule_type = "all"
                    if len(response_data['tieredConfiguration']) != 0:
                        rule_type = response_data['tieredConfiguration'][o_name]['ruleType']
                    if verbose: print(f"{o_name:<40} | {o_attr['entries']:<9} | {rule_type}")
                    if rule_type != "RAM only":
                        total_entries += o_attr['entries']
            else:
                for o_name, o_attr in response_data['objectTypes'].items():
                    if verbose: print(f"{o_name:<40} | {o_attr['tieredEntries']:<9} | RAM only")
                    total_entries += o_attr['tieredEntries']
            if verbose: print()
            del response_data
            return f"{total_entries:,}"


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
    parser.add_argument('--verbose', action="store_true", help="increase script verbosity")
    parser.add_argument('-v', '--version', action='version', version='%(prog)s v1.7.1')

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
    if ns.verbose:
        the_arguments['verbose'] = True
    return the_arguments


def get_host_yaml_servers(_cluster):
    """
    Get DIH hosts from host.yaml
    :_cluster: name of cluster to filter
    :return: list of hosts
    """
    _hosts = []
    try:
        with open(host_yaml, 'r', encoding='utf8') as cfile:
            ydata = yaml.safe_load(cfile)
    except FileNotFoundError as e:
        print('[ERROR] File not found: ', e)
    except Exception as e:
        print('[ERROR] An error occurred: ', e)
    else:
        _hosts = [h for h in ydata['servers'][_cluster].values()]
    return _hosts


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


def check_connection(_server, _port, _timeout=5):
    check_port = 1
    a_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    a_socket.settimeout(_timeout)
    try:
        check_port = a_socket.connect_ex((_server, _port))
    except socket.error as socerr:
        print(f"[ERROR] caught exception: {socerr}")
    a_socket.settimeout(None)
    return check_port == 0


def get_auth(app_config):
    auth_params = {'user': '', 'pass': ''}
    if is_env_secured():
        f = open(app_config, 'r')
        for line in f:
            if re.search("app.manager.security.username", line):
                auth_params['user'] = line.strip().replace('\n','').split('=')[1]
            if re.search("app.manager.security.password", line):
                auth_params['pass'] = line.strip().replace('\n','').split('=')[1]
    return auth_params


def is_env_secured():
    with open(app_config, 'r', encoding='utf8') as appconf:
        for line in appconf:
            if re.search("app.setup.profile", line):
                secure_flag = line.strip().replace('\n','').split('=')[1]
                if secure_flag == '""':
                    return False
                return True


def is_backup_active():
    if os.environ['ENV_CONFIG'] is not None:
        with open(app_config, 'r', encoding='utf8') as appconf:
            for line in appconf:
                if re.search("app.tieredstorage.pu.backuprequired", line):
                    backup_active = line.strip().replace('\n','').split('=')[1]
                    return backup_active.casefold() == 'y'


def get_nb_domain():
    with open(nb_conf_template, 'r', encoding='utf8') as nbconf:
        for line in nbconf:
            if re.search("NB_DOMAIN=", line):
                value = line.strip().replace('\n','').split('=')[1].replace('"','')
                return value


def get_service_space_from_nb(the_service_name):
    # get northbound host
    sh_cmd = "/dbagiga/utils/runall/runall.sh -na -l | grep -v '===' | head -1"
    the_response = str(subprocess.run([sh_cmd], shell=True, stdout=subprocess.PIPE).stdout)
    nb_host = the_response.strip("\\n'").strip("b'")
    nb_hosts = get_host_yaml_servers('nb_applicative')
    if len(nb_hosts) > 0:
        port = 22
        service_active = False
        for nb_host in nb_hosts:
            if check_connection(nb_host, port):
                service_active = True
                break
        if not service_active:
            print(f"ERROR: unable to connect to any NB app server on port {port}")
            logger.error(f"[IIDR] unable to connect to any DI server on port {port}")
    ms_conf = "/etc/nginx/conf.d/microservices.conf"
    sh_cmd = "ssh " + nb_host + " cat " + ms_conf + \
             " | sed -n '/upstream " + the_service_name + "/,/server/p' | grep -Po '(?<=server).*?(?=max)'"
    return str(subprocess.run([sh_cmd], shell=True, stdout=subprocess.PIPE).stdout.decode()).split(':')


def show_grid_info(_step=None):
    try:
        if interactive_mode:
            os.system('clear')
            print(pyfiglet.figlet_format("     Sanity", font='slant'))
        else:
            print('\n' * 3)
        _title = f'-- [ STEP {_step} ] --- DIH INFORMATION '
        print_title(_title)
        logger = logging.getLogger()
        # display DIH grid information
        the_info = osg.info()
        for key, val in the_info.items():
            print(f"{key:<14}: {val}")
            logger.info(f"{key:<14}: {val}")
        spaces_servers = get_host_yaml_servers('space')
        print(f"{'space servers':<14}: {spaces_servers}")
        print(f"{'partitions':<14}: {osg.Space.partition_count()}")
        print(f"{'space name':<14}: {space_name}")
        print('\n')
        show_total_objects()
        logging.shutdown()
    except (KeyboardInterrupt, SystemExit):
        print("\n")
        exit(1)


def show_pu_status(_step=None):
    try:
        if interactive_mode:
            os.system('clear')
            print(pyfiglet.figlet_format("     Sanity", font='slant'))
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
    except (KeyboardInterrupt, SystemExit):
        print("\n")
        exit(1)


def run_services_polling(_service_name=None, _step=None):
    '''
    function reads lines from microservices/curls file
    and executes a CURL command for each line
    '''
    try:
        logger = logging.getLogger()
        if interactive_mode:
            os.system('clear')
            print(pyfiglet.figlet_format("     Sanity", font='slant'))
        else:
            print('\n' * 3)
        _title = f'-- [ STEP {_step} ] --- DIGITAL SERVICES POLLING '
        print_title(_title)
        if not os.path.exists(ms_config):
            print(f"service polling is unavailable. configuration data required ({ms_config})")
            logger.info(f"service polling unavailable. configuration data required ({ms_config})")
            logging.shutdown()
            return
        colorama.init(autoreset=True)
        svc_status_print = f"{f'{Fore.RED}Failed':<20}"  + u'[\u2717]'
        svc_status = 'Failed'
        _timeout = 3
        with open(ms_config, 'r') as r:
            _lines = r.readlines()
            for _l in _lines:
                l = _l.strip()
                if l == '': continue
                _this_service_name = l.split('?')[0].split('/')[3]
                if _service_name != None and _this_service_name != _service_name:
                    continue
                if is_env_secured():
                    cmd = f'curl -sL -u "{auth["user"]}:{auth["pass"]}" --max-time {_timeout} \
                        --key {key_file} --cert {cert_file} --cacert {ca_file} "{l}"'
                else:
                    cmd = f'curl -sL --max-time {_timeout} \
                        --key {key_file} --cert {cert_file} --cacert {ca_file} "{l}"'
                _response = subprocess.run(cmd, shell=True, stdout=subprocess.PIPE).stdout.decode()
                if verbose:
                    print(f"curl line = {l}")
                    print(f"service name = {_this_service_name}")
                    print(f"cmd = {cmd}")
                    print(f"response = {_response}")
                print_line = f"polling service '{_this_service_name}':"
                if f"x{_response}x" == 'xx':
                    svc_status = 'No Data'
                    svc_status_print = f"{Fore.RED + svc_status:<20}" + u'[\u2717]'
                elif re.search("404", _response):
                    svc_status = 'Undeployed'
                    svc_status_print = f"{Fore.RED + svc_status:<20}" + u'[\u2717]'
                elif re.search("500 Internal Server Error", _response):
                    svc_status = 'Bad Request'
                    svc_status_print = f"{Fore.RED + svc_status:<20}" + u'[\u2717]'
                else:
                    _response = json.loads(_response)
                    if len(_response["res"]) != 0:
                        svc_status = 'Successful'
                        svc_status_print = f"{Fore.GREEN + svc_status:<20}" + u'[\u2713]'
                print(f"{print_line:<70} {svc_status_print}")
                logger.info(f"{print_line:<70} {svc_status}")
        logging.shutdown()
    except (KeyboardInterrupt, SystemExit):
        print("\n")
        exit(1)


def show_cdc_status(_step=None):
    try:
        if interactive_mode:
            os.system('clear')
            print(pyfiglet.figlet_format("     Sanity", font='slant'))
        else:
            print('\n' * 3)
        _title = f'-- [ STEP {_step} ] --- CDC PIPELINES '
        print_title(_title)
        #show_iidr_subscriptions()
        show_di_pipeline_info()
        #shob_update()
    except (KeyboardInterrupt, SystemExit):
        print("\n")
        exit(1)
    

def show_iidr_subscriptions(_step=None):
    print('\n-- [ IIDR SUBSCRIPTIONS ]')
    servers = get_host_yaml_servers('cdc')
    if len(servers) > 0:
        logger = logging.getLogger()
        port = 10101
        user = "gsods"
        service_active = False
        for server in servers:
            if check_connection(server, port):
                service_active = True
                break
        if not service_active:
            print(f"ERROR: unable to connect to any DI server on port {port}")
            logger.error(f"[IIDR] unable to connect to any DI server on port {port}")
        else:
            #as_home = f"/home/{user}/iidr_cdc/as"
            as_home = f"/giga/iidr/as/bin/chcclp"
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
    else:
        print("[ERROR] dataIntegration servers not found.")
        logger.error("[ERROR] dataIntegration servers not found.")
    logging.shutdown()


def show_di_pipeline_info():

    def get_pipeline_name(pipeline):
        return pipeline.get('name')

    servers = get_host_yaml_servers('dataIntegration')
    if len(servers) > 0:
        logger = logging.getLogger()
        port = 6080
        port_ok, service_ok = False, False
        for server in servers:
            if check_connection(server, port, 3):
                port_ok = True
                break
        if not port_ok:
            print(f"ERROR: unable to connect to DI server(s) on port {port}.")
            logger.error("[IIDR] unable to connect to DI server(s) on port {port}.")
        else:
            the_url = f'http://{server}:6080/api/v1/pipeline/'
            response_data = requests.get(the_url, auth=(auth['user'], auth['pass']), verify=False).json()
            if len(response_data) != 0 and type(response_data).__name__ == 'list':
                response_data.sort(key=get_pipeline_name)   # sorted list by pipeline name
                service_ok = True
            if service_ok:
                for p in response_data:
                    print(f"-- pipelineId: {p['pipelineId']}")
                    print(f"    name: {p['name']}")
                    print(f"    message: {p['message']}")
                    logger.info(f"pipelineId: {p['pipelineId']}")
                    logger.info(f"name: {p['name']}")
                    logger.info(f"message: {p['message']}")
                    print()
                    time.sleep(0.3)
            else:
                print(f"ERROR: unable to connect to API on DI server(s).")
                logger.error("[IIDR] unable to connect to API on DI server(s).")
    else:
        print("[ERROR] dataIntegration servers not found.")
        logger.error("[ERROR] dataIntegration servers not found.")
    logging.shutdown()


def shob_update():
    logger = logging.getLogger()
    print('\n-- [ SHOB STATUS ]')
    # set _env if nb management endpoint available
    # _env = f'{THIS_ENV}'.casefold()
    # if _env == 'TAUG': _env = 'garage'
    # if _env == 'TAUP': _env = 'lod-prd'
    # if _env == '': _env = 'tlv-prd'
    server = manager
    port = 8090
    the_base_url = f'https://{server}:{port}/v2/spaces/{space_name}'
    the_query = 'query?typeName=D2TBD201_SHOB_ODS&columns=D201_ODS_TIMESTAMP'
    the_url = f'{the_base_url}/{the_query}'
    the_headers = {'Content-Type': 'application/json'}
    if not check_connection(server, port):
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
    try:
        if interactive_mode:
            os.system('clear')
            print(pyfiglet.figlet_format("     Sanity", font='slant'))
        else:
            print('\n' * 3)
        _title = f'-- [ STEP {_step} ] --- DIH HARDWARE STATUS '
        print_title(_title)
        sh_cmd = f"{runall_exe} -hw.cpu-count -hw.cpu-load -hw.mem-count \
            -hw.capacity='/' -hw.capacity='/dbagiga' -hw.capacity='/dbagigalogs'"
        subprocess.call([sh_cmd], shell=True)
    except (KeyboardInterrupt, SystemExit):
        print("\n")
        exit(1)


def show_health_info(_step=None):
    try:
        if interactive_mode:
            os.system('clear')
            print(pyfiglet.figlet_format("     Sanity", font='slant'))
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
    except (KeyboardInterrupt, SystemExit):
        print("\n")
        exit(1)


def show_total_objects():
    logger = logging.getLogger()
    ram_count = osg.Space.total_ram_count()
    ts_count = osg.Space.total_ts_count()
    print(f"{'total number of objects in RAM:':<45}{ram_count}")
    print(f"{'total number of objects in Tiered Storage:':<45}{ts_count}")
    logger.info(f"total number of objects in RAM: {ram_count}")
    logger.info(f"total number of objects in Tiered Storage: {ts_count}")
    logging.shutdown()


def run_stress_test(_step=None):
    try:
        if interactive_mode:
            os.system('clear')
            print(pyfiglet.figlet_format("     Sanity", font='slant'))
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
    except (KeyboardInterrupt, SystemExit):
        print("\n")
        exit(1)


def show_recovery_report(script, _step=None):
    try:
        if interactive_mode:
            os.system('clear')
            print(pyfiglet.figlet_format("     Sanity", font='slant'))
        else:
            print('\n' * 3)
        _title = f'-- [ STEP {_step} ] --- PARTITIONS INTEGRITY REPORT '
        print_title(_title)
        sh_cmd = f"{script} {space_name} -u -i 0.1"
        subprocess.call([sh_cmd], shell=True)
    except (KeyboardInterrupt, SystemExit):
        print("\n")
        exit(1)


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


if __name__ == '__main__':

    ### globals ###

    # ENV_NAME
    if os.environ.get('ENV_NAME') is None:
        print("ERROR: missing ENV_NAME environment variable. cannot continue!")
        exit(1)
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
        exit(1)
    if os.path.exists(os.environ['ENV_CONFIG']):
        host_yaml = f"{os.environ['ENV_CONFIG']}/host.yaml"
        app_config = f"{os.environ['ENV_CONFIG']}/app.config"
        nb_conf_template = f"{os.environ['ENV_CONFIG']}/nb/applicative/nb.conf.template"
    elif os.path.exists(ENV_CONFIG_BACKUP):
        host_yaml = f"{ENV_CONFIG_BACKUP}/host.yaml"
        app_config = f"{ENV_CONFIG_BACKUP}/app.config"
        nb_conf_template = f"{ENV_CONFIG_BACKUP}//nb/applicative/nb.conf.template"
        print("(!) NFS mount is not accessible. using backup location for 'host.yaml' and 'app.config'.")
    else:
        print("ERROR: no 'host.yaml' and 'app.config' source is available. cannnot continue!")
        exit(1)

    gs_root = "/dbagiga"
    utils_dir = gs_root + "/utils"
    logs_root = "/dbagigalogs"
    runall_exe = utils_dir + "/runall/runall.sh"
    runall_conf = utils_dir + "/runall/runall.conf"
    recmon_script = f"{utils_dir}/recovery_monitor/recovery_monitor.py"
    defualt_port = 8090
    k6_test = f"{utils_dir}/sanity/run_k6.sh"
    ms_config = f"{gs_root}/microservices/curls"
    ssl_root = gs_root + "/ssl"

    # set display report width
    rw = 100

    proxy = {
        "http": "http://132.66.251.5:8080",
        "https": "http://132.66.251.5:8080"
    }

    try: 
        # creating logger
        if not os.path.exists(logs_root):
            print(f"logs root directory '{logs_root}' does not exist. aborting.")
            exit(1)
        else:
            logs_dir = f"{logs_root}/sanity"
            if not os.path.exists(logs_dir):
                os.makedirs(logs_dir)
        log_format = "%(asctime)s %(levelname)s %(message)s"
        log_file = f"{logs_dir}/{Path(__file__).stem}.log"
        logging.basicConfig(
            filename=log_file, 
            filemode="a", 
            format=log_format, 
            datefmt='%Y-%m-%d %H:%M:%S', 
            level=logging.INFO
            )
        logger = logging.getLogger()
        logger.info('Sanity started.')
        
        # disable insecure request warning
        requests.packages.urllib3.disable_warnings(InsecureRequestWarning)
        arguments = argument_parser()
        subprocess.run(['clear'])
        
        # present title
        print(pyfiglet.figlet_format("     Sanity", font='slant'))
        
        # check REST status and set operational manager
        managers = get_host_yaml_servers('manager')
        if len(managers) == 0:
            logger = logging.getLogger()
            print("[ERROR] manager servers not found. aborting!")
            logger.error("[ERROR] manager servers not found. aborting!")
            logging.shutdown()
            exit(1)
        
        # configure authentication
        auth = get_auth(f"{os.environ['ENV_CONFIG']}/app.config")
        
        # get REST available host
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
        verbose = False
        cycles_passed = 0
        total_cycles = 1
        
        ### parse arguments ###
        # check if 'space_name' is valid
        space_name = arguments['space_name']
        osg = OdsServiceGrid()
        
        # add sanity routines
        exec_funcs = [
            'show_grid_info',
            'show_pu_status',
            'run_services_polling',
            'show_cdc_status',
            'show_hardware_info',
            'show_health_info',
            #'run_stress_test',
            ]
        
        # if HA is active we add recovery monitor report step
        backup_active = is_backup_active()
        headers = {'Accept': 'application/json'}
        the_url = f"http://{manager}:{defualt_port}/v2/internal/spaces/{space_name}/utilization"
        response_data = requests.get(
            the_url, auth=(auth['user'], auth['pass']), headers=headers, verify=False).json()
        if backup_active and "tieredConfiguration" in response_data:
            exec_funcs.append('show_recovery_report')
        
        if not osg.Space.exist():
            print(f"space {space_name} does not exist!\n")
            logger.error(f"space {space_name} does not exist!")
            logger.info('Sanity complete.')
            logging.shutdown()
            exit(1)
        if 'verbose' in arguments:
            verbose = True
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
        
        # setup SSL certificates
        try:
            cert_file = glob(f"{ssl_root}/cert/*")[0]
        except:
            cert_file = ""
        try:
            key_file = glob(f"{ssl_root}/key/*")[0]
        except:
            key_file = ""
        try:
            ca_file = glob(f"{ssl_root}/ca/*")[0]
        except:
            ca_file = ""
    
        if verbose:
            print(f"cert_file = {cert_file}")
            print(f"key_file = {key_file}")
            print(f"ca_file = {ca_file}")
        
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
                        run_services_polling(service_name)
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
                        run_services_polling(service_name)
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
    except (KeyboardInterrupt, SystemExit):
        print("\n")
        exit
