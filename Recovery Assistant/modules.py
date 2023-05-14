#!/usr/bin/python3
# *-* coding: utf-8 *-*


import os
import sys
import threading
import itertools
import time, datetime
import sqlite3
from influxdb import InfluxDBClient
from sqlite3 import Error
import tty
import termios
from colorama import Fore, Style
import pyfiglet
from random import randint
import subprocess
import socket
import yaml


class Spinner:
    """
    spinner class to show spinner while executing something
    """
    def __init__(self, message, delay=0.1):
        self.spinner = itertools.cycle(['|', '/', '-', '\\'])
        self.delay = delay
        self.busy = False
        self.spinner_visible = False
        self._screen_lock = threading.Lock()
        self.thread = threading.Thread(target=self.spinner_task)
        sys.stdout.write(message)

    def write_next(self):
        """
        write the next char for spinner
        """
        with self._screen_lock:
            if not self.spinner_visible:
                sys.stdout.write(next(self.spinner))
                self.spinner_visible = True
                sys.stdout.flush()

    def remove_spinner(self, cleanup=False):
        """
        delete the spinner
        """
        with self._screen_lock:
            if self.spinner_visible:
                sys.stdout.write('\b')
                self.spinner_visible = False
                if cleanup:
                    sys.stdout.write(' ')       # overwrite spinner with blank
                    sys.stdout.write('\r')      # move to next line
                sys.stdout.flush()

    def spinner_task(self):
        """
        task routine: run -> wait -> remove
        """
        while self.busy:
            self.write_next()
            time.sleep(self.delay)
            self.remove_spinner()

    def __enter__(self):
        """
        context manager func
        """
        if sys.stdout.isatty():
            self.busy = True
            self.thread.start()

    def __exit__(self, _exception, _value, _tb):
        """
        context manager func
        """
        if sys.stdout.isatty():
            self.busy = False
            self.remove_spinner(cleanup=True)
        else:
            sys.stdout.write('\r')


class MySQLite:
    """
    custom SQLite class to allow working with sqlite3 database
    """
    def __init__(self, _db_file):
        self._db = _db_file
        self.home = os.path.dirname(os.path.realpath(_db_file))
        if not os.path.exists(self.home):
            try:
                os.makedirs(self.home)
            except OSError as err:
                if 'Errno 13' in str(err):
                    print(f"\n{err}\n *try changing the path in config.yaml")
                else:
                    print(err)
                sys.exit(1)

    def connect(self):
        """
        connect to database
        """
        _c = None
        try:
            _c = sqlite3.connect(self._db)
        except Error as err:
            print(err)
        return _c

    def create(self, _sql):
        """
        execute database create calls
        """
        try:
            _conn = self.connect()
            _c = _conn.cursor()
            _c.execute(_sql)
        except Error as err:
            print(err)

    def select(self, _sql):
        """
        execute database select calls
        """
        try:
            _conn = self.connect()
            _c = _conn.cursor()
            _c.execute(_sql)
        except Error as err:
            print(err)
            return None
        else:
            return _c.fetchall()

    def insert(self, _sql, _data):
        """
        execute database insert calls
        """
        try:
            _conn = self.connect()
            _c = _conn.cursor()
            _c.execute(_sql, _data)
        except Error as err:
            print(err)
            return None
        else:
            _conn.commit()
            return _c.lastrowid

    def delete(self, _sql):
        """
        execute database delete calls
        """
        try:
            _conn = self.connect()
            _c = _conn.cursor()
            _c.execute(_sql)
        except Error as err:
            print(err)
            return None
        else:
            _conn.commit()
            return _c.lastrowid


