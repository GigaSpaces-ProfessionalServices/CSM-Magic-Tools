#!/bin/bash
# updated on STAGE 2024-06-05

source ~/.bashrc
_LOG=/var/log/auto_odsx.log
_FULL_LOG=/dbagigalogs/auto_odsx_full.log
_WAIT=120

usage() {
  cat << EOF

  USAGE: $0 [<option>]

  OPTIONS:

  1       auto_spacereinstall-inmemory
  2       auto_spacereinstall-ts
  3       auto_spacereinstall-inmemory-gilboa

EOF
}

case $1 in 
  "1") 
    sed -i 's/app.dataengine.mssql-feeder.writeBatchSize=.*/app.dataengine.mssql-feeder.writeBatchSize=10000/' ${ENV_CONFIG}/app.config
    sed -i 's/app.dataengine.oracle-feeder.writeBatchSize=.*/app.dataengine.oracle-feeder.writeBatchSize=13000/' ${ENV_CONFIG}/app.config
    auto_spacereinstall-inmemory
    [[ $? -ne 0 ]] && { echo -e "\nFailed ODSX/CDC deployment.\n" ; exit 1 ; }
    ;;
  "2") 
    sed -i 's/app.dataengine.mssql-feeder.writeBatchSize=.*/app.dataengine.mssql-feeder.writeBatchSize=2000/' ${ENV_CONFIG}/app.config
    sed -i 's/app.dataengine.oracle-feeder.writeBatchSize=.*/app.dataengine.oracle-feeder.writeBatchSize=2000/' ${ENV_CONFIG}/app.config
    auto_spacereinstall-ts
    [[ $? -ne 0 ]] && { echo -e "\nFailed ODSX/CDC deployment.\n" ; exit 1 ; }
    ;;
  "3") 
    sed -i 's/app.dataengine.mssql-feeder.writeBatchSize=.*/app.dataengine.mssql-feeder.writeBatchSize=10000/' ${ENV_CONFIG}/app.config
    sed -i 's/app.dataengine.oracle-feeder.writeBatchSize=.*/app.dataengine.oracle-feeder.writeBatchSize=13000/' ${ENV_CONFIG}/app.config
    auto_spacereinstall-inmemory-gilboa
    [[ $? -ne 0 ]] && { echo -e "\nFailed ODSX/CDC deployment.\n" ; exit 1 ; }
    ;;
  *) usage ; exit 1 ;;
esac

#/giga/yuval/TAU/run-only-services.sh
echo -e "\nDeploying services with gcom:\n"
/giga/yuval/TAU/gcom_deploy_services.sh
echo -e "\nPerforming regular query for all services in /giga/microservices/curls:\n"
for i in {1..6} ; do check-env.sh -c 6 ; done | grep -v '^ *$'
echo -e "\nPerforming QUERY_2 for person_schedule_service 6 times:\n"
for i in {1..6} ; do check-env.sh -c 18 ; done | grep -v '^ *$'

[[ "${ENV_NAME}" != "TAUG" ]] && { echo -e "Start Kapacitor" ; auto_kapacitorstart ; auto_kapacitorlist ; }

# Wait for data load to finish
if [[ "${ENV_NAME}" == "TAUG" ]] ; then
  read -t 1800 -p "Waiting 30min before creating indexes. Press ENTER to cont."
else
  read -t 10800 -p "Waiting 3hrs before creating indexes. Press ENTER to cont."
fi

# Create INDEXES
echo -e "$(date) Starting Object Index Registration." | tee -a $_LOG $_FULL_LOG
auto_objectindexregistration

## Deploy notifiers
echo -e "\nDeploying notifiers with odsx...\n" | tee -a $_LOG $_FULL_LOG
auto_notifiers.sh -d

echo -e "$(date) ===Finished DIH space reinstall===" | tee -a $_LOG $_FULL_LOG
