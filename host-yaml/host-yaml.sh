#!/bin/bash
[[ $# -ne 1 ]] && { echo -e "\nGive one parameter which should be a name of a role.\n" ; exit 1 ; }

role_found=0
while read line ; do
[[ $(echo "${line}" | grep "^ *${1} *:" >/dev/null 2>&1 ; echo $? ) -eq 0 ]] && { role_found=1 ; continue ; }
if [[ $role_found -ne 0 && $(echo "${line}" | grep -w '^ *host[0-9]* *:' >/dev/null 2>&1 ; echo $? ) -eq 0 ]] ; then
  echo $line | sed 's/^ *host[0-9]* *: *//'
else
  role_found=0
fi
done < <(cat ${ENV_CONFIG}/host.yaml | sed '/^ *$/d')
