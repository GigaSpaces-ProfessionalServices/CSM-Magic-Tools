CODE=$(curl -s -o /dev/null -w %{http_code} http://localhost:8083/connectors)

RED='\033[0;31m'
GRN='\033[0;32m'
NC='\033[0m' # No Color

if [[ $CODE = "200" ]]
then
  printf "${GRN}Ready${NC}, http_code=${CODE}\n"
else
  printf "${RED}Not ready${NC}, http_code=${CODE}\n"
fi
