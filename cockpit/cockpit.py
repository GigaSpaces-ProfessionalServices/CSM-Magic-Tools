#!/usr/bin/python3
# *-* coding: utf-8 *-*

"""
cockpit.py: main script
"""

if __name__ == '__main__':
    import os
    import subprocess
    import yaml
    from colorama import Fore, Style
    
    # set required environment variables
    THIS_PATH = os.path.realpath(os.path.dirname(__file__))
    os.environ['COCKPIT_HOME'] = THIS_PATH
    if os.environ.get('PYTHONPATH') is not None:
        # add current path
        os.environ['PYTHONPATH'] = ':'.join([os.environ['PYTHONPATH'], THIS_PATH])
        # remove duplicates if exist
        os.environ['PYTHONPATH'] = ':'.join(set(os.environ['PYTHONPATH'].split(':')))
    else:
        os.environ['PYTHONPATH'] = THIS_PATH
    os.environ['PIVOT_PRD'] = '1.1.1.1'
    os.environ['PIVOT_DR'] = '2.2.2.2'

    # import custom modules
    from modules.cp_print import (
        print_locations,
        print_header,
        pretty_print
    )
    from modules.cp_inputs import (
        press_any_key,
        validate_navigation_select
    )
    from modules.cp_init import check_settings

    MENU_YAML = f"{os.environ['COCKPIT_HOME']}/config/menu.yaml"
    CONFIG_YAML = f"{os.environ['COCKPIT_HOME']}/config/config.yaml"
    user_selections = []

    # load menu yaml
    with open(MENU_YAML, 'r', encoding="utf-8") as yml:
        data = yaml.safe_load(yml)
    try:        
        print_header()
        check_settings(CONFIG_YAML)

        while True:
            if len(user_selections) != 0:
                # reconstructing dict according to menu choices
                _dict = eval("data[" + ']['.join(user_selections) + "]",
                {"user_selections": user_selections, "data" : data})
            else:
                _dict = data
            print_locations(user_selections, data)
            if _dict['type'] == 'command':
                # checking that exec-type key is set
                if _dict['exec-type'] == '':
                    pretty_print(f"YAML ERROR: missing 'exec-type' value in command \
                        '{_dict['id']}'", 'red')
                    press_any_key()
                    user_selections.pop()
                    continue
                # checking that exec key is set
                if _dict['exec'] == '':
                    pretty_print(f"YAML ERROR: missing 'exec' value in command \
                        '{_dict['id']}'", 'red')
                    press_any_key()
                    user_selections.pop()
                    continue
                if _dict['exec-type'] == 'module':
                    eval(f"{_dict['exec']}()")
                if _dict['exec-type'] == 'script':
                    script = f"{os.environ['COCKPIT_HOME']}/scripts/{_dict['exec']}"
                    subprocess.call([script], shell=True)
                user_selections.pop()
                continue
            validate_navigation_select(_dict, user_selections)
    except (KeyboardInterrupt, SystemExit):
        os.system('stty sane')
        print('\nAborted!')
