#!/usr/bin/python3
# *-* coding: utf-8 *-*

"""
recovery.py: main script
"""

if __name__ == '__main__':
    
    import subprocess
    import re
    
    def module_exist(_module_name):
        r = subprocess.run(
            "pip list".split(), 
            stdout=subprocess.PIPE).stdout.decode().lower()
        if re.search(_module_name.lower(), r):
            return True
        return False
    
    modules = ['pyyaml','colorama','pyfiglet']
    for module in modules:
        if not module_exist(module):
            subprocess.run([f'pip install {module}'], shell=True)

    from platform import system
    import os
    import yaml

    # import custom modules
    from modules import (
        print_locations,
        pretty_print,
        press_any_key,
        validate_navigation_select
    )

    BASE_DIR = os.path.dirname(os.path.abspath(__file__))
    MENU_YAML = f"{BASE_DIR}/config/menu.yaml"
    CONFIG_YAML = f"{BASE_DIR}/config/config.yaml"
    user_selections = []
    

    def exec_target_ok(_target):
        """
        check target script
        :param target: a dictionary of available choices
        :return: True/False
        """
        # checking that target key is set
        if _target == '':
            _err_msg = f"CONFIG ERROR: missing 'target' value for '{_dict['id']}' option"
            pretty_print(_err_msg, 'red')
            return False
        # checking that target script exists
        _script = f"{BASE_DIR}/scripts/{_target}"
        if not os.path.exists(_script):
            _err_msg = f"EXEC ERROR: target 'scripts/{os.path.basename(_script)}' not found"
            pretty_print(_err_msg, 'red')
            return False
        return True


    # load menu yaml
    with open(MENU_YAML, 'r', encoding="utf-8") as yml:
        data = yaml.safe_load(yml)
    try:
        #print_header()
        while True:
            if len(user_selections) != 0:
                # building dynamic dictionary according to menu choices
                _dict = eval("data[" + ']['.join(user_selections) + "]",
                {"user_selections": user_selections, "data" : data})
            else:
                _dict = data
            print_locations(user_selections, data)
            if _dict['type'] == 'exec':
                # check target script
                if not exec_target_ok(_dict['target']):
                    press_any_key()
                    user_selections.pop()
                    continue
                # execute target script
                script = f"{BASE_DIR}/scripts/{_dict['target']}"
                subprocess.call([script], shell=True)
                user_selections.pop()
                continue
            validate_navigation_select(_dict, user_selections)
    except (KeyboardInterrupt, SystemExit):
        if system() == 'Linux':
            os.system("stty sane ; stty erase ^H ; stty erase ^?")
        print('\nAborted!')
