Alerts Description
==================

 ## consul-status.tick

This alert examine the status of consul.service and consul-template.service running on servers.
These services runs on northbound and space servers.
The status of services checked by telegraf itself via systemd plugin.

Stop service consul or consul-template on any host to generate an alert.

 ## cpu-alert.tick

Not in use. The CPU state examined by other already existing agent.

 ## dil-kafka-status.tick

This alert examine the status of odsxkafka.service running on DI servers.
The status of service checked by telegraf itself via systemd plugin.

Stop service odsxkafka.service on any host to generate an alert.

 ## dil-zookeeper-status.tick

This alert examine the status of odsxzookeeper.service running on DI servers.
The status of service checked by telegraf itself via systemd plugin.

Stop service odsxzookeeper.service on any host to generate an alert.
Due to service dependencies, the odsxkafka.service will goes down too.

 ## disk-usage-pct.tick

This alert arised if disk filled over 85% for small disks, over 90% for medium disk and over 95% for large disks.
The measurement done by telegraf itself using inputs.disk plugin (enabled by default).

Fill small disks (/boot is a good choice) to get alert.

 ## jvm-app-failed.tick

This alert based on jvm_uptime measurement in influxdb.
It tests the records for last five minutes to guess which process had not updates its status for some time.
The process filtered by existing pu_name and pu_instance_id, thus this alert represents application process failure only.

Kill any container process to generate this alert.

 ## jvm-memory-used-pct.tick

This alert arised when jvm_memory_heap_used-percent measurement is over 88% .

Fill the space with objects untill 88% .

 ## jvm-proc-failed.tick

This alert based on jvm_uptime measurement in influxdb.
It tests the records for last five minutes to guess which process had not updates its status for some time.
The process filtered out the application process, thus it represent system processes only.

Kill any LUS/GSA/GSM process to generate this alert.

 ## microservices-latency.tick

This alert arised when pu_ServiceRoute_service-latency measurement is over 150.

No manual simulation for that.

 ## mservice-status.tick

This alert based on ability of consul to check the status  of registered microservices.
It measured by telegraf using consul plugin.

Kill microservice JVM to get the alert.

 ## nginx-load-raw.tick

Not an alert. Sent total amount of requests per minute.

 ## nginx-load.tick

Warn where request rate is above 150 hits/second.

 ## nginx-status.tick

This alert examine the status of nginx.service running on NB servers.
The status of service checked by telegraf itself via systemd plugin.

Stop service nginx.service on any host to generate an alert.

 ## pipeline-status.tick

Data aging alert based on fact that source replicated table has a frequently updated field with UTC timestamp, preferrable in POSIX since EPOCH time.
A telegraf fire external script that check the target table for value and fill up the pipelineState measurement.
If the value is below one minute, the alert is fired.

It is possible to play with script to generate any fake values and fire an alert.

 ## redo_log-size.tick

This alert arised when space_replication_redo-log_size measurement is more then 1000.

No manual simulation for that.

 ## space-status.tick

This alerts reflects the result of external script.
It can be a three state "healthy" , "partial" or "faulty"
A telegraf is fire the external script that generates these status, based on two API queries.

The alert can be simulated by playing with script.

 ## total-calls-pct-failed.tick

This alert arised when fault queries on nginx is over than 5% of total.
The telegraf fill this measurement using nginx access_log as its input.
It is important that this file exist and readable.

You can add some amount of fake failed queries to the end of access_log to call this alert.
