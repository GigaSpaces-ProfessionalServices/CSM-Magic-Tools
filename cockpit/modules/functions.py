#!/usr/bin/python3
# *-* coding: utf-8 *-*

###############################################################
##################          GENERAL          ##################
###############################################################

def handler(signal_recieved, frame):
    """
    catch CTRL+C keybaord press
    :param signal_recieved: caught by signal class
    :param frame:
    :return:
    """
    print('\n\nOperation aborted by user!')
    exit(0)


def sort_tuples_list(the_list):
    """
    sort a list of tuples by first key of tuple
    :param the_list: the list of tuples
    :return: the list of tuples
    """
    the_list.sort(key = lambda x: x[0])
    return the_list


def print_header():
    """
    print menu header
    """
    import pyfiglet
    import subprocess
    v_pref = ' ' * 2
    version = "ODS Cockpit 2022, v1.0 | Copyright Gigaspaces Ltd"
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
    try:
        with open(file, 'w') as f:
            f.writelines('\n'.join(data))
    except IOError as e:
        print(f"{name}.{extension} {Fore.RED}creation failed!{Style.RESET_ALL}")    
        print(e)
    else:
        print(f"{name}.{extension} {Fore.GREEN}created successfully!{Style.RESET_ALL}")    


###############################################################
##################    MENU AND VALIDATION    ##################
###############################################################

def print_menu(the_dict):
    """
    print the main menu
    :param dictionary: dictionary of menu items
    """
    for k in the_dict.keys():
        if str(k).isdigit():
            index = f"[{k}]"
            item = f"{the_dict[k]['id']}"
            if the_dict[k]['description'] != '':
                desc = f"- {the_dict[k]['description']}"
            else:
                desc = ""
            print(f'{index:<4} - {item:<24}{desc:<20}')
    print(f"{'[99]':<4} - {'ESC':<24}{'- Go Back / Exit ':<20}")


def update_selections(the_choice, choices_list):
    """
    update user selections list
    :param the_choice: the user choice
    :param choices_list: the choices options
    """
    if the_choice == '99':
        choices_list.pop()
    else:
        choices_list.append(the_choice)


def get_type_selection(the_dict):
    """
    get object type selection from user
    :param the_dict: menu dictionary object
    :return: int of user choice
    """
    q = f"What type of task do you want to create?"
    print(q + "\n" + '=' * len(q))
    for k, v in the_dict.items():
        index = f"[{k}]"
        print(f'{index:<4} - {v["name"]:<24} {v["description"]:<34}')
    print(f'{"[99]":<4} - {"ESC":<24}')
    return int(validate_input(the_dict))


def validate_main_menu_input(the_dict, the_selections):
    """
    ensure user choice is valid
    :param the_dict: a dictionary of choices
    :param the_selections: the list of choices
    """
    the_choice = input("\nEnter your choice: ")
    while True:
        if the_choice == '99':
            if the_dict['id'] == 'Main':
                print("Quitting...")
                exit(0)
            else:
                update_selections(the_choice, the_selections)
                break
        if not the_choice.isdigit() or int(the_choice) not in the_dict.keys():
            pretty_print('ERROR: Input must be a menu index!', 'red')
            the_choice = input("Enter you choice: ")
        else:
            update_selections(the_choice, the_selections)
            break


def validate_input(items_dict):
    from colorama import Fore
    choice = input("\nEnter your choice: ")
    while True:
        if choice == '99':
            return -1
        if len(items_dict) > 1:
            if choice == str(len(items_dict) + 1): # if 'ALL' is selected
                return "ALL"
        if not choice.isdigit() or int(choice) not in items_dict.keys():
            choice = input(f"{Fore.RED}ERROR: Input must be a menu index!{Fore.RESET}\nEnter you choice: ")
        else:
            return int(choice)


