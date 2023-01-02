STATUS=$(curl -s http://localhost:8083/connectors/debezium-connector/status | jq -r '.tasks[0].state')

RED='\033[0;31m'
GRN='\033[0;32m'
NC='\033[0m' # No Color

if [[ $STATUS = "RUNNING" ]]
then
  printf "${GRN}${STATUS}${NC}\n"
else
  printf "${RED}${STATUS}${NC}\n"
fi
