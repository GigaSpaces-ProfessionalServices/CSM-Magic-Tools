# Display bllservice PU's that don't have both PRIMARY and BACKUP GSC's or that have both on same space server.
## Only checks for bllservice service and only if it is INTACT or SCHEDULED
## Reguired packages/scripts
### Following script can be downloaded from CSM-Magic-Tools
/dbagiga/utils/host-yaml.sh
### Following can be installed using "yum install jq"
jq

