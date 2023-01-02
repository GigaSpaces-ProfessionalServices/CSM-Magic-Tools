STATUS=$(curl -s http://localhost:8090/v2/spaces/demo/statistics/types | jq -r '.| to_entries[] | [.key] | contains(["java.lang.Object"])' 2>/dev/null)

RED='\033[0;31m'
GRN='\033[0;32m'
NC='\033[0m' # No Color

if [[ $STATUS = *"true"* ]]
then
  printf "${GRN}READY${NC}\n"
else
  printf "${RED}NOT READY${NC}\n"
fi
