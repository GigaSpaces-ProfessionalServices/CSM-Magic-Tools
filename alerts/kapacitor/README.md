Kapacitor
=========

Kapacitor is influxdb companion software.
It's official site can be reached here https://docs.influxdata.com/kapacitor/v1.6/.  

## Short implementation guide:

Install kapacitor from suitable RPM and start it by command:  
> \# systemctl start kapacitor  

Kapacitor with its default settings starts its own internal API HTTP server
on port 9092, which can be busy, then the start can fails.
You can verify that by inspecting `/var/log/kapacitor/kapacitor.log`.
To resolve this conflict, edit the `/etc/kapacitor/kapacitor.conf`
configuration file tuning value of `bind-address`:  
> [http]  
>  ..  
> bind-address = ":9992"  

The kapacitor CLI works using this API port and will not work with non-default values.
You should set the environment variable to fix this:  
> \# export KAPACITOR_URL=http://localhost:9992  

**WARNING**: The influxdb had not configured for any security currently.
If any security will applied to influxdb,
then the `[[influxdb]]` section should be updated.

Then, the service could be started:  
> \# systemctl start kapacitor.service  
> \# tail -f /var/log/kapacitor/kapacitor.log  

You will see flood of messages interconnecting with influxdb.
If the conversation is OK, you can shut these messages by setting
`log-enabled = false` at `[http]` section of `/etc/kapacitor/kapacitor.conf` file.  

The kapacitor could be configured to send alerts in many mannors.
Bank Leumi use servicenow, which documentations could be found here
https://docs.servicenow.com/bundle/paris-it-operations-management/page/product/event-management/task/send-events-via-web-service.html

This API requires POST custom JSON data.
The kapacitor can do that perfect, using `httppost` event handler
with json template.

Every new alert contains three part:
1. The alert definition in TICK format as described here:
https://docs.influxdata.com/kapacitor/v1.6/nodes/alert_node/
2. JSON template file as described here:
https://docs.influxdata.com/kapacitor/v1.6/event_handlers/post/#alert-templates
3. Relevant section in `/etc/kapacitor/kapacitor.conf` file, tighting things together.

These are usefull commands of kapacitor CLI:  
> \# kapacitor define cpu_alert -tick cpu_alert.tick  
> \# kapacitor list tasks  
> \# kapacitor show cpu_alert  
> \# kapacitor enable cpu_alert  
> \# kapacitor disable cpu_alert  

These are usefull commands of influx CLI:  
> \# influx  
> \> show databases;   
> \> use databasename  
> \> show retention policies  
> \> ALTER RETENTION POLICY "autogen" ON mydb DURATION 32d  
> \> show measurements with measurement =~ /.*cpu.*/  
> \> show measurements with measurement =~ /(?i).*CPU.*/  
> \> show tag keys from "os_cpu_used-percent"  
> \> show series from "process_cpu_time-total" limit 20  
> \> show field keys from "os_cpu_used-percent"  
> \> select * from "os_cpu_used-percent" limit 1  

Refer to https://docs.influxdata.com/influxdb/v1.8/query_language/explore-data/
for queries syntax


## Debug HTTP POST JSON messages

It is difficult to catch POST messages for debugging purpose.
The most easy way is to send messages to your own HTTP server to catch them.
The tool `socat` could help you with this:  
> \# yes "HTTP/1.1 200 OK;\r" | socat - TCP-LISTEN:4242,reuseaddr,fork  

This line listen for HTTP on 4242, print request on screen and answer OK for sender.
