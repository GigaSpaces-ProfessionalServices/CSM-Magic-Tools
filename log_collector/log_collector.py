#!/usr/bin/python3

import sys
import os
import time
import subprocess
import pyfiglet
from signal import signal, SIGINT
from datetime import datetime, date, time


class Bcolors:
    HEADER = '\033[95m'
    OKBLUE = '\033[94m'
    OKGREEN = '\033[92m'
    WARNING = '\033[93m'
    FAIL = '\033[91m'
    ENDC = '\033[0m'

    def disable(self):
        self.HEADER = ''
        self.OKBLUE = ''
        self.OKGREEN = ''
        self.WARNING = ''
        self.FAIL = ''
        self.ENDC = ''


def handler(signal_received, frame):
    # Handle any cleanup here
    print('\n\nOperation aborted by user!\n')
    exit(0)


def get_selection(the_items):
    """
    get and validate user selections
    :param the_items: a dictionary of the menu
    :return: integer of user choice
    """
    while True:
        print('\n')
        option = input('Your choice: ')
        if not option.isdigit():
            print("ERROR : The value you entered is not a digit")
            continue
        if int(option) not in the_items.keys():
            print("ERROR : The value you entered is out of bounds")
            continue
        return int(option)


def verify_date_time(opt):
    """
    check a string of date and time
    :param opt: string indicating 'start' / 'end'
    :return: value of status as set in {dt_errors}
    """
    global date_time
    global epoch_date_time
    # opt = start / end
    opt_printable = opt.capitalize()
    # get the input
    date_time = input('{} date and time: '.format(opt_printable)).strip()
    # split input to date & time
    date_time_parts = date_time.split(' ')
    if len(date_time_parts) == 2:
        the_date, the_time = date_time_parts[0], date_time_parts[1]
    else:
        return 1
    # split date into integer components
    date_elements = the_date.split('-')
    if len(date_elements) == 3:
        _yy, _mm, _dd = date_elements[0], date_elements[1], date_elements[2]
    else:
        return 2
    # split time into integer components
    time_elements = the_time.split(':')
    if len(time_elements) == 3:
        _hr, _min, _sec = time_elements[0], time_elements[1], time_elements[2]
    else:
        return 3
    today = date.today()
    if len(_yy) != 4 or int(_yy) > today.year or \
            len(_mm) != 2 or int(_mm) > 12 or \
            len(_dd) != 2 or int(_dd) > 31:
        return 2
    if len(_hr) != 2 or int(_hr) > 23 or \
            len(_min) != 2 or int(_min) > 59 or \
            len(_sec) != 2 or int(_sec) > 59:
        return 3
    input_epoch = datetime(int(_yy), int(_mm), int(_dd), int(_hr), int(_min), int(_sec)).timestamp()
    if int(input_epoch) > int(datetime.now().timestamp()):
        return 4
    else:
        epoch_date_time = input_epoch
    return 0


def get_date_time(phase):
    """
    call for user input, verify input and calculate against epoch time
    :param phase: string indicating 'start' / 'end'
    :return: print error message
    """
    while True:
        ret = verify_date_time(phase)
        if ret == 0:
            if phase == 'start':
                global epoch_start_date_time
                global start_date_time
                start_date_time = date_time
                epoch_start_date_time = epoch_date_time
            if phase == 'end':
                global epoch_end_date_time
                global end_date_time
                end_date_time = date_time
                epoch_end_date_time = epoch_date_time
            break
        else:
            print("Enter Proper date = " + str(ret))


def path_not_exists(the_path):
    """
    Check if a given pathg exists
    :param the_path:
    :return: True / False
    """
    if os.path.isfile(the_path):
        return False
    if os.path.isdir(the_path):
        return False
    return True

def listAllScripts(list_files_path):
    """
    Check if a folder and return list of file
    :param the_path:
    :return: List of files
    """
    if os.path.exists(list_files_path):
        files = [file for file in os.listdir(list_files_path) if
            os.path.isfile(os.path.join(list_files_path, file))]
    else:
        files = []
    return files

