#!/usr/bin/python3
# *-* coding: utf-8 *-*

# def list_tasks():
#     print("listing tasks ...")
#     input("press ENTER to continue")


# def show_task():
#     list_tasks()
#     print("showing selected task ...")
#     input("press ENTER to continue")


def validator(pivot):
    import os, subprocess, json
    from colorama import Fore

    def validate_input(menu_items):
        choice = input("\nEnter your choice: ")
        while True:
            if choice == '99':
                return -1
            if len(menu_items) > 1:
                if choice == str(len(menu_items) + 1): # we check if 'ALL' is selected
                    total = 0
                    for item in menu_items.values(): total += item[1]
                    print(f"ALL objects have a total of {total} entries")
                    return total
            if not choice.isdigit() or int(choice) not in menu_items.keys():
                choice = input(f"{Fore.RED}ERROR: Input must be a menu index!{Fore.RESET}\nEnter you choice: ")
            else:
                print(f"\nobject '{menu_items[int(choice)][0]}' has {menu_items[int(choice)][1]} entries.")
                return menu_items[int(choice)][1]

    exec_script = f"{os.getcwd()}/scripts/get_obj_count.py"
    cmd = f"cat {exec_script} | ssh {pivot} python3 -"
    response = str(subprocess.run([cmd], shell=True, stdout=subprocess.PIPE).stdout).strip('b"').split('\\n')    

    # build objects dictionary from response
    index, space_objects = 1, {}
    for i in response:
        if i != '':
            # converting string to dictionary
            d = json.loads(i.replace("\'", "\""))
            for o, e in d.items():
                obj = [o, e['entries']]
                break
            item = {index : obj}
            space_objects.update(item)                
            index += 1
    # print menu
    q = "Which type would you like to validate?"
    print(q + "\n" + '=' * len(q))
    for k, v in space_objects.items():
        index = f"[{k}]"
        print(f'{index:<4} - {v[0]:<24}')
    if len(space_objects) > 1:
        index = f"[{k+1}]"
        print(f'{index:<4} - {"All Object Types":<24}')
    print(f'{"[99]":<4} - {"ESC":<24}')
    result = validate_input(space_objects)
    if result != -1:
        input("\n\npress ENTER to continue")