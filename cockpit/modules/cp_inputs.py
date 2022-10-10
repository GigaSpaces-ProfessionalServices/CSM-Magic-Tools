#!/usr/bin/python3
# *-* coding: utf-8 *-*

"""
cp_inputs: user input related collection of functions
"""

import os
import sys
import tty
import termios
from colorama import Fore, Style


def press_any_key():
    """
    pause until any key is pressed
    """

    _title = "Press any key to continue..."
    cmd = f"/bin/bash -c 'read -s -n 1 -p \"{_title}\"'"
    print('\n')
    os.system(cmd)
    print('\n')


def get_keypress():
    """ catch keypress"""

    old_settings = termios.tcgetattr(sys.stdin)
    tty.setcbreak(sys.stdin.fileno())
    key_mapping = {
        10: 'return',
        27: 'esc',
        127: 'backspace'
        }
    user_input = []
    while True:
        _b = os.read(sys.stdin.fileno(), 3).decode()
        if len(_b) == 3:
            k = ord(_b[2])
        else:
            k = ord(_b)
        this_key = key_mapping.get(k, chr(k))
        if this_key == 'return':
            break
        if this_key == 'esc':
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


def validate_navigation_select(_items_dict, _the_selections):
    """
    ensure user choice is valid and update selections list
    :param the_dict: a dictionary of available choices
    :param _the_selections: list of current user selections
    :return:
    """

    # print menu
    is_main_menu = False
    for key, val in _items_dict.items():
        if val == 'Main':
            is_main_menu = True
        if str(key).isdigit():
            index = f"[{key}]"
            item = f"{val['id']}"
            if val['description'] != '':
                description = f"- {val['description']}"
            else:
                description = ""
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
            if _items_dict['id'] == 'Main':
                sys.exit(0)
            else:
                update_selections(k, _the_selections)
                break
        if not k.isdigit() or int(k) not in _items_dict.keys():
            print(f'{Fore.RED}ERROR: Input must be a menu index!{Style.RESET_ALL}')
            continue
        update_selections(k, _the_selections)
        break


def validate_option_select(_items_dict, _title, _esc_to='Go Back'):
    """
    validate user choices from menu
    :param _items_dict: dictionary of menu items
    :param _title: the menu title printed at the start of menu
    :return: list of user choices
    """

    # check if choice in range
    def choice_ok(value, limit):
        if not value.isdigit() or int(value) < 1 or int(value) > limit:
            return False
        return True

    # build a reference dictionary for menu
    i, menu_indices = 1, {}
    for key in _items_dict.keys():
        menu_indices[i] = key
        i += 1

    # print submenu
    note = "(!) collections are supported (i.e: 1,3,2-5)"
    print(f"{_title}\n{note}")
    print('-' * len(note))
    for key, val in menu_indices.items():
        index = f"[{key}]"
        print(f'{index:<4} - {_items_dict[val][0]:<24}')
    print('-' * 32)
    print(f'{"Esc":<4} - to {_esc_to}')
    print('\n')

    # parse selections
    try:
        while True:
            valid_selections = []
            k = get_keypress()
            if k == 'esc':
                valid_selections.append(-1)
                return valid_selections
            selected_ok = False
            selected = k.split(',')
            for item in selected:
                if '-' in item: # if input is a range
                    range_select = item.split('-')
                    while '' in range_select:
                        range_select.remove('')
                    if len(range_select) != 2:
                        selected_ok = False
                        break
                    # verifying all elements of input are digits
                    range_select_check = [c for c in range_select if c.isdigit()]
                    if len(range_select) != len(range_select_check):
                        selected_ok = False
                        break

                    # populating valid selections list 
                    for i in range(int(range_select[0]), int(range_select[1])+1):
                        if choice_ok(str(i), len(_items_dict)):
                            selected_ok = True
                            valid_selections.append(menu_indices[i])
                        else:
                            selected_ok = False
                            break
                elif choice_ok(item, len(_items_dict)):
                    selected_ok = True
                    valid_selections.append(menu_indices[int(item)])
                else:
                    selected_ok = False
                    break
            if selected_ok:
                return list(set(valid_selections))
            print(f'{Fore.RED}ERROR: Input must be a menu index!{Style.RESET_ALL}')
    except (KeyboardInterrupt, SystemExit):
        os.system('stty sane')


def validate_type_select(_items_dict):
    """
    get object type selection from user
    :param the_dict: menu dictionary object
    :return: int of user choice
    """

    # print submenu
    _title = "What type of task do you want to create?"
    print(_title + "\n" + '-' * len(_title))
    for key, val in _items_dict.items():
        index = f"[{key}]"
        print(f'{index:<4} - {val["name"]:<24} {val["description"]:<34}')
    print(f'\n{"Esc":<4} - to Go Back')
    print('-' * 32)
    try:
        while True:
            k = get_keypress()
            if k == 'esc':
                return -1
            if not k.isdigit() or int(k) not in _items_dict.keys():
                print(f'{Fore.RED}ERROR: Input must be a menu index!{Style.RESET_ALL}')
            else:
                return int(k)
    except (KeyboardInterrupt, SystemExit):
        os.system('stty sane')
        return None


def update_selections(_the_choice, _choices_list):
    """
    update user selections list
    :param _the_choice: the user choice
    :param _choices_list: the choices options
    :return:
    """
    if _the_choice == 'esc':
        _choices_list.pop()
    else:
        _choices_list.append(_the_choice)


# get user acceptance to run
def get_user_ok(_question):
    """
    ask for user permission to proceed
    :param _question: the question in subject
    :return: True / False
    """

    try:
        answer = input(f"{_question} [yes/no]: ").lower()
        while True:
            if answer == 'yes':
                return True
            if answer == 'no':
                return False
            answer = input("invlid input! type 'yes' or 'no': ")
    except (KeyboardInterrupt, SystemExit):
        os.system('stty sane')
        return None
