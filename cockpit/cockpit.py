#!/usr/bin/python3
# *-* coding: utf-8 *-*

import os
import yaml
import subprocess
from signal import signal, SIGINT
from modules.functions import update_selections, pretty_print, \
    handler, print_header, check_settings, \
        print_locations, print_menu, validate_main_menu_input


def main():
    # catch user CTRL+C key press
    signal(SIGINT, handler)

    # check that COCKPIT_HOME is set and valid
    this_home = os.path.realpath(os.path.dirname(__file__))
    env_cockpit_home = os.environ.get('COCKPIT_HOME')
    if os.environ.get('COCKPIT_HOME') != this_home:
        print_header()
        pretty_print("ERROR: COCKPIT_HOME environment variable is not set or invalid!", 'red', 'bright')
        print(f"(!) set COCKPIT_HOME to: '{this_home}'\n    or run set_env.sh script located in cockpit directory.\n")
        exit(1)

    menu_yaml = f"{os.environ['COCKPIT_HOME']}/config/menu.yaml"
    config_yaml = f"{os.environ['COCKPIT_HOME']}/config/config.yaml"
    user_selections = []

    print_header()
    check_settings(config_yaml)
   
    # load menu yaml
    with open(menu_yaml, 'r') as yf:
        yd = yaml.safe_load(yf)

    while True:
        if len(user_selections) != 0: 
            dict = eval("yd[" + ']['.join(user_selections) + "]")
        else:
            dict = yd
        print_locations(user_selections, yd)
        if dict['type'] == 'command':
            # checking that exec-type key is set
            if dict['exec-type'] == '':
                pretty_print(f"YAML ERROR: missing 'exec-type' value in command '{dict['id']}'", 'red')
                input("press ENTER to go back to menu")
                user_selections.pop()
                continue
            # checking that exec key is set
            if dict['exec'] == '':
                pretty_print(f"YAML ERROR: missing 'exec' value in command '{dict['id']}'", 'red')
                input("press ENTER to go back to menu")
                user_selections.pop()
                continue
            if dict['exec-type'] == 'module':
                eval(f"{dict['exec']}()")
            if dict['exec-type'] == 'script':
                script = f"{os.environ['COCKPIT_HOME']}/modules/{dict['exec']}"
                subprocess.call([script], shell=True)
            user_selections.pop()
            continue
        print_menu(dict)
        validate_main_menu_input(dict, user_selections)


if __name__ == '__main__':
    main()
