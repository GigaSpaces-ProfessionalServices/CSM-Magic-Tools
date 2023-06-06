#!/usr/bin/python3
# *-* coding: utf-8 *-*

from setenvredolog import *
from time import sleep
import subprocess
import json
import pathlib
import socket

# load targets
with open(pathlib.PurePath.joinpath(scriptLocation, spaceHostsFileName), 'r', encoding='utf-8') as jhf:
    jdata = json.load(jhf)

exec = str(pathlib.PurePath.joinpath(resourceLocation, 'psexec.exe')).replace('\\','\\\\')
source_path = str(sourcePath).replace('\\','\\\\')
target_path = str(targetPath).replace('\\','\\\\')


for item in jdata:
    a_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)    
    port = 135
    server = item["host"]
    print(f"\nconnection to host '{server}': " ,end='', flush=True)
    a_socket.settimeout(5)
    check_port = a_socket.connect_ex((server, port))
    a_socket.settimeout(None)
    if check_port == 0:
        print("ESTABLISHED")
    else:
        print("FAILED")
        continue
    connection_params = f'{item["host"]} -u {item["username"]} -p {item["password"]}'
    create_target_cmd = f'"{exec}" -nobanner \\\\{connection_params} cmd /c "if not exist "{target_path}" (md "{target_path}")"'
    check_target_cmd = f'"{exec}" -nobanner \\\\{connection_params} cmd /c "cd "{target_path}""'
    copy_files_cmd = f'"{exec}" -nobanner \\\\{connection_params} cmd /c "xcopy "{source_path}" "{target_path}" /s /e /h /y"'
    
    # create target if not exist
    print(" - creating target folder... ", end='')
    subprocess.run(create_target_cmd, shell=True, stderr=subprocess.DEVNULL)
    count = 0
    opt_ok = False
    while count < 3:
        # wait until target is created
        if subprocess.run(check_target_cmd, shell=True, stderr=subprocess.DEVNULL).returncode == 0:
            opt_ok = True
            break
        sleep(1)
        count += 1
    if opt_ok:
        print("done")
    else:
        print("failed")
    # copy files from source
    print(" - copying files to target... ", end='')
    subprocess.call(copy_files_cmd, shell=True, stderr=subprocess.DEVNULL)
    print("done")
print()
subprocess.call('pause', shell=True)
