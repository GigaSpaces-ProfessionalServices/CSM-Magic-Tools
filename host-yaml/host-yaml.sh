#!/bin/bash
role_found=0
[[ $# -eq 0 ]] && { echo "No parameter given" ; exit 1 ; }

while read line ; do
[[ $(echo "${line}" | grep "^ *${1} *:" >/dev/null 2>&1 ; echo $? ) -eq 0 ]] && { role_found=1 ; continue ; }
if [[ $role_found -ne 0 && $(echo "${line}" | grep -w '^ *host[0-9]* *:' >/dev/null 2>&1 ; echo $? ) -eq 0 ]] ; then
  echo $line | sed 's/^ *host[0-9]* *: *//'
else
  role_found=0
fi
done < <(cat $ODSXARTIFACTS/odsx/host.yaml | sed '/^ *$/d')