def parse_multi_select(choice_list):
    """
    parse user selected choices
    :param choice_list: list of choices
    :return: list of selections
    """
    from colorama import Fore
    choice = input("\nEnter your choice: ")
    
    # check if choice in range
    def choice_ok(value, limit):
        if not value.isdigit() or int(value) < 1 or int(value) > limit:
            return False
        return True
        
    while True:
        valid_selections = []
        if choice == '99':
            valid_selections.append(-1)
            return valid_selections
        else:
            selected_ok = False
            selected = choice.split(',')
            for c in selected:
                if '-' in c:
                    range_select = c.split('-')
                    if len(range_select) != 2:
                        selected_ok = False
                        break
                    for i in range(int(range_select[0]), int(range_select[1])+1):
                        if choice_ok(str(i), len(choice_list)):
                            selected_ok = True
                            valid_selections.append(i)
                        else:
                            selected_ok = False
                            break
                elif choice_ok(c, len(choice_list)):
                    selected_ok = True
                    valid_selections.append(int(c))
                else:
                    selected_ok = False
                    break
            if selected_ok:
                return list(set(valid_selections))
            else:
                choice = input(f"{Fore.RED}ERROR: Invalid input!{Fore.RESET}\nEnter you choice: ")

# get user acceptance to run
def get_user_permission(question):
    q = f"{question} [yes/no]: "
    answer = input(q).lower()
    while True:
        if answer == 'yes': return True
        elif answer == 'no': return False
        else: answer = input("invlid input! type 'yes' or 'no': ")


def check_settings(config):
    """
    check required settings of db and network in yaml file
    :param config: the yaml file
    :return:
    """
    import os
    import yaml
    import subprocess
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
        pretty_print('ERROR: cockpit.db is not set in configuration file. Aborting!', 'red')
        exit(1)
    elif not os.path.exists(cockpit_db):
        db_set_required = True
        pretty_print("@:: cockpit db settings".upper(), 'green', 'bright')
        print("cockpit.db configuration exists but database has not been created.")
        if get_user_permission("would you like to create the cockpit database now?"):
            subprocess.call([f"{os.path.dirname(os.path.realpath(__file__))}/create_db.py"], shell=True)
            if not os.path.exists(cockpit_db): exit(1)
        else:
            pretty_print('ERROR: a cockpit database is required in order to run. Aborting!', 'red')
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
                        errstr = "ERROR: environment variable for " + f"{env_name}".upper() + " pivot is not set. aborting!"
                        pretty_print(errstr, 'red')
                        exit(1)
            pretty_print("ERROR: required parameters are not in configuration file!", 'red')
            if get_user_permission("would you like cockpit to setup parameters automatically?"):
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
        pretty_print("\nCockpit setup and verification completed successfully.", 'green')
        input("Press ENTER to continue to the main menu.")
        print_header()
    from . import spinner
    spinner = spinner.Spinner
    with spinner('Loading cockpit data... ', delay=0.1):
        conn = create_connection(cockpit_db)
        register_types(conn, get_object_types_from_space(data))


def get_object_types_from_space(yaml_data):
    """
    get object types from space
    :param yaml_data: the data from yaml file 
    :return: formatted dictionary as {key : [object_type, num_entries]}
    """
    import os
    import subprocess
    import json
    types = []
    connections_ok = []
    for env_name in yaml_data['params']:
        if env_name != 'cockpit':
            pivot = yaml_data['params'][env_name]['endpoints']['pivot']
            exec_script = f"{os.path.dirname(os.path.realpath(__file__))}/get_space_objects.py"
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


def get_object_types_from_db(conn):
    """
    get registered types from database
    :param conn: connection object
    :return: list of rows
    """
    c = conn.cursor()
    c.execute("SELECT name FROM types;")
    rows = c.fetchall()
    object_types = {}
    if len(rows) > 0:
        index = 1
        for t in rows:
            object_types[index] = [t[0]]
            index += 1
    return object_types


def register_types(conn, types):
    """
    register types
    :param conn: database connection object
    :param types: types data
    :return:
    """
    cur = conn.cursor()
    for type in types.values():
        the_type = type[0]
        if not type_exists(conn, the_type):
            sql =f"INSERT INTO types(name) VALUES(?);"
            cur.execute(sql, (the_type,))
    conn.commit()


