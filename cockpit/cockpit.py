#!/usr/bin/python3
# *-* coding: utf-8 *-*


import os
import yaml
import subprocess
from modules.commands import *
from modules.func import *


if __name__ == '__main__':
    # catch user CTRL+C key press
    signal(SIGINT, handler)

    menu_yaml = f"{os.path.dirname(os.path.abspath(__file__))}/config/menu.yaml"
    config_yaml = f"{os.path.dirname(os.path.abspath(__file__))}/config/config.yaml"
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
                cwd = os.path.abspath(os.path.dirname(__file__))
                script = f"{cwd}/scripts/{dict['exec']}"
                subprocess.call([script], shell=True)
            user_selections.pop()
            continue
        print_menu(dict)
        validate_input(dict, user_selections)
