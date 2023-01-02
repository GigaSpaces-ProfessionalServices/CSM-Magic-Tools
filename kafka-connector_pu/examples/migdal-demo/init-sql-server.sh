#!/usr/bin/env bash
set -e

# Initialize database and insert test data
cat sql/init-database.sql | docker exec -i sqlserver bash -c '/opt/mssql-tools/bin/sqlcmd -U sa -P $SA_PASSWORD'

printf "\n"
echo "Ready for the next step"
