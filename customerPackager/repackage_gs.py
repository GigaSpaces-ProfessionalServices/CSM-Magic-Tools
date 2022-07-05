#!/usr/bin/python3

import os
import sys
import argparse
import subprocess
import yaml
import ntpath
import shutil
import time

def arg_parser():
	parser = argparse.ArgumentParser(
		description='description: remove specific packages from gigaspaces archive and repackage',
		# epilog='based on yaml file'
	)
	required = parser.add_argument_group('required arguments')
	required.add_argument('-f', action='store', dest='gs_archive', help='path to gigaspaces archive')
	required.add_argument('-c', action='store', dest='client_name', help='the name of the client as appears in yaml file')
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
	print('clients listed in yaml file:')
	for client in the_yaml_data['clients']:
		print(f"- {client}")
	exit(0)

def get_client_name(the_yaml_data, the_client):
	for client in the_yaml_data['clients']:
		if client.lower() == the_client.lower():
			return client
	return ''

def exec_repackager(the_package, the_client):
	if the_client == '':
		print(f"client name: '{client_name}' is not listed in yaml file.")
		print("use [-l] to list all available clients")
		exit(0)
	print(f"executing repackager operations...")
	print(f"{'on file:':<14}{the_package}\n{'for client:':<14}{the_client}")	
	# extract basedir and filename from path
	dir_name, base_name = ntpath.split(the_package)
	extracted_dir = ntpath.splitext(base_name)[0]
	# extract archive
	print(f"extracting gigaspaces archive...")
	sh_cmd = f"cd {dir_name} ; unzip -q {the_package}"
	response = subprocess.call(sh_cmd, shell=True)
	if response != 0:
		print(f"A problems occured while extracting {the_package}. aborting!")
		exit(1)
	# get list of all packages
	client_packages = yaml_data['clients'][the_client]['packages']
	# remove packages
	print("deleting packages according to manifest:")
	for pkg in client_packages:
		pkg_path = f"{dir_name}/{extracted_dir}/{pkg}"
		print(f" {dir_name}/{extracted_dir}/{pkg}")
		if os.path.exists(pkg_path):
			shutil.rmtree(pkg_path)
		time.sleep(0.2)
	# create a new archive with client name: archive-name_<client-name>.zip
	new_dir = f"{extracted_dir}_{the_client}"
	print(f"renaming gigaspaces folder to: {dir_name}/{new_dir}")
	sh_cmd = f"mv {dir_name}/{extracted_dir} {dir_name}/{new_dir}"
	response = subprocess.call(sh_cmd, shell=True)
	print(f"creating new archive: {new_dir}.zip")
	sh_cmd = f"cd {dir_name} ; zip -qr {new_dir}.zip {new_dir}"
	response = subprocess.call(sh_cmd, shell=True)
	print(f"cleaning up extracted data...")
	if os.path.exists(f"{dir_name}/{new_dir}"):
		shutil.rmtree(f"{dir_name}/{new_dir}")


# global variables
manifest = 'manifest.yaml'


if __name__ == '__main__':
	arguments = arg_parser()
	if arguments:
		# get yaml data
		manifest_full_path = f"{ntpath.split(sys.argv[0])[0]}/{manifest}"
		if check_file_exist(manifest_full_path):
			yaml_data = get_yaml_data(manifest_full_path)
		else:
			print('could not find yaml file')
			exit(1)
		# get arguments
		if 'list' in arguments:
			list_clients_from_yaml(yaml_data)
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
			exec_repackager(gs_archive, get_client_name(yaml_data, client_name))
		else:
			print('gs archive file not found!')
			exit(1)
	else:
		print(f"missing required arguments. use [-h] for usage.")
		exit(1)


exit(0)