def list_types(conn):
    """
    list registered types in database
    :param conn: connection object
    :return: list of rows
    """
    c = conn.cursor()
    c.execute("SELECT * FROM types;")
    rows = c.fetchall()
    if len(rows) > 0:
        for t in rows:
            print(f"   {t[0]:<10}")


def type_exists(conn, the_type):
    """
    check if type exists in database
    :param conn: connection object
    :return Boolean: True / Flase
    """
    c = conn.cursor()
    c.execute("SELECT name FROM types WHERE name = ?;", (the_type,))
    rows = c.fetchall()
    if len(rows) > 0:
        return True
    else:
        return False


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
    establish a database connection (or create a new db file)
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


def list_tables(conn):
    """
    list tables in database
    :param conn: database connection object
    :return:
    """    
    c = conn.cursor()
    c.execute("SELECT name FROM sqlite_master WHERE type='table';")
    tables = c.fetchall()
    if len(tables) > 0:
        for table_name in tables:
            c.execute(f"SELECT count(*) FROM {table_name[0]};")
            num = c.fetchall()[0][0]
            num_records = f"{num} record(s)"
            print(f"   {table_name[0]:<10} : {num_records:<10}")


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
                "obj_type": data['type']
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

def list_jobs(conn, *columns):
    """
    list registered jobs in database
    :param conn: database connection object
    :param columns: collection of table columns
    :return: list of rows
    """
    cur = conn.cursor()
    args = ','.join(columns)
    if len(columns) == 0:
        args = '*'
    sql = f"SELECT {args} FROM jobs"
    cur.execute(sql)
    rows = cur.fetchall()
    return rows


def list_jobs_by_task_uid(conn, task_uid):
    """
    list jobs associated with a task uid
    :param conn: connection object
    :param task_uid: the task uid
    :return: list of rows
    """
    cur = conn.cursor()
    sql = """ SELECT j.name, j.id, j.destination, j.metadata, j.content 
              FROM tasks t INNER JOIN jobs j 
              ON j.id = t.job_id 
              WHERE t.uid = ?; """
    cur.execute(sql, task_uid)
    rows = cur.fetchall()
    return rows


def parse_jobs_selections(jobs):
    """
    parse user selected jobs
    :param jobs: list of jobs
    :return: list of selections
    """
    from colorama import Fore
    choice = input("\nEnter your choice: ")
    
    # check if choice in range
    def choice_ok(value, limit):
        if not value.isdigit() or int(value) < 1 or int(value) > limit:
            return False
        return True
        
    while True:
        valid_selections = []
        if choice == '99':
            valid_selections.append(-1)
            return valid_selections
        else:
            selected_ok = False
            selected = choice.split(',')
            for c in selected:
                if '-' in c:
                    range_select = c.split('-')
                    if len(range_select) != 2:
                        selected_ok = False
                        break
                    for i in range(int(range_select[0]), int(range_select[1])+1):
                        if choice_ok(str(i), len(jobs)):
                            selected_ok = True
                            valid_selections.append(i)
                        else:
                            selected_ok = False
                            break
                elif choice_ok(c, len(jobs)):
                    selected_ok = True
                    valid_selections.append(int(c))
                else:
                    selected_ok = False
                    break
            if selected_ok:
                return list(set(valid_selections))
            else:
                choice = input(f"{Fore.RED}ERROR: Invalid input!{Fore.RESET}\nEnter you choice: ")


def jobs_exist(conn, new_job_name):
    """
    check if job exists
    :param conn: database connection object
    :param new_job_name: the name of job to look for 
    :return Boolean: True/False
    """
    job_exists = False
    c = conn.cursor()
    c.execute("SELECT name FROM jobs;")
    registered_job_names = c.fetchall()
    for name in registered_job_names:
        if new_job_name in name:
            return True
    return False


