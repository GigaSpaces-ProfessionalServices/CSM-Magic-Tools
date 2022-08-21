#!/usr/bin/python3
# *-* coding: utf-8 *-*

import pyfiglet
import subprocess
from colorama import Fore, Style
from signal import SIGINT, signal
import sqlite3
#import yaml


def handler(signal_recieved, frame):
    print('\n\nOperation aborted by user!')
    exit(0)


def exec_menu(dict):
    if 'attr' in dict.keys():
        if dict['attr'] != '':
            eval(f"{dict['cmd']}('{dict['attr']}')")
        else:
            eval(f"{dict['cmd']}()")
    else:    
        for k,v in dict.items():
            if str(k).isdigit():
                print(f"{f'[{k}]':<4} - {v}")
    print(f"{'[99]':<4} - ESC")


def print_selections(list):
    flen = 60
    v_pref = ' ' * 2
    version = "ODS Cockpit 2022, v1.0 | Copyright © Gigaspaces Ltd."
    selections = f"@MAIN".upper()
    if len(list) != 0:
        for i in list:
            selections += f" :: {i}".upper()
    styled_str = f"{Fore.GREEN}{Style.BRIGHT}{selections}{Style.RESET_ALL}"
    print(f"{v_pref}{version}\n")
    print(f"{styled_str}\n" + '=' * flen, "\n")


def validate_input(the_dict):
    the_choice = input("\nEnter your choice: ")
    while True:
        if the_choice == '99':
            if the_dict['parent'] == '':
                exit(0)
            else:
                return the_choice
        if not the_choice.isdigit() or int(the_choice) not in dict.keys():
            the_choice = input(f"{Fore.RED}ERROR: Input must be a menu index!{Fore.RESET}\nEnter you choice: ")
        else:
            return the_choice


def is_cmd_menu(the_menu):
    if 'attr' in the_menu.keys():
        return True


# tasks functions
def list_tasks():
    print("listing tasks...")
    input("\nPress Enter to continue...")


def show_task():
    list_tasks()
    print("showing selected task")
    input("\nPress Enter to continue...")


def create_task():
    print("creating new task... ")
    input("\nPress Enter to continue...")


def edit_task(attribute):
    list_tasks()
    print(f"Executing: '{attribute}' ...")
    input("\nPress Enter to continue...")


def delete_task():
    list_tasks()
    print("deleting task #...")
    input("\nPress Enter to continue...")


# jobs functions
def list_jobs():
    print("listing jobs...")
    input("\nPress Enter to continue...")


def show_job():
    list_jobs()
    print("showing selected job")
    input("\nPress Enter to continue...")


def create_job(attribute):
    print(f"Executing: '{attribute}' ...")
    input("\nPress Enter to continue...")


def edit_job(attribute):
    list_jobs()
    print(f"Executing: '{attribute}' ...")
    input("\nPress Enter to continue...")


def delete_job():
    list_jobs()
    print("deleting job #...")
    input("\nPress Enter to continue...")


# policies functions
def list_policies():
    print("listing policies...")
    input("\nPress Enter to continue...")


def show_policy():
    list_policies()
    print("showing selected policy")
    input("\nPress Enter to continue...")


def create_policy():
    print("creating new policy... ")
    input("\nPress Enter to continue...")


def edit_policy(attribute):
    list_policies()
    print(f"Executing: '{attribute}' ...")
    input("\nPress Enter to continue...")


def delete_policy():
    list_policies()
    print("deleting policy #...")
    input("\nPress Enter to continue...")


# reports functions
def list_report():
    print("listing last 15...")
    input("\nPress Enter to continue...")


def show_report():
    print("showing executions...")
    input("\nPress Enter to continue...")


def cancelled_report():
    print("creating new policy... ")
    input("\nPress Enter to continue...")


def monitor_report():
    print("showing jobs... ")
    input("\nPress Enter to continue...")


# settings functions
def prod_settings():
    print("listing settings for prod...")
    input("\nPress Enter to continue...")


def dr_settings():
    print("listing settings for dr...")
    input("\nPress Enter to continue...")


def cockpit_settings():
    print("listing settings for cockpit...")
    input("\nPress Enter to continue...")

