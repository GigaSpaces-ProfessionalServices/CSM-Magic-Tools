
# Cockpit

The cockpit should ensure cross-cluster similarity by controlling load tasks which are not on-line like cdc, measure data across the clusters and provide alerts and observability.

Requirements: 
> - Linux service on a standalone server
> - Data layer :  use sqlite for the service and influxdb for telemetric 
> - Define jobs : atomic load command or atomic validate command 
> - Define task : 1 or more jobs. support retry, logs , telemetric, tolerance 
> - Job: load->feeder : invokes single feeder command through ODSX cli. (sql/ db2)
> - Job: validate->Table<X> : invoke data-validator REST command and store in Influx 
> - Kapacitor to run as a service, uses the influxDB to generate alerts 
> - Grafana to show dashboards based on the Influx
> - Upon tool update - no need to remove data and config 
> - tool must have a version and build number 
> - Language : Python