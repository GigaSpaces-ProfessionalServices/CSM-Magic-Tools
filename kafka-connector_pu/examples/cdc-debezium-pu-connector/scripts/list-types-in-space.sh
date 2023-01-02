curl -s http://localhost:8090/v2/spaces/demo/statistics/types | jq -r '.| to_entries[] | [.key, .value["entries"]|tostring] | join("\t")'
