#!/usr/bin/python3
# *-* coding: utf-8 *-*

"""
set_env: set required environment variables
"""

import os
import subprocess
from colorama import Fore, Style

def cockpit_env_is_set():
    """
    set cockpit environment variables
    """

    def build_pythonpath(_add_path):
        """
        build the environment variable for PYTHONPATH
        :param _add_path: the path to add to the variable
        :return ppath: value string for the variable
        """
        if os.environ.get('PYTHONPATH') is not None:       # concatenate if PYTHOPATH not empty
            if _add_path not in os.environ['PYTHONPATH'].split(':'):
                return ':'.join([os.environ['PYTHONPATH'], _add_path])
            return os.environ['PYTHONPATH']
        return _add_path

    def source_rc():
        """
        source bashrc file
        """

        try:
            answer = input("Restart now? [yes / no]: ").lower()
            while True:
                if answer == 'yes':
                    print('\n')
                    subprocess.run(["/bin/bash", "-i", "-c", f"source {RC_FILE}"], check=True)
                    subprocess.run(["/bin/bash", "-i", "-c", "exec bash"], check=True)
                    break
                if answer == 'no':
                    break
                answer = input("invlid input! type 'yes' or 'no': ")
        except (KeyboardInterrupt, SystemExit):
            os.system('stty sane')
            return False
        else:
            return True

    try:
        # initiate variables
        THIS_PATH = os.path.realpath(os.path.dirname(__file__)) # = COCKPIT_HOME
        RC_FILE = os.path.expanduser('~/.bashrc')
        ENV_VARS = {
            'COCKPIT_HOME': f'{THIS_PATH}',
            'PYTHONPATH': f'{build_pythonpath(THIS_PATH)}',
            'PIVOT_PRD': '1.1.1.1',
            'PIVOT_DR': '2.2.2.2'
        }
        absent_vars = False
        for var, val in ENV_VARS.items():
            with open(RC_FILE, 'r', encoding='utf-8') as rcf_r:
                if var not in rcf_r.read():
                    # adding export line for this var
                    with open(RC_FILE, 'a', encoding='utf-8') as rcf_a:
                        rcf_a.write(f"export {var}={val}\n")
                    absent_vars = True
        if absent_vars:
            print(f"\n{Fore.GREEN}(!) missing required environment variables have been updated\
                {Style.RESET_ALL}".upper())
            print("    cockpit must be restarted for the changes to take affect.".upper())
            return source_rc()
        # vars in rc file so we check environment vars are available (= set)
        absent_vars = [var for var in ENV_VARS if os.environ.get(var) is None]
        if len(absent_vars) > 0:
            print(f"\n{Fore.RED}ERROR: the following environment variables are not set:\
                {Style.RESET_ALL}")
            for var in absent_vars:
                print(f"   - {var}")
            print("\n(!) cockpit must be restarted for the changes to take affect.".upper())
            return source_rc()
        return True
    except (KeyboardInterrupt, SystemExit):
        os.system('stty sane')
        return False