def write_to_influx(_dbname, _measurement, **data):
        """
        write data to influx database
        :param dbname: the name of the target database
        :param data: data payload kwargs as tags={the_tags}, fields={the_fields}
        :return:
        """
        client = InfluxDBClient(host='localhost', port=8086)
        if _dbname not in str(client.get_list_database()):
            client.create_database(_dbname)
        client.switch_database(_dbname)
        timestamp = (datetime.datetime.now()).strftime('%Y-%m-%dT%H:%M:%SZ')
        # construct json body for influx write
        json_body = {"measurement": _measurement, "time": timestamp, }
        json_body['tags'] = data['tags']        # the tags
        json_body['fields'] = data['fields']    # the fields
        json_body = [json_body]                 # wrapping as a list
        client.write_points(json_body)


def check_settings(config_yaml):
    """
    check settings of cockpit upon initialization
    """

    db_set_required = False
    env_set_required = False
     
    # load config yaml
    with open(config_yaml, 'r', encoding="utf-8") as yml:
        data = yaml.safe_load(yml)

    COCKPIT_DB_HOME = data['params']['cockpit']['db_home']
    COCKPIT_DB_NAME = data['params']['cockpit']['db_name']
    COCKPIT_DB = f"{COCKPIT_DB_HOME}/{COCKPIT_DB_NAME}"

    try:
        # check if environments are set in config.yaml
        for env_name in data['params']:
            if 'SET_ENV_NAME' in env_name:
                pretty_print('@:: cockpit configuration settings'.upper(), 'green', 'bright')
                pretty_print('\nERROR: environments are not set in config.yaml. Aborting!', 'red')
                sys.exit(0)
        # check cockpit database settings
        if COCKPIT_DB_HOME == '' or COCKPIT_DB_HOME is None \
            or COCKPIT_DB_NAME == '' or COCKPIT_DB_NAME is None:
            pretty_print("@:: cockpit db settings".upper(), 'green', 'bright')
            pretty_print('\nERROR: cockpit.db is not set in configuration file. Aborting!', 'red')
            sys.exit(1)
        if not os.path.exists(COCKPIT_DB):
            db_set_required = True
            pretty_print("@:: cockpit db settings".upper(), 'green', 'bright')
            pretty_print('\nERROR: Cockpit database was not found!', 'red')
            if get_user_ok("Would you like to create the cockpit database?"):
                try:
                    subprocess.run(
                    [f"{os.environ['COCKPIT_HOME']}/scripts/create_db.py"],
                    stdout=subprocess.DEVNULL,
                    stderr=subprocess.DEVNULL,
                    check=True
                    )
                except subprocess.SubprocessError as err:
                    print(err)
                if not os.path.exists(COCKPIT_DB):
                    sys.exit(1)
                print('\n')
            else:
                pretty_print('\nERROR: a cockpit database is required in order to run. '
                'Aborting!', 'red')
                sys.exit(1)

        # check cockpit enviroment settings
        for env_name in data['params']:
            if env_name != 'cockpit':
                pivot = data['params'][env_name]['endpoints']['pivot']
                if pivot == '' or pivot is None:
                    env_set_required = True
                    config_ok = False
                    break
        if env_set_required:
            pretty_print('@:: cockpit environment settings'.upper(), 'green', 'bright')
            while not config_ok:
                pretty_print("\nERROR: required parameters are not in configuration file!", 'red')
                if get_user_ok("Would you like cockpit to setup parameters automatically?"):
                    script = f"{os.environ['COCKPIT_HOME']}/scripts/get_params.py"
                    subprocess.call([script], shell=True)
                    
                    # reload cockpit configuration after changes
                    with open(config_yaml, 'r', encoding="utf-8") as yml:
                        data = yaml.safe_load(yml)
                else:
                    print(f"\nplease set required parameters in: '{config_yaml}'\n")
                    sys.exit(1)
                config_ok = True
                for env_name in data['params']:
                    if env_name != 'cockpit':
                        pivot = data['params'][env_name]['endpoints']['pivot']
                        if pivot == '' or pivot is None:
                            config_ok = False
                            break
        if db_set_required or env_set_required:
            pretty_print("\nCockpit setup and verification completed successfully!", 'green')
            press_any_key()
            print_header()

        spinner = Spinner
        object_loader = f"{os.environ['COCKPIT_HOME']}/assets/cockpit/load_objects.py"
        with spinner('Loading cockpit data... ', delay=0.1):            
            # add to cron if not exists
            if not crontab_ops('test', 'assets/cockpit/load_objects.py'):
                cron_comment = "# cockpit: update ods space objects every 60s"
                cron_line = f"{cron_comment}\n* * * * * /bin/bash -i -c '{object_loader}'\n"
                crontab_ops('add', cron_line)

            # run initial load
            return subprocess.run(
                [object_loader], 
                check=True,
                stdout=subprocess.PIPE
            ).stdout.decode()
    except (KeyboardInterrupt, SystemExit):
        os.system("stty sane ; stty erase ^H ; stty erase ^?")


