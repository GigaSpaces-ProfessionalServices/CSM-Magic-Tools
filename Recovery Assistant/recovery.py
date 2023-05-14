#!/usr/bin/python3
# *-* coding: utf-8 *-*

"""
runme: main script
"""

if __name__ == '__main__':
    
    import os
    import subprocess
    import yaml

    # import custom modules
    from modules import (
        print_locations,
        print_header,
        pretty_print,
        press_any_key,
        validate_navigation_select
    )

    BASE_DIR = os.path.dirname(os.path.abspath(__file__))
    MENU_YAML = f"{BASE_DIR}/config/menu.yaml"
    CONFIG_YAML = f"{BASE_DIR}/config/config.yaml"
    user_selections = []
    
    # load menu yaml
    with open(MENU_YAML, 'r', encoding="utf-8") as yml:
        data = yaml.safe_load(yml)
    try:
        print_header()
        while True:
            if len(user_selections) != 0:
                # building dynamic dictionary according to menu choices
                _dict = eval("data[" + ']['.join(user_selections) + "]",
                {"user_selections": user_selections, "data" : data})
            else:
                _dict = data
            print_locations(user_selections, data)
            if _dict['type'] == 'exec':
                # checking that target key is set
                if _dict['target'] == '':
                    pretty_print(f"YAML ERROR: missing 'target' value "
                    f"in command '{_dict['id']}'", 'red')
                    press_any_key()
                    user_selections.pop()
                    continue
                script = f"'{BASE_DIR}/scripts/{_dict['target']}'"
                subprocess.call([script], shell=True)
                user_selections.pop()
                continue
            validate_navigation_select(_dict, user_selections)
    except (KeyboardInterrupt, SystemExit):
        os.system("stty sane ; stty erase ^H ; stty erase ^?")
        print('\nAborted!')
