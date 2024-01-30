# runall.sh utility script

## Execute commands or run health checks on remote servers across selected clusters

### Requirements
* runall.sh - the main utility script
* runall.conf - configuration file for clusters and services
* host.yaml - yaml containing a list of all servers

### Deployment
1. mkdir /dbagiga/utils/runall
2. Copy runall.sh and runall.conf files from GIT to /dbagiga/utils/runall/
3. ln -s /giga/utils/runall/runall /usr/local/bin/runall.sh
4. chmod +x /usr/local/bin/runall.sh

### Configuration
runall configuration is managed in runall.conf.
Every cluster of servers has two parameters:
1. ENV_NAME - Discriptive cluster name (e.g. Space Servers / management servers etc)
2. SERVICES - A collection of parameters delimited by colons (e.g. F1:F2:F3)
* > F1 - the scope of check R / L
* >> R = check is executed remotely - not on the server itself
* >> L = check is executed localy on the server
* > F2 ::	the number of the service port
* > F3 ::	the name of the service

* Example:  to test ssh enter connectivity: R:22:SSH_SERVER

For help and usage run:<br>
	
	runall -h