def press_any_key():
    """
    pause until any key is pressed
    """

    _title = "Press any key to continue..."
    cmd = f"/bin/bash -c 'read -s -n 1 -p \"{_title}\"'"
    print('\n')
    os.system(cmd)
    print('\n')


def get_keypress():
    """ catch keypress"""

    old_settings = termios.tcgetattr(sys.stdin)
    tty.setcbreak(sys.stdin.fileno())
    key_mapping = {
        10: 'return',
        27: 'esc',
        127: 'backspace'
        }
    user_input = []
    while True:
        _b = os.read(sys.stdin.fileno(), 3).decode()
        if len(_b) == 3:
            k = ord(_b[2])
        else:
            k = ord(_b)
        this_key = key_mapping.get(k, chr(k))
        if this_key == 'return':
            break
        if this_key == 'esc':
            user_input.clear()
            user_input.append('esc')
            break
        if this_key == 'backspace':
            sys.stdout.write("\033[K")
            if len(user_input) > 0:
                user_input.pop()
        else:
            user_input.append(this_key)
        print(''.join(user_input), end='\r')
    termios.tcsetattr(sys.stdin, termios.TCSADRAIN, old_settings)
    return ''.join(user_input)


def validate_navigation_select(_items_dict, _the_selections):
    """
    ensure user choice is valid and update selections list
    :param the_dict: a dictionary of available choices
    :param _the_selections: list of current user selections
    :return:
    """

    # print menu
    is_main_menu = False
    max_item_len = 0
    # get max length of item string
    for key, val in _items_dict.items():
        if not str(key).isdigit(): continue
        if len(val['id']) > max_item_len: max_item_len =  len(val['id'])
    # print menu
    for key, val in _items_dict.items():
        if val == 'Main':
            is_main_menu = True
        if str(key).isdigit():
            index = f"[{key}]"
            item = f"{val['id']}"
            if val['description'] != '':
                description = val['description']
            else:
                description = ""
            print(f'{index:<4} - {item:<{max_item_len}} - {description}')
    print('\n' + '-' * 32)
    if is_main_menu:
        print(f'{"Esc":<4} - to Exit')
    else:
        print(f'{"Esc":<4} - to Go Back')
    print('\n')
    k = ''
    while True:
        k = get_keypress()
        if k == 'esc':
            if _items_dict['id'] == 'Main':
                sys.exit(0)
            else:
                update_selections(k, _the_selections)
                break
        if not k.isdigit() or int(k) not in _items_dict.keys():
            print(f'{Fore.RED}ERROR: Input must be a menu index!{Style.RESET_ALL}')
            continue
        update_selections(k, _the_selections)
        break


