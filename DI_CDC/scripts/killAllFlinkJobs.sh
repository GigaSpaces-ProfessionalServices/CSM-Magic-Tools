#!/bin/bash

json_data=$(curl -s localhost:8081/jobs |jq)
echo "Cancelling old flink jobs if any ..."
for job_entry in $(echo "$json_data" | jq -c '.jobs[]'); do
  JOB_ID=$(echo "$job_entry" | jq -r '.id')
  curl -X PATCH "localhost:8081/jobs/${JOB_ID}?mode=cancel"
  sleep 1
done

