#!/bin/bash

#
# By Alon Segal, Jan 2022
#
# obfuscate.sh will change strings in/of files and folders regarded as 
# sensitive data to allow exporting outside of the organisation
# while eliminating exposure of sensitive information.
#
# values regarded as sensitive data are listed in a obfuscate.dict file
# located in the same directory as the script
# values must comply to the following format: ENV_NAME:LOOKUP_STRING:REPLACEMENT_STRING
#


function usage() {
    echo
    echo "Usage: $(basename $0) <Target File/Dir> [Options]"
    echo
    echo "Options:"
    echo -e "   -g\t\tObfuscate for Garage environment"
    echo -e "   -d\t\tObfuscate for Development environment"
    echo -e "   -s\t\tObfuscate for Staging environment"
    echo -e "   -p\t\tObfuscate for Production environment"
    echo -e "   -dr\t\tObfuscate for DR environment"
    echo -e "   -c, --code\tCode obfuscation: no changes - test and report only"
    echo -e "   -q, --quiet\tDo not print warnings/Info on screen"
    echo -e "   -z, --zip\tZip input file/folder after obfuscation"
    echo -e "   --dict=*\tPath to dictionary file"
    echo -e "   -h, --help\tDisplay this help screen"
    echo
}

function print_err() {
    # print styled error message

    local str="$1"
    echo  -ne "\033[5mERROR: \033[0m"
    echo "$str"
}

function print_run_time() {
    # get start + end value in seconds
    # and print runtime in human readable format

    local start=$1
    local end=$2
    local rt=$(($end-$start))
    if (($rt<60)); then
        echo "0h:0m:${rt}s"
        return
    fi
    if ((60<=$rt<3600)); then
        echo "0h:$(($rt/60))m:$(($rt%60))s"
        return
    fi
    if ((3600<$rt<86400)); then
        echo "$(($rt/3600))h:$((($rt%3600)/60))m: $(($(($rt%3600))%60))s"
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

    local type=$1 # type is file or directory
    local this_path="$2"
    local this_path_dirname="$(dirname $this_path)"
    local this_path_basename="$(basename $this_path)"
    local _i=$(ok_to_parse "$this_path_basename")
    case "$type" in
        -f)
            echo "obfuscate_name $this_path" >> /tmp/debug
            local this_file_ext="${this_path_basename##*.}"
            # if file has no extension we set '.ext'
            [[ "$this_file_ext" == "$this_path_basename" ]] && this_file_ext="ext"
            local ext_flag=false
            if ! $(echo "$this_file_ext" | egrep -q "txt|log"); then
                this_file_ext="log"
                ext_flag=true
            fi
            if [[ "$_i" != "_NaN" ]] || $ext_flag ; then
                local this_file_name="$(echo "${this_path_basename%.*}" | \
                sed "s#${ENV_SOURCES[$_i]}#${ENV_TARGETS[$_i]}#g")"
                if $is_code; then
                    if [[ $(echo "${this_path_basename%.*}" | grep -q "${ENV_SOURCES[$_i]}" ; echo $?) -eq 0 ]]; then
                        $WRITE_TO_LOG && echo "$(date +"%Y-%m-%d %H:%M:%S") session $_R (module: obfuscate_name file) found violation: '${ENV_SOURCES[$_i]}', in: '${this_path}'" >> $LOG_FILE
                    fi
                fi
                if [[ ! -e "$this_path" ]]; then
                    $WRITE_TO_LOG && echo "$(date +"%Y-%m-%d %H:%M:%S") session $_R file not found (${this_path})" >> $LOG_FILE
                    return
                else
                    if ! $is_code; then
                        mv "$this_path" "${this_path_dirname}/${this_file_name}.${this_file_ext}"
                        [[ "$this_path" == "$TPATH" ]] && TPATH="${this_path_dirname}/${this_file_name}.${this_file_ext}"
                    fi
                fi
            fi
            ;;
        -d)
            if [[ "$_i" != "_NaN" ]]; then
                local new_path_basename="$(echo "$this_path_basename" | \
                sed "s#${ENV_SOURCES[$_i]}#${ENV_TARGETS[$_i]}#g")"
                if [[ ! -d "$this_path" ]] ; then
                    $WRITE_TO_LOG && echo "$(date +"%Y-%m-%d %H:%M:%S") session $_R directory not found (${this_path})" >> $LOG_FILE
                    return
                else
                    if $is_code; then
                        $WRITE_TO_LOG && echo "$(date +"%Y-%m-%d %H:%M:%S") session $_R (module: obfuscate_name folder) found violation: directory '$this_path'" >> $LOG_FILE
                    else
                        mv "$this_path" "${this_path_dirname}/${new_path_basename}"
                        [[ "$this_path" == "$TPATH" ]] && TPATH="${this_path_dirname}/${new_path_basename}"
                    fi
                fi
            fi
    esac
}