if __name__ == '__main__':
    # catch user CTRL+C key press
    signal(SIGINT, handler)

    user_selections = []
    main_menu = {'parent': '', 1: 'Tasks', 2: 'Jobs', 3: 'Policies', 4: 'Reporting', 5: 'Settings'}
    # tasks
    tasks_menu = {'parent': 'main_menu', 1: 'Tasks Catalog', 2: 'Create New Task', 3: 'Edit Task', 4: 'Delete Task'}
    tasks_catalog_menu = {'parent': 'tasks_menu', 1: 'List Tasks', 2: 'Show Task'}
    tasks_create_menu = {'parent': 'tasks_menu', 'cmd': 'create_task', 'attr': ''}
    tasks_edit_menu = {'parent': 'tasks_menu', 1: 'Task Name', 2: 'Task Policy', 3: 'Activate Task', 
    4: 'Deactivate Task', 5: 'Add Job to Task', 6: 'Remove Job from Task', 7: 'Show Task Metadata'}
    tasks_delete_menu = {'parent': 'tasks_menu', 'cmd': 'delete_task', 'attr': ''}
    tasks_catalog_list_menu = {'parent': 'tasks_catalog_menu', 'cmd': 'list_tasks', 'attr': ''}
    tasks_catalog_show_menu = {'parent': 'tasks_catalog_menu', 'cmd': 'show_task', 'attr': ''}
    tasks_edit_sub_menu = {'parent': 'tasks_edit_menu', 'cmd': 'edit_task', 'attr': ''}

    # jobs
    jobs_menu = {'parent': 'main_menu', 1: 'Jobs Catalog', 2: 'Create New Job', 3: 'Edit Job', 4: 'Delete Job'}
    jobs_catalog_menu = {'parent': 'jobs_menu', 1: 'List Jobs', 2: 'Show Job'}
    jobs_create_menu = {'parent': 'jobs_menu',1: 'Validate', 2: 'Start Feeder', 3: 'Pause Feeder', 4: 'Deploy Feeder'}
    jobs_edit_menu = {'parent': 'jobs_menu', 1: 'Job Name', 2: 'Job Command', 3: 'Job Destination', 4: 'Show Job Metadata'}
    jobs_delete_menu = {'parent': 'jobs_menu', 'cmd': 'delete_job', 'attr': ''}
    jobs_catalog_list_menu = {'parent': 'jobs_catalog_menu', 'cmd': 'list_jobs', 'attr': ''}
    jobs_catalog_show_menu = {'parent': 'jobs_catalog_menu', 'cmd': 'show_job', 'attr': ''}
    jobs_create_sub_menu = {'parent': 'jobs_create_menu', 'cmd': 'create_job', 'attr': ''}
    jobs_edit_sub_menu = {'parent': 'jobs_edit_menu', 'cmd': 'edit_job', 'attr': ''}

    # policies
    policies_menu = {'parent': 'main_menu', 1: 'Policies Catalog', 2: 'Create New Policy', 3: 'Edit Policy', 4: 'Delete Policy'}
    policies_catalog_menu = {'parent': 'policies_menu', 1: 'List Policies', 2: 'Show Policy'}
    policies_create_menu = {'parent': 'policies_menu', 'cmd': 'create_policy', 'attr': ''}
    policies_edit_menu = {'parent': 'policies_menu', 1: 'Policy Name', 2: 'Policy Schedule', 3: 'Policy Retry', 4: 'Activate Policy', 5: 'Deactivate Policy'}
    policies_delete_menu = {'parent': 'policies_menu', 'cmd': 'delete_policy', 'attr': ''}
    policies_catalog_list_menu = {'parent': 'policies_catalog_menu', 'cmd': 'list_policies', 'attr': ''}
    policies_catalog_show_menu = {'parent': 'policies_catalog_menu', 'cmd': 'show_policy', 'attr': ''}
    policies_create_sub_menu = {'parent': 'policies_create_menu', 'cmd': 'create_policy', 'attr': ''}
    policies_edit_sub_menu = {'parent': 'policies_edit_menu', 'cmd': 'edit_policy', 'attr': ''}

    # reporting
    reports_menu = {'parent': 'main_menu', 1: 'Task Execution', 2: 'Task Cancellation', 3: 'Jobs Monitor'}
    reports_execution_menu = {'parent': 'reports_menu', 1: 'Last 15 Tasks', 2: 'Show Executions'}
    reports_cancelled_menu = {'parent': 'reports_menu', 'cmd': 'cancelled_report', 'attr': ''}
    reports_monitor_menu = {'parent': 'reports_menu', 'cmd': 'monitor_report', 'attr': ''}
    reports_execution_list_menu = {'parent': 'reports_execution_menu', 'cmd': 'list_report', 'attr': ''}
    reports_execution_show_menu = {'parent': 'reports_execution_menu', 'cmd': 'show_report', 'attr': ''}

    # settings
    settings_menu = {'parent': 'main_menu', 1: 'IP/Endpoint for PROD', 2: 'IP/Endpoint for DR', 3: 'Cockpit Database'}
    settings_prod_menu = {'parent': 'settings_menu', 'cmd': 'prod_settings', 'attr': ''}
    settings_dr_menu = {'parent': 'settings_menu', 'cmd': 'dr_settings', 'attr': ''}
    settings_cockpit_menu = {'parent': 'settings_menu', 'cmd': 'cockpit_settings', 'attr': ''}

    # build a list of attributes for corresponding functions
    tasks_edit_attributes = [ tasks_edit_menu[x] for x in tasks_edit_menu.keys() if x != 'parent']
    jobs_create_attributes = [ jobs_create_menu[x] for x in jobs_create_menu.keys() if x != 'parent']
    jobs_edit_attributes = [ jobs_edit_menu[x] for x in jobs_edit_menu.keys() if x != 'parent']
    policies_edit_attributes = [ policies_edit_menu[x] for x in policies_edit_menu.keys() if x != 'parent']

    dict = main_menu
    while True:
        subprocess.run("clear")
        print(pyfiglet.figlet_format("ODS Cockpit", font='slant'))
        print_selections(user_selections)
        exec_menu(dict)
        if 'attr' in dict.keys():
            dict['attr'] = ''
            user_selections.pop()
            dict = eval(dict['parent'])
            continue
        choice = validate_input(dict)
        if choice == '99':
            user_selections.pop()
            dict = eval(dict['parent'])
            continue
        else:
            user_selections.append(dict[int(choice)])
            # main
            if dict[int(choice)] == 'Tasks':
                dict = tasks_menu
                continue
            if dict[int(choice)] == 'Jobs':
                dict = jobs_menu
                continue
            if dict[int(choice)] == 'Policies':
                dict = policies_menu
                continue
            if dict[int(choice)] == 'Reporting':
                dict = reports_menu
                continue
            if dict[int(choice)] == 'Settings':
                dict = settings_menu
                continue
            # tasks
            if dict[int(choice)] == 'Tasks Catalog':
                dict = tasks_catalog_menu
                continue
            if dict[int(choice)] == 'Create New Task':
                dict = tasks_create_menu
                continue
            if dict[int(choice)] == 'Edit Task':
                dict = tasks_edit_menu
                continue
            if dict[int(choice)] == 'Delete Task':
                dict = tasks_delete_menu
                continue
            if dict[int(choice)] == 'List Tasks':
                dict = tasks_catalog_list_menu
                continue
            if dict[int(choice)] == 'Show Task':
                dict = tasks_catalog_show_menu
                continue
            if dict[int(choice)] in tasks_edit_attributes:
                the_attr = dict[int(choice)]
                dict = tasks_edit_sub_menu
                dict['attr'] = the_attr
                continue
            # jobs
            if dict[int(choice)] == 'Jobs Catalog':
                dict = jobs_catalog_menu
                continue
            if dict[int(choice)] == 'Create New Job':
                dict = jobs_create_menu
                continue
            if dict[int(choice)] == 'Edit Job':
                dict = jobs_edit_menu
                continue
            if dict[int(choice)] == 'Delete Job':
                dict = jobs_delete_menu
                continue
            if dict[int(choice)] == 'List Jobs':
                dict = jobs_catalog_list_menu
                continue
            if dict[int(choice)] == 'Show Job':
                dict = jobs_catalog_show_menu
                continue
            if dict[int(choice)] in jobs_create_attributes:
                the_attr = dict[int(choice)]
                dict = jobs_create_sub_menu
                dict['attr'] = the_attr
                continue
            if dict[int(choice)] in jobs_edit_attributes:
                the_attr = dict[int(choice)]
                dict = jobs_edit_sub_menu
                dict['attr'] = the_attr
                continue
            # policies
            if dict[int(choice)] == 'Policies Catalog':
                dict = policies_catalog_menu
                continue
            if dict[int(choice)] == 'Create New Policy':
                dict = policies_create_menu
                continue
            if dict[int(choice)] == 'Edit Policy':
                dict = policies_edit_menu
                continue
            if dict[int(choice)] == 'Delete Policy':
                dict = policies_delete_menu
                continue
            if dict[int(choice)] == 'List Policies':
                dict = policies_catalog_list_menu
                continue
            if dict[int(choice)] == 'Show Policy':
                dict = policies_catalog_show_menu
                continue
            if dict[int(choice)] in policies_edit_attributes:
                the_attr = dict[int(choice)]
                dict = policies_edit_sub_menu
                dict['attr'] = the_attr
                continue        
            # reports
            if dict[int(choice)] == 'Tasks Execution':
                dict = reports_execution_menu
                continue
            if dict[int(choice)] == 'Task Cancellation':
                dict = reports_cancelled_menu
                continue
            if dict[int(choice)] == 'Jobs Monitor':
                dict = reports_monitor_menu
                continue
            if dict[int(choice)] == 'Last 15 Tasks':
                dict = reports_execution_list_menu
                continue
            if dict[int(choice)] == 'Show Executions':
                dict = reports_execution_show_menu
                continue
            # settings
            if dict[int(choice)] == 'IP/Endpoint for PROD':
                dict = settings_prod_menu
                continue
            if dict[int(choice)] == 'IP/Endpoint for DR':
                dict = settings_dr_menu
                continue
            if dict[int(choice)] == 'Cockpit Database':
                dict = settings_cockpit_menu
                continue


