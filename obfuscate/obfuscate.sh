#!/bin/bash

#
# obfuscate.sh will change strings/values in file(s) to allow sending the file(s) outside
# of a domain/network without exposing sensitive information
#
# * values are set in a separate dictionary like file named obfuscate.dict
#   by default the dictionary is located in the same directory as the script
#   this can be changed during script execution.
#
# By Alon Segal, Jan 2022
#

function usage() {

	echo
	echo "Usage: obfuscate.sh [Target] [Commands] [Options]"
	echo
	echo "Target:"
	echo -e "\tA path to a file or a folder"
	echo
	echo "Commands:"
	echo -e "   -d\t\tObfuscate for development environment"
	echo -e "   -s\t\tObfuscate for staging environment"
	echo -e "   -p\t\tObfuscate for production environment"
	echo -e "   -h, --help\tDisplay this help screen"
	echo
	echo "Options:"
	echo -e "   -z\t\tZip input file/folder after obfuscation"
	echo -e "   --dict=*\tPath to dictionary file"
	echo
}

function print_err() {
	# print styled error message
	
	local str="$1"
	echo  -ne "\033[5mERROR: \033[0m"
	echo "$str"
}

function print_runtime() {
	# get start + end value in seconds
	# and print runtime in human readable format
	
	local start=$1
	local end=$2
	local rt=$(($end-$start))
	if (($rt<60)); then
		echo "0h:0m:${rt}s" && return
	fi
	if ((60<=$rt<3600)); then
		echo "0h:$(($rt/60))m:$(($rt%60))s" && return
	fi
	if ((3600<$rt<86400)); then
		echo "$(($rt/3600))h:$((($rt%3600)/60))m:$(((($rt%3600)%60)))s"
		return
	fi
}

function ok_to_parse() {
	# parse input according to dictionary
	# return the array index if matched or '_NaN' if not
	
	local the_str="$1"
	local type=$2
	local parse_it=false
	for ((i=0 ; i<${#ENV_SOURCES[@]}; i++)); do
		[[ "x${ENV_SOURCES[$i]}x" == "xx" ]] && continue
		if [[ "$the_str" == *"${ENV_SOURCES[$i]}"* ]]; then		
			parse_it=true
			break
		fi
	done
	$parse_it && echo "$i" || echo "_NaN"
}

function obfuscate_name() {
	# get folder or file and rename it
	
	local type=$1	# type is file or directory
	local this_path="$2"
	local this_path_dirname=$(dirname "$this_path")
	local this_path_basename=$(basename "$this_path")
	local _i=$(ok_to_parse "$this_path_basename")
	case "$type" in
		-f)
			local this_file_ext=${this_path_basename##*.}
			local ext_flag=false
			if ! $(echo "$this_file_ext" | egrep -q "txt|log"); then
				this_file_ext="${this_path_basename##*.}.log"
				ext_flag=true
			fi
			if [[ "$_i" != "_NaN" ]] || $ext_flag ; then
				local this_file_name="$(echo ${this_path_basename%.*} | \
				sed "s#${ENV_SOURCES[$_i]}#${ENV_TARGETS[$_i]}#g")"
				if [[ ! -e "$this_path" ]]; then
					echo "${TS} ERROR file not found (${this_path})" >> $RT_LOG
					return
				else
					mv "$this_path" ${this_path_dirname}/${this_file_name}.${this_file_ext}
				fi
			fi
			;;
		-d)
			if [[ "$_i" != "_NaN" ]]; then
				local new_path_basename="$(echo $this_path_basename | \
				sed "s#${ENV_SOURCES[$_i]}#${ENV_TARGETS[$_i]}#g")"
				if [[ ! -d "$this_path" ]] ; then
					echo "${TS} ERROR directory not found (${this_path})" >> $RT_LOG
					return
				else
					mv "$this_path" "${this_path_dirname}/${new_path_basename}"
				fi
			fi
	esac
}

function obfuscate_text() {
	# get file path and obfuscate any required strings in it
	
	local the_path=$1
	for ((i=0 ; i<${#ENV_SOURCES[@]}; i++)); do
		[[ "x${ENV_SOURCES[$i]}x" == "xx" ]] && continue
		sed -i "s#${ENV_SOURCES[$i]}#${ENV_TARGETS[$i]}#g" $the_path
	done
}

#
### main ###
#

# show usage if invoked
case $1 in
	-h|--help) usage; exit 1 ;;
esac

# verify 1st perameter is an existing file or folder
if [[ -f $1 ]]; then
	is_dir=false
elif [[ -d $1 ]]; then
	is_dir=true
else
	echo ; print_err "unable to access target file or path not found"
	echo ; exit 1
fi
TPATH="$(echo "$1" | sed 's#/$##g')"	# the target file (without trailing slashs)
shift

# globals
_TS="$(date +"%Y-%m-%d %H:%M:%S")"
_R=$RANDOM
UTILS_DIR="/dbagiga/utils"
LOGS_DIR="${UTILS_DIR}/logs"
RT_LOG="${LOGS_DIR}/obfuscate_runtime_${_R}.log"
DICT_FILE="${UTILS_DIR}/obfuscate/obfuscate.dict"
TFD="$(dirname $TPATH)"			# the target file directory
LTFN="$(basename $TPATH)"		# the target file long name
STFN="${LTFN%.*}"				# the target file short name
TFEXT="${LTFN##*.}"				# the target file extension
start_time=$(date +"%s")
zip_when_done=false

while [[ $# -gt 0 ]]; do
	case $1 in
		-d) THE_ENV=DEV ; THE_ENV_NAME="Development" ; shift ;;
		-s) THE_ENV=STG ; THE_ENV_NAME="Staging" ; shift ;;
		-p) THE_ENV=PRD ; THE_ENV_NAME="Production" ; shift ;;
		-z) zip_when_done=true ; shift ;;
		--dict=*)
			DICT_FILE=$(echo $1 | cut -d= -f2 | sed 's# ##g')
			shift
			;;
		*)
			echo
			print_err "Invalid option." ; usage
			echo ; exit 1
	esac