def register_job(conn, job):
    """
    register a new job
    :param conn: database connection object
    :param job: job data
    :return: job id
    """
    sql = """ INSERT INTO jobs(name,metadata,content,command,destination,created)
              VALUES(?,?,?,?,?,?) """
    cur = conn.cursor()
    cur.execute(sql, job)
    conn.commit()
    return cur.lastrowid


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
    jobs_home = f"{os.path.dirname(os.path.realpath(__file__))}/../jobs"
    job_file_name = f"{job_type}_{env_name}_{obj_type}.py".lower()
    job_file = f"{jobs_home}/{job_file_name}"
    pivot = yaml_data['params'][env_name_low]['endpoints']['pivot']
    cmd = "cat {exec_script} | ssh " + pivot + " python3 -"
    sp_exec = 'subprocess.run([cmd], shell=True, stdout=subprocess.PIPE).stdout.decode()'
    lines = [
        '#!/usr/bin/python3\n\n',
        'import subprocess\n',
        f'exec_script = "{os.path.dirname(os.path.realpath(__file__))}/get_space_objects.py"',
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


###############################################################
##################           TASKS           ##################
###############################################################

def list_tasks(conn, *columns):
    """
    list registered tasks in database
    :param conn: connection object
    :param columns: collection of table columns
    :return: list of rows
    """
    cur = conn.cursor()
    args = ','.join(columns)
    if len(columns) == 0:
        args = '*'
    sql = f"SELECT {args} FROM tasks"
    cur.execute(sql)
    rows = cur.fetchall()
    return rows


def list_tasks_grouped(conn, *columns):
    """
    list registered tasks in database
    :param conn: connection object
    :param columns: collection of table columns
    :return: list of rows
    """
    cur = conn.cursor()
    args = ','.join(columns)
    if len(columns) == 0:
        args = '*'
    sql = f"SELECT {args} FROM tasks GROUP BY uid"
    cur.execute(sql)
    rows = cur.fetchall()
    return rows


def register_task(conn, task):
    """
    register a new task
    :param conn: database connection object
    :param task: task data
    :return: task id
    """
    sql = """ INSERT INTO tasks(uid,type,sn_type,job_id,metadata,content,state,created)
              VALUES(?,?,?,?,?,?,?,?) """
    cur = conn.cursor()
    cur.execute(sql, task)
    conn.commit()
    return cur.lastrowid


def list_tasks_by_policy_schedule(conn, policy_schedule):
    """
    list tasks associated with a policy id
    :param conn: connection object
    :param policy_id: the policy id
    :return: list of rows
    """
    cur = conn.cursor()
    sql = "SELECT task_uid FROM policies WHERE policies.schedule_sec = ?;"
    cur.execute(sql, (policy_schedule,))
    rows = cur.fetchall()
    return rows


###############################################################
##################         POLICIES          ##################
###############################################################

def list_policies(conn, *columns):
    """
    list registered policies in database
    :param conn: connection object
    :param columns: collection of table columns
    :return: list of rows
    """
    cur = conn.cursor()
    args = ','.join(columns)
    if len(columns) == 0:
        args = '*'
    sql = f"SELECT {args} FROM policies"
    cur.execute(sql)
    rows = cur.fetchall()
    return rows


def register_policy(conn, policy):
    """
    register a new policy
    :param conn: database connection object
    :param policy: policy data
    :return: policy id
    """
    sql = """ INSERT INTO policies(name,schedule_sec,task_uid,metadata,content,active_state,created)
              VALUES(?,?,?,?,?,?,?) """
    cur = conn.cursor()
    cur.execute(sql, policy)
    conn.commit()
    return cur.lastrowid


def policy_schedule_exists(conn, policy_schedule):
    """
    check if policy schedule exists
    :param conn: database connection object
    :param policy_schedule: the policy schedule to check
    :return Boolean: True/False
    """
    c = conn.cursor()
    c.execute("SELECT schedule_sec FROM policies GROUP BY schedule_sec;")
    schedules = c.fetchall()
    for s in schedules:
        if s[0] == int(policy_schedule):
            return True
    return False
