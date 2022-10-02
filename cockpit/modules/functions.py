#!/usr/bin/python3
# *-* coding: utf-8 *-*

###############################################################
##################          GENERAL          ##################
###############################################################

def handler(signal_recieved, frame):
    ### catch CTRL+C keybaord press ###
    print('\n')
    exit(0)


def print_header():
    """
    print menu header - figlet and version
    """
    import pyfiglet
    import subprocess
    v_pref = ' ' * 2
    version = "ODS Cockpit 2022, v1.1 | Copyright Gigaspaces Ltd"
    subprocess.run("clear")
    print(pyfiglet.figlet_format("ODS Cockpit", font='slant'))
    print(f"{v_pref}{version}\n\n")


def pretty_print(string, color, style=None):
    """
    pretty print
    :param string: the string to pretify
    :param color: the color to print 
    :param style: the style to apply
    """
    from colorama import Fore, Style
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
    location = f"@:: MAIN".upper()
    for i in selections:
        index += f"[{str(i)}]"
        location += " :: " + str(eval(f"dictionary{index}['id']")).upper()
    print_header()
    pretty_print(f'{location}\n', 'green', 'bright')


def check_connection(server, port):
    """
    check connection to server on given port
    :param selections: the selections list
    :param dictionary: dictionary of menu items
    :return:
    """
    import socket
    conn_timeout = 1    # adjust value for connection test
    a_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    a_socket.settimeout(conn_timeout)
    check_port = a_socket.connect_ex((server, port))
    a_socket.settimeout(None)
    if check_port == 0:
        return True
    else:
        return False


def create_file(data, file):
    """
    create a file for data
    :param data: the lines to inject into file
    :param file: the file to create
    :return:
    """
    from colorama import Fore, Style
    import os
    name = '.'.join(os.path.basename(file).split('.')[:-1])
    extension = os.path.basename(file).split('.')[-1:][0]
    if os.path.exists(file):
        print(f"{name}.{extension} already exists. {Fore.RED}creation aborted!{Style.RESET_ALL}")
    else:
        try:
            with open(file, 'w') as f:
                f.writelines('\n'.join(data))
        except IOError as e:
            print(f"{name}.{extension} {Fore.RED}creation failed!{Style.RESET_ALL}")
            print(e)
        else:
            print(f"{name}.{extension} {Fore.GREEN}created successfully!{Style.RESET_ALL}")
        

def get_keypress():
    import sys,tty,os,termios
    old_settings = termios.tcgetattr(sys.stdin)
    tty.setcbreak(sys.stdin.fileno())
    key_mapping = {
        10: 'return', 
        27: 'esc', 
        127: 'backspace'
        }
    user_input = []
    while True:
        b = os.read(sys.stdin.fileno(), 3).decode()
        if len(b) == 3:
            k = ord(b[2])
        else:
            k = ord(b)
        this_key = key_mapping.get(k, chr(k))
        if this_key == 'return':
            break
        elif this_key == 'esc':
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


def sort_tuples_list(the_list):
    """
    sort a list of tuples by first key of tuple
    :param the_list: the list of tuples
    :return: the list of tuples
    """
    the_list.sort(key = lambda x: x[0])
    return the_list


def press_any_key():
    import os
    title = "Press any key to continue..."
    cmd = f"/bin/bash -c 'read -s -n 1 -p \"{title}\"'"
    print('\n')
    os.system(cmd)
    print('\n')


###############################################################
##################    MENU AND VALIDATION    ##################
###############################################################

def validate_navigation_select(items_dict, the_selections):
    """
    ensure user choice is valid and update selections list
    :param the_dict: a dictionary of available choices
    :param the_selections: list of current user selections
    :return:
    """
    import os
    # print menu
    is_main_menu = False
    for k, v in items_dict.items():
        if v == 'Main':
            is_main_menu = True
        if str(k).isdigit():
            index = f"[{k}]"
            item = f"{v['id']}"
            if v['description'] != '':
                description = f"- {v['description']}"
            else:
                desc = ""
            print(f'{index:<4} - {item:<24}{description:<20}')
    print('-' * 32)
    if is_main_menu:
        print(f'{"Esc":<4} - to Exit')
    else:
        print(f'{"Esc":<4} - to Go Back')
    print('\n')
    k = ''
    while True:
        k = get_keypress()
        if k == 'esc':
            if items_dict['id'] == 'Main':
                quit()
            else:
                update_selections(k, the_selections)
                break
        if not k.isdigit() or int(k) not in items_dict.keys():
            pretty_print('ERROR: Input must be a menu index!', 'red')
            continue
        else:
            update_selections(k, the_selections)
            break


