topics=$(./list-topics-filtered.sh)

if [ -z "$topics" ]
then
  exit 0
fi

output=''
while IFS= read -r topic; do
    x=$(docker exec connect /kafka/bin/kafka-run-class.sh kafka.tools.GetOffsetShell --broker-list kafka:29092 --topic $topic --time -1 --offsets 1 | awk -F  ":" '{sum += $3} END {print sum}')
    output="${output}\n${topic}\t${x}"
done <<< "$topics"
printf $output
printf '\n'