def validate_option_select(_items_dict, _title, _esc_to='Go Back'):
    """
    validate user choices from menu
    :param _items_dict: dictionary of menu items
    :param _title: the menu title printed at the start of menu
    :return: list of user choices
    """

    # check if choice in range
    def choice_ok(value, limit):
        if not value.isdigit() or int(value) < 1 or int(value) > limit:
            return False
        return True

    # build a reference dictionary for menu
    i, menu_indices = 1, {}
    for key in _items_dict.keys():
        menu_indices[i] = key
        i += 1

    # print submenu
    note = "(!) collections are supported (i.e: 1,3,2-5)"
    print(f"{_title}\n{note}")
    print('-' * len(_title))
    for key, val in menu_indices.items():
        index = f"[{key}]"
        print(f'{index:<4} - {_items_dict[val][0]:<24}')
    print('\n' + '-' * len(_title))
    print(f'{"Esc":<4} - to {_esc_to}')
    print('\n')

    # parse selections
    try:
        while True:
            valid_selections = []
            k = get_keypress()
            if k == 'esc':
                valid_selections.append(-1)
                return valid_selections
            selected_ok = False
            selected = k.split(',')
            for item in selected:
                if '-' in item: # if input is a range
                    range_select = item.split('-')
                    while '' in range_select:
                        range_select.remove('')
                    if len(range_select) != 2:
                        selected_ok = False
                        break
                    # verifying all elements of input are digits
                    range_select_check = [c for c in range_select if c.isdigit()]
                    if len(range_select) != len(range_select_check):
                        selected_ok = False
                        break

                    # populating valid selections list 
                    for i in range(int(range_select[0]), int(range_select[1])+1):
                        if choice_ok(str(i), len(_items_dict)):
                            selected_ok = True
                            valid_selections.append(menu_indices[i])
                        else:
                            selected_ok = False
                            break
                elif choice_ok(item, len(_items_dict)):
                    selected_ok = True
                    valid_selections.append(menu_indices[int(item)])
                else:
                    selected_ok = False
                    break
            if selected_ok:
                return list(set(valid_selections))
            print(f'{Fore.RED}ERROR: Input must be a menu index!{Style.RESET_ALL}')
    except (KeyboardInterrupt, SystemExit):
        os.system("stty sane ; stty erase ^H ; stty erase ^?")


def validate_type_select(_items_dict):
    """
    get object type selection from user
    :param _items_dict: menu dictionary object
    :return: int of user choice
    """

    # print submenu
    _title = "What type of task do you want to create?"
    print(_title + "\n" + '-' * len(_title))
    for key, val in _items_dict.items():
        index = f"[{key}]"
        print(f'{index:<4} - {val["name"]:<24} {val["description"]:<34}')
    print(f'\n{"Esc":<4} - to Go Back')
    print('-' * 32)
    try:
        while True:
            k = get_keypress()
            if k == 'esc':
                return -1
            if not k.isdigit() or int(k) not in _items_dict.keys():
                print(f'{Fore.RED}ERROR: Input must be a menu index!{Style.RESET_ALL}')
            else:
                return int(k)
    except (KeyboardInterrupt, SystemExit):
        os.system("stty sane ; stty erase ^H ; stty erase ^?")
        return None


def update_selections(_the_choice, _choices_list):
    """
    update user selections list
    :param _the_choice: the user choice
    :param _choices_list: the choices options
    :return:
    """
    if _the_choice == 'esc':
        _choices_list.pop()
    else:
        _choices_list.append(_the_choice)


# get user acceptance to run
def get_user_ok(_question):
    """
    ask for user permission to proceed
    :param _question: the question in subject
    :return: True / False
    """

    try:
        answer = input(f"{_question} [yes/no]: ").lower()
        while True:
            if answer == 'yes':
                return True
            if answer == 'no':
                return False
            answer = input("invlid input! type 'yes' or 'no': ")
    except (KeyboardInterrupt, SystemExit):
        os.system("stty sane ; stty erase ^H ; stty erase ^?")
        return False


### set debug flag ###
DEBUG = 0