def validate_option_select(items_dict, title, esc_to='Go Back'):
    """
    validate user choices from menu
    :param items_dict: dictionary of menu items
    :param title: the menu title printed at the start of menu
    :return: list of user choices
    """
    import os
    
    # check if choice in range
    def choice_ok(value, limit):
        if not value.isdigit() or int(value) < 1 or int(value) > limit:
            return False
        return True
    
    # build a reference dictionary for menu
    i, menu_indices = 1, {}
    for k in items_dict.keys():
        menu_indices[i] = k
        i += 1

    # print submenu
    note = "(!) collections are supported (i.e: 1,3,2-5)"
    print(f"{title}\n{note}")
    print('-' * len(note))
    for k, v in menu_indices.items():
        index = f"[{k}]"
        print(f'{index:<4} - {items_dict[v][0]:<24}')
    print('-' * 32)
    print(f'{"Esc":<4} - to {esc_to}')
    print('\n')

    # parse selections
    try:
        while True:
            valid_selections = []
            k = get_keypress()
            if k == 'esc':
                valid_selections.append(-1)
                return valid_selections
            else:
                selected_ok = False
                selected = k.split(',')
                for c in selected:
                    if '-' in c:
                        range_select = c.split('-')
                        while('' in range_select): range_select.remove('')
                        if len(range_select) != 2:
                            selected_ok = False
                            break
                        for i in range(int(range_select[0]), int(range_select[1])+1):
                            if choice_ok(str(i), len(items_dict)):
                                selected_ok = True
                                valid_selections.append(menu_indices[i])
                            else:
                                selected_ok = False
                                break
                    elif choice_ok(c, len(items_dict)):
                        selected_ok = True
                        valid_selections.append(menu_indices[int(c)])
                    else:
                        selected_ok = False
                        break
                if selected_ok:
                    return list(set(valid_selections))
                else:
                    pretty_print('ERROR: Input must be a menu index!', 'red')
    except (KeyboardInterrupt, SystemExit):
        os.system('stty sane')


def validate_type_select(items_dict):
    """
    get object type selection from user
    :param the_dict: menu dictionary object
    :return: int of user choice
    """
    import os
    # print submenu
    q = f"What type of task do you want to create?"
    print(q + "\n" + '-' * len(q))
    for k, v in items_dict.items():
        index = f"[{k}]"
        print(f'{index:<4} - {v["name"]:<24} {v["description"]:<34}')
    print(f'\n{"Esc":<4} - to Go Back')
    print('-' * 32)
    try:
        while True:
            k = get_keypress()
            if k == 'esc':
                return -1
            if not k.isdigit() or int(k) not in items_dict.keys():
                pretty_print('ERROR: Input must be a menu index!', 'red')
            else:
                return int(k)
    except (KeyboardInterrupt, SystemExit):
        os.system('stty sane')
            

def update_selections(the_choice, choices_list):
    """
    update user selections list
    :param the_choice: the user choice
    :param choices_list: the choices options
    :return:
    """
    if the_choice == 'esc':
        choices_list.pop()
    else:
        choices_list.append(the_choice)


# get user acceptance to run
def get_user_ok(question):
    """
    ask for user permission to proceed
    :param question: the question in subject
    :return: True / False
    """
    import os
    try:
        q = f"{question} [yes/no]: "
        answer = input(q).lower()
        while True:
            if answer == 'yes': return True
            elif answer == 'no': return False
            else: answer = input("invlid input! type 'yes' or 'no': ")
    except (KeyboardInterrupt, SystemExit):
        os.system('stty sane')


