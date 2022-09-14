#!/usr/bin/python3

from curses.ascii import isalnum, isalpha, isdigit
import os
import sys
import argparse
import subprocess
import yaml
import ntpath
import shutil
import time
import readline
import glob
from signal import signal, SIGINT


def handler(signal_recieved, frame):
    '''
    catch CTRL+C keybaord press
    :param signal_recieved: caught by signal class
    :param frame:
    :return:
    '''
    print('\n\nOperation aborted by user!')
    exit(0)


def arg_parser():
	parser = argparse.ArgumentParser(
		description='description: remove specific packages from gigaspaces archive and repackage',
		epilog='run without options for interactive mode'
	)
	parser.add_argument('-f', action='store', dest='gs_archive', help='path to gigaspaces archive')
	parser.add_argument('-c', action='store', dest='client_name', help='the name of the client as appears in yaml file')
	parser.add_argument('-l', '--list', action='store_true', help='list client names from yaml file')

	args = {}
	ns = parser.parse_args()
	if ns.gs_archive:
		args['gs_archive'] = ns.gs_archive
	if ns.client_name:
		args['client_name'] = ns.client_name
	if ns.list:
		args['list'] = True
	return args


def check_file_exist(the_file):
	if os.path.exists(the_file):
		return True
	else:
		return False


def get_yaml_data(yaml_file):
	with open(yaml_file, 'r') as yf:
		return yaml.safe_load(yf)


def gs_archive_exist(gs_archive):
	if not os.path.exists(gs_archive):
		return False
	else:
		return True


def list_clients_from_yaml(the_yaml_data):
	for client in the_yaml_data['clients']:
		print(f"{the_yaml_data['clients'][client]['id']} - {client}")


def get_client_from_user(the_yaml_data):
	print('available clients:')
	list_clients_from_yaml(the_yaml_data)
	while True:
		ans = input("Pick a client: ")
		if ans.strip().isdigit():
			if int(ans) > 0 and int(ans) < len(the_yaml_data['clients']) + 1:
				for name in the_yaml_data['clients']:
					# print(the_yaml_data['clients'][name]['id'])
					# exit(0)
					if int(ans) == the_yaml_data['clients'][name]['id']:
						choice = name
						return choice


def check_client_name(the_yaml_data, the_client):
	for client in the_yaml_data['clients']:
		if client.lower() == the_client.lower():
			return client
	return ''


def get_file_path():
	readline.set_completer_delims('\t')
	readline.parse_and_bind("tab: complete")
	readline.set_completer(path_completer)
	while True:
		f = input("Enter path to gigaspaces file: ")
		if os.path.exists(f):
			break
		else:
			print(f"file not found. try again...")
	return f


def exec_repackager(the_package, the_client):
	if the_client == '':
		print(f"client name: '{client_name}' is not listed in yaml file.")
		print("use [-l] to list all available clients")
		exit(1)
	print(f"executing repackager operations...")
	# extract basedir and filename from path
	package_base_dir, package_base_name = ntpath.split(the_package)
	package_base_dir = os.path.realpath(package_base_dir)
	extracted_dir = ntpath.splitext(package_base_name)[0]
	print(f"   {'file:':<14}{package_base_dir}/{package_base_name}\n   {'client:':<14}{the_client}")	

	# extract archive
	print(f"   extracting gigaspaces archive...")
	sh_cmd = f"cd {package_base_dir} ; unzip -q {package_base_name}"
	response = subprocess.call(sh_cmd, shell=True)
	if response != 0:
		print(f"A problems occured while extracting {package_base_name}. aborting!")
		exit(1)
	
	# get list of all packages
	client_packages = yaml_data['clients'][the_client]['packages']
	
	# remove packages
	print("   deleting packages:")
	for pkg in client_packages:
		pkg_path = f"{package_base_dir}/{extracted_dir}/{pkg}"
		print(f"   .../{extracted_dir}/{pkg}")
		if os.path.exists(pkg_path):
			shutil.rmtree(pkg_path)
		time.sleep(0.2)
	
	# create a new archive: archive-name-patch-<char>-<digit>.zip
	new_dir_name = f"{extracted_dir}_patch-{patch_params['char']}-{patch_params['num']}"
	print(f"   renaming gigaspaces folder to: {package_base_dir}/{new_dir_name}")
	sh_cmd = f"mv {package_base_dir}/{extracted_dir} {package_base_dir}/{new_dir_name}"
	response = subprocess.call(sh_cmd, shell=True)
	print(f"   creating new archive: {new_dir_name}.zip")
	sh_cmd = f"cd {package_base_dir} ; zip -qr {new_dir_name}.zip {new_dir_name}"
	response = subprocess.call(sh_cmd, shell=True)
	print(f"   cleaning up extracted data...")
	if os.path.exists(f"{package_base_dir}/{new_dir_name}"):
		shutil.rmtree(f"{package_base_dir}/{new_dir_name}")


def path_completer(text, state):
    """ tabbed path autocomplete """
    line = readline.get_line_buffer().split()
    if '~' in text:
        text = os.path.expanduser('~')
    return [x for x in glob.glob(text+'*')][state]
	

def get_patch_id(type):
	while True:
		try:
			if type == 'character':
				a = input("Enter a character id for this package [e.g. 'a']: ").lower()
				if isalpha(a) and len(a) == 1:
					return a
			else:
				a = input("Enter a number id for this package [e.g. '2']: ")
				if isdigit(a) and len(a) == 1:
					return a
			if a == '':
				raise EOFError
			else:
				raise TypeError
		except TypeError:
			print(f"bad input/type. please enter a single {type}")
		except EOFError:
			print(f"input cannot be empty")

	

# global variables
manifest = 'manifest.yaml'

if __name__ == '__main__':
	signal(SIGINT, handler)
	arguments = arg_parser()
	# get yaml data
	manifest_full_path = f"{ntpath.split(sys.argv[0])[0]}/{manifest}"
	if check_file_exist(manifest_full_path):
		yaml_data = get_yaml_data(manifest_full_path)
	else:
		print('could not find yaml file')
		exit(1)
	if arguments:
		# get arguments
		if 'list' in arguments:
			list_clients_from_yaml(yaml_data)
			exit(0)
		if 'gs_archive' in arguments:
			gs_archive = arguments['gs_archive']
		else:
			print(f"missing required argument: GS_ARCHIVE. use [-h] for usage.")
			exit(1)
		if 'client_name' in arguments:
			client_name = arguments['client_name']
		else:
			print(f"missing required argument: CLIENT_NAME. use [-h] for usage.")
			exit(1)
		# parse arguments
		if check_file_exist(gs_archive):
			exec_repackager(gs_archive, check_client_name(yaml_data, client_name))
		else:
			print('gs archive file not found!')
			exit(1)
	else:
		patch_params = {}
		print()
		client_name = get_client_from_user(yaml_data)
		print()
		gs_archive = get_file_path()
		print()
		patch_params['char'] = get_patch_id('character')
		print()
		patch_params['num'] = get_patch_id('digit')
		print()
		exec_repackager(gs_archive, check_client_name(yaml_data, client_name))
exit(0)