def print_header():
    """ print menu header - figlet and VERSION """
    NAME = "Recovery Assistant"
    SPC = ''
    VERSION = "v1.0.0"
    VERLINE = f"{NAME} 2023, {VERSION} | Copyright Gigaspaces Ltd"
    if DEBUG != 1:
        os.system("clear")
    print(pyfiglet.figlet_format(f"{NAME}", font='slant', width=100))
    #print(pyfiglet.FigletFont.getFonts())
    print(f"{SPC}{VERLINE}\n\n")


def pretty_print(string, color, style=None):
    """
    pretty print
    :param string: the string to pretify
    :param color: the color to print
    :param style: the style to apply
    """

    color = eval('Fore.' + f'{color}'.upper())
    if style is None:
        print(f"{color}{string}{Style.RESET_ALL}")
    else:
        style = eval('Style.' + f'{style}'.upper())
        print(f"{color}{style}{string}{Style.RESET_ALL}")


def print_locations(selections, dictionary):
    """
    print locations line accordding to menu positions
    :param selections: the selections list
    :param dictionary: dictionary of menu items
    :return:
    """

    index = ""
    location = "@:: MAIN".upper()
    for item in selections:
        index += f"[{str(item)}]"
        location += " :: " + str(eval(f"dictionary{index}['id']")).upper()
    print_header()
    pretty_print(f'{location}\n', 'green', 'bright')


