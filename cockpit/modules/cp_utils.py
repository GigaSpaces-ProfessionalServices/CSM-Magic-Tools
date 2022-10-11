#!/usr/bin/python3
# *-* coding: utf-8 *-*

"""
cp_utils: collection of general functions
"""

import os
import subprocess
import socket
import json


def sort_tuples_list(_the_list):
    """
    sort a list of tuples by first key of tuple
    :param the_list: the list of tuples
    :return: the list of tuples
    """
    _the_list.sort(key = lambda x: x[0])
    return _the_list


def check_connection(_server, _port):
    """
    check connection to server on given port
    :param selections: the selections list
    :param dictionary: dictionary of menu items
    :return:
    """

    CONN_TIMEOUT = 1    # adjust value for connection test
    a_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    a_socket.settimeout(CONN_TIMEOUT)
    check_port = a_socket.connect_ex((_server, _port))
    a_socket.settimeout(None)
    return check_port == 0


def create_file(_data, _file):
    """
    create a file for _data
    :param _data: the lines to inject into _file
    :param _file: the file to create
    :return:
    """

    name = '.'.join(os.path.basename(_file).split('.')[:-1])
    extension = os.path.basename(_file).split('.')[-1:][0]
    if os.path.exists(_file):
        print(f"{name}.{extension} already exists. operation aborted")
    else:
        try:
            with open(_file, 'w', encoding="utf-8") as f_obj:
                f_obj.writelines('\n'.join(_data))
        except IOError as err:
            print(f"{name}.{extension} creation failed")
            print(f"{err}\n")
        else:
            print(f"{name}.{extension} created successfully\n")


def execute_command(_cmd, _title, indent=None):
    """
    execute subprocess command
    :param _cmd: command to execute of type list
    :param _title: print title for operation
    :param _indented: True/False for title indentation
    """
    if indent is None:
        print(f"{_title} ...", end=' ')
    else:
        indent_by = " " * indent
        print(f"{indent_by}{_title} ...", end=' ')
    try:
        result = subprocess.run(
            _cmd,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
            check=True
            )
    except subprocess.SubprocessError as err:
        print("failed")
        print(f"{err}\n")
    else:
        if result.returncode == 0:
            print("successful\n")
        else:
            print("failed\n")


def get_object_types_from_space(_yaml_data):
    """
    get object types from ods space
    :param _yaml_data: the data from yaml file
    :return: formatted dictionary as {key : [object_type, num_entries],}
    """

    types = []
    connections_ok = []
    for env_name in _yaml_data['params']:
        if env_name != 'cockpit':
            pivot = _yaml_data['params'][env_name]['endpoints']['pivot']
            exec_script = f"{os.environ['COCKPIT_HOME']}/scripts/get_space_objects.py"
            if check_connection(pivot, 22):
                connections_ok.append(True)
                cmd = f"cat {exec_script} | ssh {pivot} python3 -"
                response = subprocess.run(
                    [cmd],
                    shell=True,
                    check=True,
                    stdout=subprocess.PIPE
                    ).stdout.decode()
                response = json.loads(response.replace("\'", "\""))
                for k in response.keys():
                    if k != 'java.lang.Object':
                        types.append(k)
    if True in connections_ok:
        k = 1
        object_types = {}
        for the_type in set(types):
            value = [ the_type, response[the_type]['entries']]
            object_types[k] = value
            k += 1
    else:
        ### IF NO PIVOTS ARE ACCESSIBLE WE RETURN DUMMY OBJECTS ###
        object_types = {
            1: ['com.j_spaces.examples.benchmark.messages.MessagePOJO', 100000],
            2: ['com.j_spaces.examples.benchmark.messages.MessagePOJO1', 110000],
            3: ['com.j_spaces.examples.benchmark.messages.MessagePOJO2', 120000],
            4: ['com.j_spaces.examples.benchmark.messages.MessagePOJO3', 130000],
            5: ['com.j_spaces.examples.benchmark.messages.MessagePOJO4', 140000]
            }
    return object_types


def generate_job_file(_job_type, _env_name, _obj_type, _yaml_data):
    """
    create a file for a job
    :param _job_type: name of environment
    :param _env_name: name of environment
    :param _obj_type: name of target object
    :param _yaml_data: data from config yaml
    :return:
    """

    env_name_low = _env_name.lower()
    pivot = f"PIVOT_{_env_name}"
    jobs_home = f"{os.environ['COCKPIT_HOME']}/jobs"
    job_file_name = f"{_job_type}_{_env_name}_{_obj_type}.py".lower()
    job_file = f"{jobs_home}/{job_file_name}"
    pivot = _yaml_data['params'][env_name_low]['endpoints']['pivot']
    cmd = "cat {exec_script} | ssh " + pivot + " python3 -"
    sp_exec = 'subprocess.run([cmd], shell=True, stdout=subprocess.PIPE).stdout.decode()'
    lines = [
        '#!/usr/bin/python3\n\n',
        'import subprocess\n',
        f'exec_script = "{os.environ["COCKPIT_HOME"]}/scripts/get_space_objects.py"',
        f'cmd = f"{cmd}"',
        f'response = {sp_exec}',
        'print(response)\n\n'
    ]
    # create jobs home folder if not exists
    if not os.path.exists(jobs_home):
        try:
            os.makedirs(jobs_home)
        except OSError as err:
            print(err)
    with open(job_file, 'w', encoding="utf-8") as j:
        j.writelines('\n'.join(lines))
    # set execution bit for job file
    subprocess.run([f"chmod +x {job_file}"], shell=True, check=True)
