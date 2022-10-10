#!/usr/bin/python3
# *-* coding: utf-8 *-*

"""
cp_init: checking cockpit settings upon initialization
"""

import os
import sys
import subprocess
import yaml
from colorama import Fore, Style
from .classes import MySQLite, Spinner
from .cp_inputs import (
    get_user_ok,
    press_any_key
)
from .cp_print import(
    pretty_print,
    print_header
)
from .cp_utils import get_object_types_from_space


def check_settings(config_yaml):
    """
    check settings of cockpit upon initialization
    """
    db_set_required = False
    env_set_required = False
     
    # load config yaml
    with open(config_yaml, 'r', encoding="utf-8") as yml:
        data = yaml.safe_load(yml)

    COCKPIT_DB_HOME = data['params']['cockpit']['db_home']
    COCKPIT_DB_NAME = data['params']['cockpit']['db_name']
    COCKPIT_DB = f"{COCKPIT_DB_HOME}/{COCKPIT_DB_NAME}"

    # instantiate db object
    sqlitedb = MySQLite(COCKPIT_DB)

    # check cockpit database settings
    if COCKPIT_DB_HOME == '' or COCKPIT_DB_HOME is None \
        or COCKPIT_DB_NAME == '' or COCKPIT_DB_NAME is None:
        pretty_print("@:: cockpit db settings".upper(), 'green', 'bright')
        pretty_print('\nERROR: cockpit.db is not set in configuration file. Aborting!', 'red')
        sys.exit(1)
    if not os.path.exists(COCKPIT_DB):
        db_set_required = True
        pretty_print("@:: cockpit db settings".upper(), 'green', 'bright')
        pretty_print('\nCockpit database was not found!', 'red')
        if get_user_ok("Would you like to create the cockpit database?"):
            try:
                subprocess.run(
                [f"{os.environ['COCKPIT_HOME']}/scripts/create_db.py"],
                stdout=subprocess.DEVNULL,
                stderr=subprocess.DEVNULL,
                check=True
                )
            except subprocess.SubprocessError as err:
                print(err)
            if not os.path.exists(COCKPIT_DB):
                sys.exit(1)
        else:
            pretty_print('\nERROR: a cockpit database is required in order to run. \
                Aborting!', 'red')
            sys.exit(1)

    # check cockpit enviroment settings
    for env_name in data['params']:
        if env_name != 'cockpit':
            pivot = data['params'][env_name]['endpoints']['pivot']
            if pivot == '' or pivot is None:
                env_set_required = True
                config_ok = False
                break
    if env_set_required:
        pretty_print('@:: cockpit environment settings'.upper(), 'green', 'bright')
        while not config_ok:
            pretty_print("ERROR: required parameters are not in configuration file!", 'red')
            if get_user_ok("\nWould you like cockpit to setup parameters automatically?"):
                for env_name in data['params']:
                    if env_name != 'cockpit':
                        script = f"{os.environ['COCKPIT_HOME']}/scripts/get_{env_name}_params.py"
                        subprocess.call([script], shell=True)
                # reload cockpit configuration after changes
                with open(config_yaml, 'r', encoding="utf-8") as yml:
                    data = yaml.safe_load(yml)
            else:
                print(f"\nplease set required parameters in: '{config_yaml}'\n")
                sys.exit()
            config_ok = True
            for env_name in data['params']:
                if env_name != 'cockpit':
                    pivot = data['params'][env_name]['endpoints']['pivot']
                    if pivot == '' or pivot is None:
                        config_ok = False
                        break
    if db_set_required or env_set_required:
        pretty_print("\nCockpit setup and verification completed successfully!", 'green')
        press_any_key()
        print_header()

    spinner = Spinner
    with spinner('Loading cockpit data... ', delay=0.1):
        types = get_object_types_from_space(data)
        for _type in types.values():
            the_type = _type[0]
            sql = f"SELECT name FROM types WHERE name = '{the_type}';"
            if len(sqlitedb.select(sql)) == 0:
                sqlitedb.insert("INSERT INTO types(name) VALUES(?);", (the_type,))
