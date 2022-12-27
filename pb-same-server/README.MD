# Display bllservice PU's that don't have both PRIMARY and BACKUP GSC's or that have both on same space server.
Only checks for bllservice service and only if it is INTACT/SCHEDULED/COMPROMISED  
ENV_CONFIG=/dbagigashare/env_config   (New odsx path) 
## Reguired packages/scripts
  -  Following script can be downloaded from CSM-Magic-Tools  
/dbagiga/utils/host-yaml.sh
  -  Following can be installed using "yum install jq"  
jq

## USAGE:

  Check if P/B pair of containers are on the same space server.
    Usage: /usr/local/bin/pb-same-server

    PARAMETERS:
      -d | --debug - Display each pair of containers in service (bllservice)
      -s | --service - Specify a service name --- TBD ---

    DEFAULT VALUES:
    -s     - bllservice
