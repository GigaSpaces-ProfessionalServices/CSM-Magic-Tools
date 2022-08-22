#!/usr/bin/python3
# *-* coding: utf-8 *-*


import pyfiglet
import subprocess
from colorama import Fore, Style
from signal import SIGINT, signal
import sqlite3
import yaml
from modules.commands import *

def handler(signal_recieved, frame):
    print('\n\nOperation aborted by user!')
    exit(0)


def update_selections(val, list):
    '''
    @param : val - last value selected by user
    @param : list - the list of selections 
    @returns : list - the list of selections 
    '''
    if val == '99':
        list.pop()
    else:
        list.append(val)


def print_locations(selections, dictionary):
    flen = 56
    v_pref = ' '*2
    version = "ODS Cockpit 2022, v1.0 | Copyright © Gigaspaces Ltd.\n"
    index = ""
    location = f" @MAIN".upper()
    for i in selections:
        index += f"[{str(i)}]"
        location += " :: " + str(eval(f"dictionary{index}['id']")).upper()
    styled_str = f"{Fore.GREEN}{Style.BRIGHT}{location}{Style.RESET_ALL}"
    subprocess.run("clear")
    print(pyfiglet.figlet_format("ODS Cockpit", font='slant'))
    print(f"{v_pref}{version}\n\n")
    print('=' * (len(location) + 1) + f"\n{styled_str}\n" + '=' * (len(location) + 1), "\n")


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
            the_choice = input(f"{Fore.RED}ERROR: Input must be a menu index!{Fore.RESET}\nEnter you choice: ")
        else:
            update_selections(the_choice, the_selections)
            break


def print_menu(dict):
    for k in dict.keys():
        if str(k).isdigit():
            print(f"{f'[{k}]':<4} - {dict[k]['id']}")    
    print(f"{'[99]':<4} - ESC")


if __name__ == '__main__':
    # catch user CTRL+C key press
    signal(SIGINT, handler)

    menu_file = "./config/menu.yaml"
    user_selections = []  

    # load yaml
    with open(menu_file, 'r') as yf:
        yd = yaml.safe_load(yf)

    while True:
        if len(user_selections) != 0: 
            dict = eval("yd[" + ']['.join(user_selections) + "]")
        else:
            dict = yd
        print_locations(user_selections, yd)
        if dict['type'] == 'command':
            if dict['exec-type'] == 'module':
                eval(f"{dict['exec']}")
            if dict['exec-type'] == 'script':
                pass
            user_selections.pop()
            continue
        print_menu(dict)
        validate_input(dict, user_selections)
        