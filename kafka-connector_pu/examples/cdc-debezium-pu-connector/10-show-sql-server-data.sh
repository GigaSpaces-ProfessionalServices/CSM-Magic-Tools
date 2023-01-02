#!/usr/bin/env bash
set -e

cat sql/show-data.sql | docker exec -i sqlserver bash -c '/opt/mssql-tools/bin/sqlcmd -U sa -P $SA_PASSWORD'

printf "\n"
echo "Ready for the next step"
