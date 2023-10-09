auto_influxdbstop
auto_influxdbremove
auto_influxdbinstall
influx -database mydb -execute 'ALTER RETENTION POLICY "autogen" ON "mydb" DURATION 30d SHARD DURATION 24h REPLICATION 1  DEFAULT'
auto_influxdbstart