function obfuscate_text() {
    # get file path and obfuscate any required strings in it

    local the_path="$1"
    echo "obfuscate_text $the_path" >> /tmp/debug
    for ((i=0 ; i<${#ENV_SOURCES[@]}; i++)); do
        [[ "x${ENV_SOURCES[$i]}x" == "xx" ]] && continue
        if $is_code; then
            if [[ $(grep -q "${ENV_SOURCES[$i]}" "$the_path" ;echo $?) -eq 0 ]]; then
                ln=$(grep -n "${ENV_SOURCES[$i]}" "$the_path" | cut -d: -f1)
                $WRITE_TO_LOG && echo "$(date +"%Y-%m-%d %H:%M:%S") session $_R (module: obfuscate_text) found violation: '${ENV_SOURCES[$i]}', in: '${the_path}' (line ${ln})" >> $LOG_FILE
            fi
        else
            sed -i "s#${ENV_SOURCES[$i]}#${ENV_TARGETS[$i]}#g" "$the_path"
        fi
    done
}

#
### main ###
#
# globals
WRITE_TO_LOG=true
_R=$RANDOM
UTILS_DIR="/dbagiga/utils"
LOGS_DIR="/dbagigalogs/obfuscate"
LOG_FILE="${LOGS_DIR}/obfuscate.log"
DICT_FILE="${UTILS_DIR}/obfuscate/obfuscate.dict"
STFN="${LTFN%.*}"       # the target file short name
TFEXT="${LTFN##*.}"       # the target file extension
start_time=$(date +"%s")
zip_when_done=false
is_code=false
silent=false
ENVS=( "GRG" "DEV" "STG" "PRD" "DR")

if [[ $# -eq 0 ]]; then
    echo ; print_err "missing required target path" ; echo
    usage
    exit 1
fi
while [[ $# -gt 0 ]]; do
    case $1 in
        -g) THE_ENV=GRG 
            THE_ENV_NAME="Garage"
            shift ;;
        -d) THE_ENV=DEV
            THE_ENV_NAME="Development"
            shift ;;
        -s) THE_ENV=STG
            THE_ENV_NAME="Staging"
            shift ;;
        -p) THE_ENV=PRD
            THE_ENV_NAME="Production"
            shift ;;
        -dr) THE_ENV=DR
             THE_ENV_NAME="DR"
             shift ;;
        -c| --code)
            is_code=true
            shift ;;
        -q| --quiet)
            silent=true
            shift ;;
        -z|--zip)
            zip_when_done=true
            shift ;;
        --dict=*)
            DICT_FILE=$(echo $1 | cut -d= -f2 | sed 's# ##g')
            shift
            ;;
        -h|--help)
            usage
            exit 1 ;;
        *)
            if [[ -f $1 ]]; then
                is_dir=false
            elif [[ -d $1 ]]; then
                is_dir=true
            else
                echo ; print_err "path not found or invalid parameter"
                echo ; exit 1
            fi
            TPATH="$(echo "$1" | sed 's#/$##g')"  # the target file (without trailing slashs)
            if [[ ! -z $TPATH ]]; then
                TFD="$(dirname $TPATH)"     # the target file directory
                LTFN="$(basename $TPATH)"   # the target file long name
                shift
            else
                echo
                print_err "Invalid option." ; usage
                echo ; exit 1
            fi
    esac
done
# if no env is chosen will parse all envs (=default)
[[ -z $THE_ENV ]] && THE_ENV=${ENVS[@]}

# verify dictionary file exists
if [[ ! -e $DICT_FILE ]]; then
    echo
    print_err "missing dictionary file. verify dictionary file location"
    echo ; exit 1
fi

# register init in log
[[ ! -d $LOGS_DIR ]] && mkdir $LOGS_DIR
[[ ! -f $LOG_FILE ]] && touch $LOG_FILE
$is_code && TYPE="code obfuscation" || TYPE="log obfuscation"
warning='\033[5m!!! WARNING !!! \033[0m'
hframe=$(printf -- '= %.0s' {1..15})
fframe=$(printf -- '= %.0s' {1..39})
if $is_code && ! $silent; then
    echo -e "\n!!! NOTE !!!"
    echo -e " Obfuscation with option '-c' will only report violations."
    echo -e " No changes to files / folders names or data will be made"
    echo -e " Details are printed to '$LOG_FILE'\n"
    #echo -e "$fframe"
elif ! $is_code ; then
    echo -e "\n$hframe $warning $hframe"
    echo -e "=$(printf -- ' %.0s' {1..75})="
    echo -e "=  If you are scanning code it is highly recommended you use option '-c'    ="
    echo -e "=  otherwise the obfuscation might change files / folders names and data    ="
    echo -e "=  if any violations are found.                                             ="
    echo -e "=$(printf -- ' %.0s' {1..75})="
    echo -e "=  OBFUSCATION FOR CODE ONLY REPORTS VIOLATIONS AND DOES NOT CHANGE VALUES! ="
    echo -e "=  Details are printed to '$LOG_FILE'            ="
    echo -e "=$(printf -- ' %.0s' {1..75})="
    echo -e "$fframe"
