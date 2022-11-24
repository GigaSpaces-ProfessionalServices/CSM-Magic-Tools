#!/usr/bin/python3
# -*- coding: utf8 -*-

import os
import sys
import requests
import json
import yaml
from requests.packages.urllib3.exceptions import InsecureRequestWarning
import subprocess
import argparse
import re
import time
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
        epilog='* please report any issue to alon.segal2@bankleumi.co.il'
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
    parser.add_argument('-v', '--version', action='version', version='%(prog)s v1.4')

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
    :return: List of management hosts
    """
    
    with open(host_yaml, 'r', encoding='utf8') as cfile:
        ydata = yaml.safe_load(cfile)
    _hosts = [h for h in ydata['servers'][_cluster].values()]
    return _hosts


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
        response_data = requests.get(
            self.url,
            auth=(auth['user'], auth['pass']),
            headers=self.headers,
            verify=False
        )
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


def test_microservice_e2e(_the_host, _the_port, _the_json):
    """
    test a microservice by sending a REST GET query
    :param _the_host: url for the GET request
    :param _the_port: port for the GET request
    :param _the_json: data for the GET request
    :return: response status code
    """

    the_url = f"http://{_the_host}:{_the_port}/v1/u1"
    the_headers = {'Content-Type': 'application/json'}
    response = requests.get(
        the_url,
        json=_the_json,
        auth=(auth['user'], auth['pass']),
        headers=the_headers
    )
    return response.status_code


def get_service_space_from_nb(the_service_name):
    """
    get the space server hosting a specific service
    :param the_service_name: the name of the microservice
    :return: response status code
    """
    
    sh_cmd = "/dbagiga/utils/runall/runall.sh -na -l | grep -v '===' | head -1"
    the_response = str(subprocess.run([sh_cmd], shell=True, stdout=subprocess.PIPE).stdout)
    nb_host = the_response.strip("\\n'").strip("b'")
    microservices = "/etc/nginx/conf.d/microservices.conf"
    sh_cmd = "ssh " + nb_host + " cat " + microservices + \
             " | sed -n '/upstream " + the_service_name + "/,/server/p' | grep -Po '(?<=server).*?(?=max)'"
    the_response = str(subprocess.run([sh_cmd], shell=True, stdout=subprocess.PIPE).stdout)
    return the_response.strip("b' \\n").split(':')


def show_service_polling(the_service_name):
    print('#' * 80 + '\n' + '#' * 29 + ' [ SERVICE POLLING ] ' + '#' * 30 + '\n' + '#' * 80)
    logger = logging.getLogger()
    i, count = 1, 1
    if interactive_mode:
        count = 10
    while i <= count:
        connection_params = get_service_space_from_nb(the_service_name)        
        if connection_params == ['']:
            print("could not retrieve " + f"{the_service_name}".upper() + " connection parameters!")
            return
        if connection_params[0] == "127.0.0.1":
            print(f"connection to " + f"{the_service_name}".upper() + " service unavailable!")
            return
        colorama.init(autoreset=True)
        time.sleep(0.5)
        response = test_microservice_e2e(connection_params[0], connection_params[1], ms_config_data[the_service_name])
        if response == 200:
            svc_status = f"{Fore.GREEN}Successful"
            svc_log_status = 'Successful'
        else:
            svc_status = f"{Fore.RED}Failed"
            svc_log_status = 'Failed'
        print_line = f"polling service '{the_service_name}' {i}/{count}:"
        print(f"{print_line:<50}{svc_status}")
        logger.info(f"{print_line:<50}{svc_log_status}")
        i += 1
    logging.shutdown()


def show_pu_status():
    print('#' * 80 + '\n' + '#' * 29 + ' [ SERVICES STATUS ] ' + '#' * 30 + '\n' + '#' * 80)
    logger = logging.getLogger()
    the_pu_list = osg.ProcessingUnit.list()
    colorama.init(autoreset=True)
    for pu in the_pu_list:
        time.sleep(0.1)
        the_status = str(pu['status']).upper()
        if the_status == "INTACT":
            print(f"{pu['name']:<40} status: {Fore.GREEN}{the_status}")
        if the_status == "SCHEDULED":
            print(f"{pu['name']:<40} status: {Fore.YELLOW}{the_status}")
        if the_status == "BROKEN":
            print(f"{pu['name']:<40} status: {Fore.RED}{the_status}")
        logger.info(f"{pu['name']:<40} status: {pu['status']}")
    logging.shutdown()


def show_grid_info():
    print('#' * 80 + '\n' + '#' * 29 + ' [ GRID INFORMATION ] ' + '#' * 29 + '\n' + '#' * 80)
    logger = logging.getLogger()
    # display ODS grid information
    the_info = osg.info()
    for key, val in the_info.items():
        print(f"{key:<14}: {val}")
        logger.info(f"{key:<14}: {val}")
    print()
    print(f"{'space name':<14}: {space_name}")
    spaces_servers = []
    hosts = osg.Host.list()
    for _h in hosts:
        if _h not in the_info['managers']:
            spaces_servers.append(_h)
    if len(spaces_servers) == 0:
        spaces_servers = the_info['managers']
    print(f"{'space servers':<14}: {spaces_servers}")
    print(f"{'partitions':<14}: {osg.Space.partition_count()}")
    logging.shutdown()


def show_total_objects():
    print('#' * 80 + '\n' + '#' * 29 + ' [ ENTRIES IN SPACE ] ' + '#' * 29 + '\n' + '#' * 80)
    logger = logging.getLogger()
    oc = osg.Space.total_object_count()
    print(f"[ {space_name} ] {'total number of objects in RAM:':<34} {oc:<12}")
    wc = osg.Space.total_write_count()
    print(f"[ {space_name} ] {'total number of objects in TS:':<34} {wc:<12}")
    logger.info(f"[ {space_name} ] total number of objects in RAM: {oc}")
    logger.info(f"[ {space_name} ] total number of objects in TS: {wc}")
    logging.shutdown()


def show_hardware_info():
    print('#' * 80 + '\n' + '#' * 29 + ' [ HARDWARE REPORT ] ' + '#' * 30 + '\n' + '#' * 80)
    sh_cmd = f"{runall_exe} -hw.cpu-count -hw.cpu-load -hw.mem-count \
        -hw.capacity='/' -hw.capacity='/dbagiga' -hw.capacity='/dbagigalogs'"
    subprocess.call([sh_cmd], shell=True)


def show_health_info():
    print('#' * 80 + '\n' + '#' * 27 + ' [ HEALTH CHECK REPORT ] ' + '#' * 28 + '\n' + '#' * 80)
    sh_cmd = f"{runall_exe} -a -hc -q"
    subprocess.call([sh_cmd], shell=True)
    sh_cmd = f"{runall_exe} -n -hc -q"
    subprocess.call([sh_cmd], shell=True)
    sh_cmd = f"{runall_exe} -p -hc -q"
    subprocess.call([sh_cmd], shell=True)


def run_stress_test():
    spinner = Spinner
    logger = logging.getLogger()
    print('#' * 80 + '\n' + '#' * 31 + ' [ STRESS TEST ] ' + '#' * 32 + '\n' + '#' * 80)
    rand_id = random.randrange(10000, 99999)
    report_file = f"{utils_dir}/sanity/k6-{rand_id}.out.report"
    subprocess.run(f"{k6_test} {rand_id} &", shell=True)
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
                print(f"{Fore.GREEN}{Style.BRIGHT}{line}")
            elif "service:" in line:
                print(f"{Fore.RED}{Style.BRIGHT}{line}")
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


def show_recovery_report(script):
    sh_cmd = f"{script} {space_name} -u"
    subprocess.call([sh_cmd], shell=True)


def run_sanity_routine():
    show_grid_info()
    print('\n')
    if interactive_mode:
        input("Press Enter to continue...")
    show_total_objects()
    print('\n')
    if interactive_mode:
        input("Press Enter to continue...")
    show_pu_status()
    print('\n')
    if interactive_mode:
        input("Press Enter to continue...")
    for s in services:
        show_service_polling(s)
    print('\n')
    if interactive_mode:
        input("Press Enter to continue...")
    show_iidr_subscriptions()
    print('\n')
    if interactive_mode:
        input("Press Enter to continue...")
    show_di_pipeline_info()
    print('\n')
    if interactive_mode:
        input("Press Enter to continue...")
    show_hardware_info()
    print('\n')
    if interactive_mode:
        input("Press Enter to continue...")
    show_health_info()
    print('\n')
    if interactive_mode:
        input("Press Enter to continue...")
    run_stress_test()
    print('\n')
    if interactive_mode:
        input("Press Enter to continue...")
    show_recovery_report(recmon_script)
    print('\n')
    

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


def is_env_secured():
    if os.environ['ENV_CONFIG'] is not None:
        with open(app_config, 'r', encoding='utf8') as appconf:
            for line in appconf:
                if re.search("app.setup.profile", line):
                    secure_flag = line.strip().replace('\n','').split('=')[1]
                    if secure_flag == '""':
                        return False
                    return True


def show_di_pipeline_info():
    logger = logging.getLogger()
    a_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    print('#' * 80 + '\n' + '#' * 31 + ' [ DI PIPELINES ] ' + '#' * 31 + '\n' + '#' * 80)
    servers = get_host_yaml_servers('dataIntergation')
    port = 6080
    port_ok, service_ok = False, False
    for server in servers:
        a_socket.settimeout(5)
        check_port = a_socket.connect_ex((server, port))
        a_socket.settimeout(None)
        if check_port == 0:
            port_ok = True
            break
    if not port_ok:
        print(f"ERROR: unable to connect to DI server(s) on port {port}.")
        logger.error("[IIDR] unable to connect to DI server(s) on port {port}.")
    else:
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
        if service_ok:
            r = response_data[0]
            for k,v in r.items():
                key = f"{k}:"
                print(f"{k:<18} {v}")
                logger.info(f"[IIDR] {k:<18} {v}")
        else:
            print(f"ERROR: unable to connect to API on DI server(s).")
            logger.error("[IIDR] unable to connect to API on DI server(s).")
    logging.shutdown()


def show_iidr_subscriptions():
    logger = logging.getLogger()
    a_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    print('#' * 80 + '\n' + '#' * 28 + ' [ IIDR SUBSCRIPTIONS ] ' + '#' * 28 + '\n' + '#' * 80)
    servers = get_host_yaml_servers('dataIntergation')
    port = 10101
    user = "USERNAME"
    service_ok = False
    for server in servers:
        a_socket.settimeout(5)
        check_port = a_socket.connect_ex((server, port))
        a_socket.settimeout(None)
        if check_port == 0:
            service_ok = True
            break
    if not service_ok:
        print(f"ERROR: none of the DI server listens on port {port}")
        logger.error(f"[IIDR] none of the DI server listens on port {port}")
    else:
        as_home = f"/home/{user}/iidr_cdc/as"
        monitor_home = f"/home/{user}/iidr_cdc/iidr_monitor"
        ss_file = "status_subscription.chcclp"
        exclude = "sed -n '/SUBSCRIPTION/,/Repl/p' | egrep -v '(^$|Repl|---)'"
        sh_cmd = f'ssh {server} "su - {user} -c \\"{as_home}/bin/chcclp -f {monitor_home}/{ss_file} | {exclude}\\""'
        response = subprocess.run([sh_cmd], shell=True, stdout=subprocess.PIPE).stdout.decode()
        lnum = 1
        for line in response.splitlines():
            f = line.strip().split()
            if 'mirror' in f[1].lower():
                print(f"{f[0]:<15} | {f[1]} {f[2]}")
                continue
            else:
                print(f"{f[0]:<15} | {f[1]:<30}")
            if lnum == 1: print("="*36)
            lnum += 1
    logging.shutdown()


# globals
gs_root = "/dbagiga"
utils_dir = gs_root + "/utils"
runall_exe = utils_dir + "/runall/runall.sh"
runall_conf = utils_dir + "/runall/runall.conf"
host_yaml = f"{os.environ['ENV_CONFIG']}/host.yaml"
app_config = f"{os.environ['ENV_CONFIG']}/app.config"
recmon_script = f"{utils_dir}/recovery_monitor/recovery_monitor.py"
defualt_port = 8090
k6_test = f"{utils_dir}/sanity/run_k6.sh"
ms_config = f"{gs_root}/microservices/config.json"

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
                url = f'http://{mgr}:{defualt_port}/v2/index.html'
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
            # load microservices config
            with open(ms_config, 'r', encoding='utf8') as msc:
                ms_config_data = json.load(msc) 
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
                        show_service_polling(service_name)
                        print()
                        time_passed = int(time.time() - started)
                    logger.info('Sanity complete.')
                    logging.shutdown()
                    exit(0)
                else:
                    while cycles_passed < total_cycles:
                        show_service_polling(service_name)
                        print()
                        cycles_passed += 1
                    logger.info('Sanity complete.')
                    logging.shutdown()
                    exit(0)
            else:
                ### add additional services here! ###
                services = ['ms-digital-nt2cr']
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
        print('\nAborted!')