def check_settings(config):
    """
    check required settings of db and network in yaml file
    :param config: the yaml file
    :return:
    """
    import os
    import yaml
    import subprocess
    from .spinner import Spinner
    db_set_required = False
    env_set_required = False
    
    # load cockpit configuration
    with open(config, 'r') as yf:
        data = yaml.safe_load(yf)

    # cockpit database settings
    cockpit_db_home = data['params']['cockpit']['db_home']
    cockpit_db_name = data['params']['cockpit']['db_name']
    cockpit_db = f"{cockpit_db_home}/{cockpit_db_name}"
    if cockpit_db_home == '' or cockpit_db_home is None or cockpit_db_name == '' or cockpit_db_name is None:
        pretty_print("@:: cockpit db settings".upper(), 'green', 'bright')
        pretty_print('\nERROR: cockpit.db is not set in configuration file. Aborting!', 'red')
        exit(1)
    elif not os.path.exists(cockpit_db):
        db_set_required = True
        pretty_print("@:: cockpit db settings".upper(), 'green', 'bright')
        pretty_print('\nCockpit database was not found!', 'red')
        if get_user_ok("Would you like to create the cockpit database?"):
            subprocess.call([f"{os.environ['COCKPIT_HOME']}/modules/create_db.py"], shell=True)
            if not os.path.exists(cockpit_db): exit(1)
        else:
            pretty_print('\nERROR: a cockpit database is required in order to run. Aborting!', 'red')
            exit(1)
    # cockpit enviroment settings
    for env_name in data['params']:
        if env_name != 'cockpit':
            pivot = data['params'][env_name]['endpoints']['pivot']
            if pivot == '' or pivot is None:
                env_set_required = True
                config_ok = False
                break
    if env_set_required:
        pretty_print(f'@:: cockpit environment settings'.upper(), 'green', 'bright')
        while not config_ok:
            for env_name in data['params']:
                if env_name != 'cockpit':
                    if os.environ.get(data['params'][env_name]['variables']['pivot']) == None:
                        errstr = "\nERROR: environment variable for " + f"{env_name}".upper() + " pivot is not set. aborting!"
                        pretty_print(errstr, 'red')
                        exit(1)
            pretty_print("ERROR: required parameters are not in configuration file!", 'red')
            if get_user_ok("\nWould you like cockpit to setup parameters automatically?"):
                for env_name in data['params']:
                    if env_name != 'cockpit':
                        script = f"./modules/get_{env_name}_params.py"
                        subprocess.call([script], shell=True)
                # reload cockpit configuration after changes
                with open(config, 'r') as yf:
                    data = yaml.safe_load(yf)
            else:
                print(f"\nplease set required parameters in: '{config}'\n")
                exit()
            config_ok = True
            for env_name in data['params']:
                if env_name != 'cockpit':
                    pivot = data['params'][env_name]['endpoints']['pivot']
                    if pivot == '' or pivot is None:
                        config_ok = False
                        break
    if db_set_required or env_set_required:
        pretty_print("Cockpit setup and verification completed successfully.", 'green')
        press_any_key()
        print_header()
    spinner = Spinner
    with spinner('Loading cockpit data... ', delay=0.1):
        conn = create_connection(cockpit_db)
        types = get_object_types_from_space(data)
        for type in types.values():
            the_type = type[0]
            sql = f"SELECT name FROM types WHERE name = '{the_type}';"
            if len(db_select(conn, sql)) == 0:
                sql =f"INSERT INTO types(name) VALUES(?);"
                db_insert(conn, sql, (the_type,))


def get_object_types_from_space(yaml_data):
    """
    get object types from ods space
    :param yaml_data: the data from yaml file 
    :return: formatted dictionary as {key : [object_type, num_entries],}
    """
    import os
    import subprocess
    import json
    types = []
    connections_ok = []
    for env_name in yaml_data['params']:
        if env_name != 'cockpit':
            pivot = yaml_data['params'][env_name]['endpoints']['pivot']
            exec_script = f"{os.environ['COCKPIT_HOME']}/modules/get_space_objects.py"
            if check_connection(pivot, 22):
                connections_ok.append(True)
                cmd = f"cat {exec_script} | ssh {pivot} python3 -"
                response = subprocess.run([cmd], shell=True, stdout=subprocess.PIPE).stdout.decode()
                response = json.loads(response.replace("\'", "\""))
                for k in response.keys():
                    if k != 'java.lang.Object':
                        types.append(k)
    if True in connections_ok:
        k = 1
        object_types = {}
        for the_type in set(types):
            v = [ the_type, response[the_type]['entries']]
            object_types[k] = v
            k += 1
    else:
        ### IF NO PIVOT ARE ACCESSIBLE WE GENERATE THIS OBJECT FOR DEBUGGING ###
        object_types = {
            1: ['com.j_spaces.examples.benchmark.messages.MessagePOJO', 100000],
            2: ['com.j_spaces.examples.benchmark.messages.MessagePOJO1', 110000],
            3: ['com.j_spaces.examples.benchmark.messages.MessagePOJO2', 120000],
            4: ['com.j_spaces.examples.benchmark.messages.MessagePOJO3', 130000],
            5: ['com.j_spaces.examples.benchmark.messages.MessagePOJO4', 140000]
            }
    return object_types