done
# verify environment parameter is given
if [[ -z $THE_ENV ]]; then
	echo
	print_err "missing environment parameter"
	echo ; exit 1
fi
# verify dictionary file exists
if [[ ! -e $DICT_FILE ]]; then
	echo
	print_err "missing dictionary file. verify dictionary file location"
	echo ; exit 1
fi

echo -ne "\nBuilding obfuscation dictionary for $THE_ENV_NAME environment... "
# populate arrays of source and target strings from the dictionary
while read line; do
	# skip comment lines
	$(echo $line | grep -E "^#" > /dev/null 2>&1) && continue
	# skip empty lines
	$(echo $line | grep -E "^$" > /dev/null 2>&1) && continue
	target_env=$(echo $line | cut -d: -f1)
	[[ ${target_env^^} != ${THE_ENV} ]] && continue
	ENV_SOURCES+=( $(echo "$line" | cut -d: -f2) )
	ENV_TARGETS+=( $(echo "$line" | cut -d: -f3) )
done < $DICT_FILE
echo "done"
echo -e "\nObfuscating file names and content:"
# remove ip addresses and obfuscate strings in files
_files=( $(find $TPATH -type f) )
c=0 ; echo -n "   processed file: "
for _f in "${_files[@]}"; do
	((c++)) ; echo -ne "$c"
	# obfuscate all IP addresses (except 0.0.0.0) in the target file
	if [[ -e "$_f" ]]; then		# make sure the path to the file is valid
		sed -i -E 's#(([1-9][0-9]{1,2})\.([0-9]{1,3})\.([0-9]{1,3})\.([0-9]{1,3}))#\2.\3.X.X#g' "$_f"
	fi
	# change file contents if contains any sensitive date
	obfuscate_text "$_f"
	# change file name if contains any sensitive date
	obfuscate_name -f "$_f"
	bs=""; for ((j=0; j<${#c}; j++)); do bs="${bs}\b" ; done ; echo -ne "${bs}"
done
#echo "$c"
if $is_dir; then
	echo -e "\n\nObfuscating directory names ... "
	# change folder name if contains any sensitive date
	_dirs=( $(find $TPATH -type d) )
	c=0 ; echo -n "   processed folder: "
	for ((i=((${#_dirs[@]}-1)); i>=0; i--)); do
		((c++)) ; echo -ne "$c"
		obfuscate_name -d "${_dirs[$i]}";
		bs=""; for ((j=0; j<${#c}; j++)); do bs="${bs}\b" ; done ; echo -ne "${bs}"
	done
fi
echo "$c"

if $zip_when_done; then
	echo
	echo -n "Creating zip file for delivery... "
	zip -r -q "${STFN}.zip" "$TPATH"
	echo "done"
	echo " * New zip file created: $TFD/${STFN}.zip"
fi
echo

if [[ -e "$RT_LOG" ]]; then
	echo -e "\nThe following problems were found:"
	cat $RT_LOG ; rm -f $RT_LOG
fi

end_time=$(date +"%s")
echo -ne "operations completed in:   " ; print_runtime $start_time $end_time

exit 0
