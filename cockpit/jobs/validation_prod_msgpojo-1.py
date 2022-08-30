#!/usr/bin/python3


import subprocess

exec_script = "/home/alon/documents/myprojects/csm-magic-tools/cockpit/scripts/get_obj_count_prod.py"
cmd = f"cat {exec_script} | ssh ${PIVOT_PRD} python3 -"
response = str(subprocess.run([cmd], shell=True, stdout=subprocess.PIPE).stdout).strip('b"').split('\n')
print(response)

