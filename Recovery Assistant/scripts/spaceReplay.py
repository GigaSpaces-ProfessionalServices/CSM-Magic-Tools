#!/usr/bin/python3
# *-* coding: utf-8 *-*

from setenvredolog import *
import subprocess
import pathlib

exec = str(pathlib.PurePath.joinpath(resourceLocation, 'ReadRedoLogContents.exe')).replace('\\','\\\\')
cmd_params = '--spaceName={} --lookupLocators={} --lookupGroups={} --redoLogYaml="{}" --assemblyFileName="{}"'.format(
    spaceName, 
    lookupLocators, 
    lookupGroups, 
    redoLogYaml, assemblyFileName
    )
cmd = f'"{exec}" {cmd_params}'

subprocess.run(cmd, shell=True)

print()
subprocess.call('pause', shell=True)
