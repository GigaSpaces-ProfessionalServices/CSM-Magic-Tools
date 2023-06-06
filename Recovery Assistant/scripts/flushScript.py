#!/usr/bin/python3
# *-* coding: utf-8 *-*

import subprocess
from setenvredolog import *

jCMD = f'java -jar "{str(jarFilePath)}" FlushRedoLogToDisk --spaceName={spaceName}'
subprocess.call(jCMD, shell=True)
subprocess.call('pause', shell=True)