###############################################################
#################         DATABASES          ##################
###############################################################

def create_database_home(db_folder):
    """
    create sqlite3 home directory if doesn't exists
    :param db_folder: sqlite3 home path
    :return:
    """
    import os
    if not os.path.exists(db_folder):
        try:
            os.makedirs(db_folder)
        except OSError as e:
            if 'Errno 13' in str(e):
                print(f"\n{e}\n *try changing the path in config.yaml")
            else:
                print(e)
            exit(1)


def create_connection(db_file):
    """
    establish a database connection (or create a new db if not exist)
    :param db_file: path to db file
    :return: connection object
    """
    import sqlite3
    from sqlite3 import Error
    conn = None
    try:
        conn = sqlite3.connect(db_file)
    except Error as e:
        print(e)
    return conn


def create_table(conn, create_table_sql):
    """
    create a table from sql create_table_sql
    :param conn: connection object
    :param create_table_sql: sqlite create table statement
    :return:
    """
    from sqlite3 import Error
    try:
        c = conn.cursor()
        c.execute(create_table_sql)
    except Error as e:
        print(e)


def db_select(conn, sql):
    """
    execute a select query on the database
    :param conn: database connection object
    :param sql: the query to execute
    :return:
    """
    from sqlite3 import Error
    try:
        c = conn.cursor()
        c.execute(sql)
    except Error as e:
        print(e)
    else:
        result = c.fetchall()
        return result



def db_insert(conn, sql, data):
    """
    insert into database
    :param conn: database connection object
    :param sql: the query to execute
    :param data: the data to insert
    :return: row id
    """
    from sqlite3 import Error
    try:
        c = conn.cursor()
        c.execute(sql, data)
    except Error as e:
        print(e)
    else:
        conn.commit()
        return c.lastrowid


def db_delete(conn, sql):
    """
    delete from database
    :param conn: database connection object
    :param sql: the query to execute
    :return:
    """
    from sqlite3 import Error
    try:
        c = conn.cursor()
        c.execute(sql)
    except Error as e:
        print(e)
    else:
        conn.commit()
        return c.lastrowid


def write_to_influx(dbname, data):
    """
    write data to influx database
    :param dbname: the name of the target database
    :param data: dictionary of the data payload
    e.g: data = {env: 'prod/dr', type: 'the_object', count: num_of_entries}
    :return:
    """
    import datetime
    from influxdb import InfluxDBClient

    client = InfluxDBClient(host='localhost', port=8086)
    if dbname not in str(client.get_list_database()):
        client.create_database(dbname)
    client.switch_database(dbname)
    timestamp = (datetime.datetime.now()).strftime('%Y-%m-%dT%H:%M:%SZ')
    json_body = [
        {
            "measurement": "validation",
            "tags": {
                "env": data['env'],
                "obj_type": data['obj_type']
            },
            "time": timestamp,
            "fields": {
                "count": data['count']
            }
        }
    ]
    client.write_points(json_body)


###############################################################
##################           JOBS            ##################
###############################################################

def generate_job_file(job_type, env_name, obj_type, yaml_data):
    """
    create a file for a job
    :param env_name: name of environment
    :param obj_type: name of target object
    :param yaml_data: data from config yaml
    :return:
    """
    import os
    import subprocess
    env_name_low = env_name.lower()
    pivot = f"PIVOT_{env_name}"
    jobs_home = f"{os.environ['COCKPIT_HOME']}/jobs"
    job_file_name = f"{job_type}_{env_name}_{obj_type}.py".lower()
    job_file = f"{jobs_home}/{job_file_name}"
    pivot = yaml_data['params'][env_name_low]['endpoints']['pivot']
    cmd = "cat {exec_script} | ssh " + pivot + " python3 -"
    sp_exec = 'subprocess.run([cmd], shell=True, stdout=subprocess.PIPE).stdout.decode()'
    lines = [
        '#!/usr/bin/python3\n\n',
        'import subprocess\n',
        f'exec_script = "{os.environ["COCKPIT_HOME"]}/modules/get_space_objects.py"',
        f'cmd = f"{cmd}"',
        f'response = {sp_exec}',
        f'print(response)\n\n'
    ]
    # create jobs home folder if not exists
    if not os.path.exists(jobs_home):
        try:
            os.makedirs(jobs_home)
        except OSError as e:
            print(e)
    with open(job_file, 'w') as j:
        j.writelines('\n'.join(lines))
    # set execution bit for job file
    subprocess.run([f"chmod +x {job_file}"], shell=True)
