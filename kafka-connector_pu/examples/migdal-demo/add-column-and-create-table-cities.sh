#!/usr/bin/env bash
set -e

cat sql/add-column-to-product-table.sql | docker exec -i sqlserver bash -c '/opt/mssql-tools/bin/sqlcmd -U sa -P $SA_PASSWORD'
cat sql/create-table-cities.sql | docker exec -i sqlserver bash -c '/opt/mssql-tools/bin/sqlcmd -U sa -P $SA_PASSWORD'

echo `basename "$0"` > last_step.txt
printf "\n"
echo "Ready for the next step"
