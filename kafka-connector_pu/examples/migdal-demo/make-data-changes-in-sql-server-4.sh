#!/usr/bin/env bash
set -e

# Initialize database and insert test data
cat sql/make-changes-4.sql | docker exec -i sqlserver bash -c '/opt/mssql-tools/bin/sqlcmd -U sa -P $SA_PASSWORD'

echo `basename "$0"` > last_step.txt
printf "\n"
echo "Ready for the next step"