if __name__ == '__main__':
    signal(SIGINT, handler)

    # GLOBALS
    LOCAL_LOGS_DIR = "/dbagigalogs/log_collections"
    REMOTE_LOGS_DIR = "/dbagigalogs"
    date_time = ""
    epoch_date_time = ""
    epoch_start_date_time = ""
    epoch_end_date_time = ""
    start_date_time = ""
    end_date_time = ""

    servers = {
        1: {"type": "Spaces", "runall": "-s"},
        2: {"type": "Management", "runall": "-m"},
        3: {"type": "Spaces and Management", "runall": "-a"},
        4: {"type": "Northbound Application", "runall": "-na"},
        5: {"type": "Northbound Management", "runall": "-nm"},
        6: {"type": "Northbound Application and Management", "runall": "-n"},
        7: {"type": "CDC", "runall": "-c"},
        8: {"type": "DI", "runall": "-d"},
        9: {"type": "Pivot", "runall": "-p"},
        10: {"type": "All Servers", "runall": "-A"},
    }

    os.system('clear -x')
    print(pyfiglet.figlet_format("Log Collector", font='slant'))
    title = "Choose the servers you wish to get logs from:"
    print(f"{title}\n" + '=' * len(title))

    for k, v in servers.items():
        print(f"{k:<2} - {v['type']}")
    result = get_selection(servers)
    runall_cmd = servers[result]['runall']
    print()

    run_this = True
    if run_this:
        Run_Gc_log = True
        Remove_GC_Log = False
        while Run_Gc_log == True:
            Remove_GC_Log = input('GC Logs required [Y/n] : ')
            if Remove_GC_Log == "":
                Remove_GC_Log = True
                Run_Gc_log = False
            elif (str(Remove_GC_Log.lower()) == "y" or str(Remove_GC_Log.lower()) == "n"):
                if (str(Remove_GC_Log.lower()) == "n"):
                    Remove_GC_Log = False
                else:
                    Remove_GC_Log = True
                Run_Gc_log = False
            else:
                Run_Gc_log = True
        print("Choose date & time window for your search (FORMAT: 2023-01-09 10:25:00)")
        while True:
            get_date_time('start')
            get_date_time('end')
            if epoch_start_date_time >= epoch_end_date_time:
                print(f'{Bcolors.FAIL}ERROR:{Bcolors.ENDC} End '
                'timestamp is expected to be greater than Start timestamp.')
            else:
                break

    # get a list of servers according to user selected options
    sh_cmd = f"runall {runall_cmd} -l | grep -v '==='"
    list_of_servers = str(subprocess.run([sh_cmd], shell=True, stdout=subprocess.PIPE).stdout.decode()).strip('\n').split('\n')

    print("LET'S GO >>>\n============")

    # we create logs collections base directory if not exists
    if not os.path.exists(LOCAL_LOGS_DIR):
        os.makedirs(LOCAL_LOGS_DIR, exist_ok=True)
        print(f"[ INFO ] Created logs base directory '{LOCAL_LOGS_DIR}'")

    # instantiate a temporary directory for log collections (format: [cluster]_[datetime])
    tmp_dir = f"{LOCAL_LOGS_DIR}/{servers[result]['type']}_{datetime.now()}".replace(' ','_').lower()
    if path_not_exists(tmp_dir):
        os.makedirs(tmp_dir, exist_ok=True)

    print(f"[ INFO ] Log files location: '{tmp_dir}'")
    for node in list_of_servers:
        node_path = f"{tmp_dir}/{node}"
        if path_not_exists(node_path):
            os.mkdir(node_path)
        # get file count
        sh_cmd = f'ssh {node} "find {REMOTE_LOGS_DIR} -type f | wc -l"'
        num_of_files = str(subprocess.run([sh_cmd], shell=True, stdout=subprocess.PIPE).stdout.decode()).strip('\n')
        # copy file to designated node
        sh_cmd = f"scp -qrC {node}:{REMOTE_LOGS_DIR}/* {node_path}/"
        # print(f'[ INFO ] Getting {num_of_files} log files from node {node} ... ', flush=True ,end="")
        print(f'[ INFO ] Getting log files from node {node} ... ', flush=True ,end="")
        subprocess.run([sh_cmd], shell=True)
        ListAllFiles = listAllScripts(tmp_dir + "/" +node)
        for i in range(len(ListAllFiles)):
            try:
                datetime_str = str(ListAllFiles[i])[0:16]
                datetime_object = datetime.strptime(datetime_str, '%Y-%m-%d~%H.%M')
                if ((str(start_date_time) <= str(datetime_object)) & (str(end_date_time) >= str(datetime_object))):
                    pass
                else:
                    os.remove(tmp_dir + "/" +node + "/" +ListAllFiles[i])
            except:
                if "gc_" in ListAllFiles[i] and Remove_GC_Log:
                    os.remove(tmp_dir + "/" +node + "/" +ListAllFiles[i])

        num_of_files = len(listAllScripts(tmp_dir + "/" +node))
        print(f'\n[ INFO ] Total {num_of_files} files done')
    print('\n')

    # TODO
    # add support for auxiliary operations such as obfuscate.sh

