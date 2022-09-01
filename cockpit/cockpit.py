#!/usr/bin/python3
# *-* coding: utf-8 *-*


import os
from signal import SIGINT, signal
import yaml
import pyfiglet
import subprocess
from colorama import Fore, Style
import sqlite3
from modules.commands import *


def handler(signal_recieved, frame):
    print('\n\nOperation aborted by user!')
    exit(0)


def print_header():
    v_pref = ' ' * 2
    version = "ODS Cockpit 2022, v1.0 | Copyright Gigaspaces Ltd"
    subprocess.run("clear")
    print(pyfiglet.figlet_format("ODS Cockpit", font='slant'))
    print(f"{v_pref}{version}\n\n")


def pretty_print(string, color, style=None):
    color = eval('Fore.' + f'{color}'.upper())
    if style is None:
        print(f"{color}{string}{Style.RESET_ALL}")
    else:
        style = eval('Style.' + f'{style}'.upper())
        print(f"{color}{style}{string}{Style.RESET_ALL}")


def print_locations(selections, dictionary):
    index = ""
    location = f"@:: MAIN".upper()
    for i in selections:
        index += f"[{str(i)}]"
        location += " :: " + str(eval(f"dictionary{index}['id']")).upper()
    print_header()
    pretty_print(f'{location}\n', 'green', 'bright')


def print_menu(dict):
    for k in dict.keys():
        if str(k).isdigit():
            index = f"[{k}]"
            item = f"{dict[k]['id']}"
            if dict[k]['description'] != '':
                desc = f"- {dict[k]['description']}"
            else:
                desc = ""
            print(f'{index:<4} - {item:<24}{desc:<20}')
    print(f"{'[99]':<4} - {'ESC':<24}{'- Go Back / Exit ':<20}")


def update_selections(a_choice, choices_list):
    if a_choice == '99':
        choices_list.pop()
    else:
        choices_list.append(a_choice)


def validate_input(the_dict, the_selections):
    the_choice = input("\nEnter your choice: ")
    while True:
        if the_choice == '99':
            if the_dict['id'] == 'Main':
                exit(0)
            else:
                update_selections(the_choice, the_selections)
                break
        if not the_choice.isdigit() or int(the_choice) not in dict.keys():
            pretty_print('ERROR: Input must be a menu index!', 'red')
            the_choice = input("Enter you choice: ")
        else:
            update_selections(the_choice, the_selections)
            break


def check_settings(menu, config):
    db_set_required = False
    env_set_required = False
    # load cockpit configuration
    with open(config, 'r') as yf:
        data = yaml.safe_load(yf)

    # get user acceptance to run
    def get_user_permission(question):
        q = f"{question} [yes/no]: "
        answer = input(q)
        while True:
            if answer in ['yes', 'Yes', 'YES']: return True
            elif answer in ['no', 'No', 'NO']: return False
            else: answer = input("invlid input! type 'yes' or 'no': ")

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
            subprocess.call(['./create_db.py'], shell=True)
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
                        script = f"./scripts/get_{env_name}_params.py"
                        subprocess.call([script], shell=True)
                # reload cockpit configuration after changes
                with open(config, 'r') as yf:
                    data = yaml.safe_load(yf)
            else:
                print(f"\nplease set required parameters in: '{config_yaml}'\n")
                exit()
            config_ok = True
            for env_name in data['params']:
                if env_name != 'cockpit':
                    pivot = data['params'][env_name]['endpoints']['pivot']
                    if pivot == '' or pivot is None:
                        config_ok = False
                        break
    if db_set_required or env_set_required:
        pretty_print("\nSetup and verification of required settings completed successfully.", 'green')
        input("Press ENTER to continue to the main menu.")


if __name__ == '__main__':
    # catch user CTRL+C key press
    signal(SIGINT, handler)

    menu_yaml = f"{os.path.dirname(os.path.abspath(__file__))}/config/menu.yaml"
    config_yaml = f"{os.path.dirname(os.path.abspath(__file__))}/config/config.yaml"
    
    print_header() 
    check_settings(menu_yaml, config_yaml)
   
    user_selections = []  

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