# def task_catalog_list():
#     print("listing all tasks ...")

# def task_catalog_show():
#     print("showing task ...")

# def update_selections(val, list):
#     '''
#     @param : val - last value selected by user
#     @param : list - the list of selections 
#     @returns : list - the list of selections 
#     '''
#     if val == "99":
#         list.pop()
#     else:
#         list.append(val)
#     #return list

# def locations(list):
#     suffix = " MAIN MENU > "
#     location = ""
#     if len(list) == 0:
#         location = suffix
#         print(location)
#     else:
#         for i in len(list):
#             location += f"{yd[i]['id']}"

# def print_keys(dict):
#     print(dict)
#     for i in range(1, len(dict)+1):
#         print(f"{i} - {dict[i]['id']}")


# def print_menu(dict):
#     # for i in dict.keys():
#     #     if str(i).isdigit():
#     #         print(f"{f'[{i}]':<4} - {dict[i]['id']}")
#     for i in dict.keys():
#         print(i)
#     if len(dict) != 0:
#         print(f"{'[99]':<4} - ESC")


# def print_menu_old(dict):
#     print(f"\nuser selections is now: {user_selections}")
#     # for i in dict.keys():
#     #     if str(i).isdigit():
#     #         print(f"{f'[{i}]':<4} - {dict[i]['id']}")
#     #     else:
#     #         print(f"{i} is not in menu")
#     for i in dict.values():
#         print(i)
#     exit(0)
#     if len(dict) != 0:
#         print(f"{'[99]':<4} - ESC")
#     choice = input("Enter you choice: ")
#     while True:
#         if not choice.isdigit():
#             choice = input("error: input not a number!\nEnter you choice: ")
#         elif int(choice) not in dict.keys() and int(choice) != 99 :
#             choice = input("error: input out of bounds!\nEnter you choice: ")
#         elif choice == '99':
#             if len(user_selections) == 0:
#                 print("exiting...")
#                 exit(0)
#             else:
#                 user_selections.pop()
#                 print(f"after popping. selections = {user_selections}")
#                 return
#         else:
#             choice = int(choice)
#             break
#     user_selections.append(choice)
#     if len(user_selections) != 0:
#         for c in user_selections:
#             dict = eval('dict' + f'[{c}]')
#     #update_selections(choice, user_selections)
#     # dict = eval('dict' + f'[{choice}]')
#     print_menu(dict)
#     return

    
# menu_file = "menu.yaml"
# user_selections = []    


# # load yaml
# with open(menu_file, 'r') as yf:
#     yd = yaml.safe_load(yf)


# print(yd[1]['name'][1]['name'])

# exit(0)
# for i in yd[1]:
#     print(i)
#     #print(yd[i]['name'], "\n\n")
# #print(yd[1], "\n\n")
# # print(yd[1][1], "\n\n")
# exit(0)

# while True:
#     subprocess.run("clear")
#     print(pyfiglet.figlet_format("ODS Cockpit"))
#     print(f"\nuser selections = {user_selections}")
#     if len(user_selections) != 0:
#         for c in user_selections:
#             dict = eval("dict" + f"[{c}]")
#     else:
#         dict = yd
#     print_menu(dict)

#     choice = input("Enter you choice: ")
#     if not choice.isdigit():
#         choice = input("error: input not a number!\nEnter you choice: ")
#     elif int(choice) not in dict.keys() and int(choice) != 99 :
#         choice = input("error: input out of bounds!\nEnter you choice: ")
#     elif choice == '99':
#         if len(user_selections) == 0:
#             break
#         else:
#             user_selections.pop()
#             print(f"after popping. selections = {user_selections}")
#     else:
#         choice = int(choice)
#         user_selections.append(choice)