def print_table_headers(_headers):
    """
    print table headers
    :param _headers: dictionary of headers as '{col1_name: width, col2_name: width, ...}'
    :return:
    """
    
    v_sep = '- -'
    header_row = ""
    h_sep = ""
    last_col = list(_headers)[-1]
    for col, width in _headers.items():
        header_row += f'{col:<{width}} | '
        if col == last_col:
            h_sep += "-"*width    
        else:
            h_sep += "-"*width + v_sep
    print(header_row.rstrip('| '))
    print(h_sep)


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
    :param _server: the host
    :param _port:   the port
    :return: True / False
    """

    CONN_TIMEOUT = 1    # adjust value for connection test
    a_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    a_socket.settimeout(CONN_TIMEOUT)
    check_port = a_socket.connect_ex((_server, _port))
    a_socket.settimeout(None)
    return check_port == 0


def create_file(_data, _file, quite=None):
    """
    create a file for _data
    :param _data: the lines to inject into _file
    :param _file: the file to create
    :param quite: if not none supresses printing
    :return:
    """

    name = '.'.join(os.path.basename(_file).split('.')[:-1])
    extension = os.path.basename(_file).split('.')[-1:][0]
    if os.path.exists(_file):
        if quite is None:
            print(f"{name}.{extension} already exists. operation aborted")
    else:
        try:
            with open(_file, 'w', encoding="utf-8") as f_obj:
                f_obj.writelines('\n'.join(_data))
        except IOError as err:
            if quite is None:
                print(f"{name}.{extension} creation failed")
                print(f"{err}\n")
        else:
            if quite is None:
                print(f"{name}.{extension} created successfully\n")


def execute_command(_cmd, _title, indent=None):
    """
    execute subprocess command
    :param _cmd: command to execute of type list
    :param _title: print title for operation
    :param _indented: True/False for title indentation
    """
    if indent is None and _title != '':
        print(f"{_title} ...", end=' ')
    elif _title != '':
        indent_by = " " * indent
        print(f"{indent_by}{_title} ...", end=' ', flush=True)
    try:
        result = subprocess.run(
            _cmd,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
            check=True
            )
    except subprocess.SubprocessError as err:
        if _title != '':
            print("failed")
        print(f"{err}\n")
    else:
        if _title != '':
            if result.returncode == 0:
                print("successful\n")
            else:
                print("failed\n")


def generate_counter_job_file(_job_type, _env_name, _obj_type, _yaml_data):
    """
    create a counter job file
    :param _job_type: type of job
    :param _env_name: name of environment
    :param _obj_type: null or name of target object
    :param _yaml_data: data from config yaml
    :return:
    """

    env_name_low = _env_name.lower()
    pivot = f"PIVOT_{_env_name}"
    jobs_home = f"{os.environ['COCKPIT_HOME']}/jobs"
    if _obj_type != '':
        job_file_name = f"{_job_type}_{_env_name}_{_obj_type}.py".lower()
    else:
        job_file_name = f"{_job_type}_{_env_name}.py".lower()
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


def generate_feeder_job_file(_env_name, _feeder_type, _feeder_option):
    """
    create a feeder job file
    :param _env_name: name of environment
    :param _feeder_type: type of feeder
    :param _feeder_option: the option of feeder operation (start/stop/deploy)
    :return:
    """
    jobs_home = f"{os.environ['COCKPIT_HOME']}/jobs"
    cp_assets_dir = f"{os.environ['COCKPIT_HOME']}/assets/cockpit"
    cp_feeder_template = f"{cp_assets_dir}/feeder_template.py"
    feeder_job_file_name = f"feeder_{_env_name}_{_feeder_type}_{_feeder_option}.py".lower()
    feeder_job_file = f"{jobs_home}/{feeder_job_file_name}"
    # generate feeder job from template 
    cmd = f"cp {cp_feeder_template} {jobs_home}/{feeder_job_file_name}".split(' ')
    try:
        response = subprocess.run(cmd, check=True)
    except subprocess.SubprocessError as err:
        print(err)
    else:
        # setting execution bit for job file
        cmd = f"chmod +x {feeder_job_file}".split(' ')
        response = subprocess.run(cmd, check=True)

        # populating values according to feeder type and env
        if response.returncode == 0:
            with open(feeder_job_file, 'r', encoding="utf-8") as fjf:
                lines = fjf.readlines()
            with open(feeder_job_file, 'w', encoding="utf-8") as fjf:
                for line in lines:
                    if 'ENV_NAME =' in line:
                        fjf.write(f"ENV_NAME = '{_env_name}'\n")
                        continue
                    if 'FEEDER_TYPE = ' in line:
                        fjf.write(f"FEEDER_TYPE = '{_feeder_type}'\n")
                        continue
                    if 'FEEDER_OPTION = ' in line:
                        fjf.write(f"FEEDER_OPTION = '{_feeder_option}'\n")
                        continue
                    fjf.write(line)


def crontab_ops(_ops_type, _str):
    """
    add/remove/change crontab directives
    :param _ops_type: the operation type (add/delete/test)
    :param _str: the string to add/delete
    :return:
    """

    # output crontab
    cron_out = f"/tmp/cron_{randint(1000, 9999)}.out"
    cmd = f"crontab -l > {cron_out}"
    try:
        result = subprocess.run([cmd], shell=True, check=True)
    except subprocess.SubprocessError as err:
        print(f"ERROR: (crontab_ops) {err}")
        return False

    # test if exists in crontab
    if _ops_type == 'test':
        with open(cron_out, 'r', encoding='utf8') as cfile:
            lines = cfile.readlines()
            os.remove(cron_out)
            for line in lines:
                if _str in line:
                    return True    
            return False

    # add to crontab
    if _ops_type == 'add':
        with open(cron_out, 'a', encoding='utf8') as cfile:
            cfile.write(_str)

    # delete from crotab
    if _ops_type == 'delete':
        job_cmd = _str[5:]
        with open(cron_out, 'r', encoding='utf8') as cfile:
            lines = cfile.readlines()
        with open(cron_out, 'w', encoding='utf8') as cfile:
            for line in lines:
                if job_cmd in line:
                    continue
                cfile.write(line)

    # input crontab
    cmd = f"crontab {cron_out}".split(' ')
    try:
        result = subprocess.run(cmd, check=True)
    except subprocess.SubprocessError as err:
        print(f"ERROR: (crontab_ops) {err}")
        return False
    else:
        # delete temporary file
        os.remove(cron_out)
        if result.returncode != 0:
            print(f"ERROR: (crontab_ops) import completed with error code {result.returncode}")
            return False
        return True
