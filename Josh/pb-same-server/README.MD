# Display bllservice PU's that don't have both PRIMARY and BACKUP GSC's or that have both on same space server.
Only checks for bllservice service and only if it is INTACT/SCHEDULED/COMPROMISED  
ENV_CONFIG=/dbagigashare/env_config   (New odsx path) 
## Reguired packages/scripts
  -  Following script can be downloaded from CSM-Magic-Tools  
/dbagiga/utils/host-yaml.sh
  -  Following can be installed using "yum install jq"  
jq

## USAGE:

    PARAMETERS:
      -d | --debug    - Display each pair of containers in service (bllservice)
      --service       - Specify a service name
      --space         - Specify a space name

    DEFAULT VALUES:
      --service bllservice
      --space bllspace

    Example: pb-same-server --service dih-tau-service --space dih-tau-space

