curl -s GET http://localhost:8082/topics | jq -r '.[] | select(. | (contains("my_connect") or contains("_confluent") or contains("ksql") or contains("docker") or contains("schema")) | not)'
