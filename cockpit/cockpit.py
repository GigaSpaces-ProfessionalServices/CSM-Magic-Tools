#!/usr/bin/python3
# *-* coding: utf-8 *-*


import os
import yaml
import subprocess
from modules.functions import *


def validate_input(the_dict, the_selections):
    '''
    ensure user choice is valid
    :param the_dict: a dictionary of choices
    :param the_selections: the list of choices
    '''
    the_choice = input("\nEnter your choice: ")
    while True:
        if the_choice == '99':
            if the_dict['id'] == 'Main':
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
                #cwd = os.path.abspath(os.path.dirname(__file__))
                script = f"{os.path.abspath(os.path.dirname(__file__))}/modules/{dict['exec']}"
                subprocess.call([script], shell=True)
            user_selections.pop()
            continue
        print_menu(dict)
        validate_input(dict, user_selections)
