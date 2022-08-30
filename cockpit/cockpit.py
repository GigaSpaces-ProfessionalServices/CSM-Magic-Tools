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


def print_locations(selections, dictionary):
    v_pref = ' ' * 2
    version = "ODS Cockpit 2022, v1.0 | Copyright Gigaspaces Ltd"
    index = ""
    location = f"@:: MAIN".upper()
    for i in selections:
        index += f"[{str(i)}]"
        location += " :: " + str(eval(f"dictionary{index}['id']")).upper()
    styled_str = f"{Fore.GREEN}{Style.BRIGHT}{location}{Style.RESET_ALL}"
    #subprocess.run("clear")
    #print(pyfiglet.figlet_format("ODS Cockpit", font='slant'))
    print(f"{v_pref}{version}\n\n")
    print(f"{styled_str}\n")


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
            the_choice = input(f"{Fore.RED}ERROR: Input must be a menu index!{Fore.RESET}\nEnter you choice: ")
        else:
            update_selections(the_choice, the_selections)
            break


if __name__ == '__main__':
    # catch user CTRL+C key press
    signal(SIGINT, handler)

    menu_file = f"{os.path.dirname(os.path.abspath(__file__))}/config/menu.yaml"
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
            # checking that exec-type key is set
            if dict['exec-type'] == '':
                print(f"{Fore.RED}YAML ERROR: missing 'exec-type' value in command '{dict['id']}'.{Fore.RESET}")
                input("press ENTER to go back to menu")
                user_selections.pop()
                continue
            # checking that exec key is set
            if dict['exec'] == '':
                print(f"{Fore.RED}YAML ERROR: missing 'exec' value in command '{dict['id']}'.{Fore.RESET}")
                input("press ENTER to go back to menu")
                user_selections.pop()
                continue
            if dict['exec-type'] == 'module':
                eval(f"{dict['exec']}()")
            if dict['exec-type'] == 'script':
                script = f"./scripts/{dict['exec']}"
                subprocess.call([script], shell=True)
            user_selections.pop()
            continue
        print_menu(dict)
        validate_input(dict, user_selections)