fi
while true; do
    read -ep "Continue? [yes/no] : " user_choice
    user_choice=$(echo $user_choice | tr '[:upper:]' '[:lower:]')
    case ${user_choice} in
        'yes')
            $WRITE_TO_LOG && echo "$(date +"%Y-%m-%d %H:%M:%S") session $_R started (type: $TYPE)" >> $LOG_FILE
            break ;;
        'no')
            exit ;;
        *)
            echo "Type 'yes' or 'no'" ; continue
    esac
done

# build a list of parameters for selected environment
echo -ne "\nBuilding obfuscation dictionary... "
for the_env in ${THE_ENV[@]}; do
    while read line; do
        $(echo "$line" | grep -E "^#" > /dev/null 2>&1) && continue    
        $(echo "$line" | grep -E "^$" > /dev/null 2>&1) && continue
        target_env=$(echo "$line" | cut -d: -f1)
        if [[ ${#THE_ENV[@]} -eq 1 ]]; then
            [[ ${target_env^^} != ${the_env} ]] && continue
        fi
        ENV_SOURCES+=( $(echo "$line" | cut -d: -f2) )
        ENV_TARGETS+=( $(echo "$line" | cut -d: -f3) )
    done < $DICT_FILE
    if [[ $ENV_SOURCES == "" ]] || [[ $$ENV_TARGETS == "" ]]; then
        echo ; print_err "error populating dictionary values. please check your dictionary file."
        echo ; exit 1
    fi
done
echo "done"
$WRITE_TO_LOG && echo "$(date +"%Y-%m-%d %H:%M:%S") session $_R built obfuscation dictionary for environments: '${THE_ENV[@]}'" >> $LOG_FILE
$WRITE_TO_LOG && echo "$(date +"%Y-%m-%d %H:%M:%S") session $_R processing files and directories for path: '$(realpath $TPATH)'" >> $LOG_FILE
echo -e "\nObfuscating files... "
# remove ip addresses and obfuscate strings in files
_files=() ; while IFS= read -r -d $'\0'; do _files+=("$REPLY"); done < <(find "$TPATH" -type f -print0)
c=0 ; echo -n "   processed file: "
for _f in "${_files[@]}"; do
  ((c++)) ; echo -ne "$c"
  # obfuscate all IP addresses (except 0.0.0.0) in the target file
  if [[ -e "$_f" ]]; then   # make sure the path to the file is valid
    sed -i -E 's#(([1-9][0-9]{1,2})\.([0-9]{1,3})\.([0-9]{1,3})\.([0-9]{1,3}))#\2.\3.X.X#g' "$_f"
  fi
  # change file contents if contains any sensitive date
  obfuscate_text "$_f"
  # change file name if contains any sensitive date
  obfuscate_name -f "$_f"
  bs=""; for ((j=0; j<${#c}; j++)); do bs="${bs}\b" ; done ; echo -ne "${bs}"
done
$WRITE_TO_LOG && echo "$(date +"%Y-%m-%d %H:%M:%S") session $_R processed $c files" >> $LOG_FILE

if $is_dir; then
    echo -e "\n\nObfuscating directories... "
  # change folder name if contains any sensitive date
  _dirs=() ; while IFS= read -r -d $'\0'; do _dirs+=("$REPLY"); done < <(find "$TPATH" -type d -print0)
  c=0 ; echo -n "   processed folder: "
  for ((i=((${#_dirs[@]}-1)); i>=0; i--)); do
    ((c++)) ; echo -ne "$c"
    obfuscate_name -d "${_dirs[$i]}";
    bs=""; for ((j=0; j<${#c}; j++)); do bs="${bs}\b" ; done ; echo -ne "${bs}"
  done
fi
echo "$c"
$WRITE_TO_LOG && echo "$(date +"%Y-%m-%d %H:%M:%S") session $_R processed $c folders" >> $LOG_FILE

if $zip_when_done; then
    LTFN="$(basename $TPATH)"
    STFN="${LTFN%.*}"
    echo
    echo -n "Creating zip file for delivery... "
    zip -r -q "${STFN}.zip" "$TPATH"
    echo "done"
    echo "  zip file: $TFD/${STFN}.zip"
    $WRITE_TO_LOG && echo "$(date +"%Y-%m-%d %H:%M:%S") session $_R created zip archive as '$TFD/${STFN}.zip'" >> $LOG_FILE
fi

echo
end_time=$(date +"%s")
echo -ne "operations completed in:   " ; print_run_time $start_time $end_time
# report if no violations found
if [[ $(grep "found violation" $LOG_FILE | grep "session $_R" | wc -l) -eq 0 ]]; then
    $WRITE_TO_LOG && echo "$(date +"%Y-%m-%d %H:%M:%S") session $_R no violations found!" >> $LOG_FILE
fi
$WRITE_TO_LOG && echo "$(date +"%Y-%m-%d %H:%M:%S") session $_R completed" >> $LOG_FILE
if $is_code; then
    echo -e "\033[5m>>> \033[0mreview obfuscation log file with session $_R for details\033[5m <<<\033[0m"
    echo 
fi
exit 0
