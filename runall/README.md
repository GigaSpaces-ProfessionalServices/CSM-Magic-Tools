# runall.sh utility script

Execute commands or run health checks on remote servers across all clusters

The function get_targeted_servers() holds the parameters necessary
Every cluster of servers has 3 parameters:
1. Discriptive cluster name (e.g Spaces / management servers / DI servers etc)
2. SERVER_LIST :: space delimited list of host names (as resolved in DNS)
3. SERVICES :: every service is constructed of 3 fields delimited by a colon
	#1 :: the type of check R (=Remote) or L (=Local) *** some service have to be tested remotely (e.g. PING or SSH) to ensure connectivity
	#2 :: the number of the service port
	#3 :: the name of the service
	Example:  to test ssh connectivity one would enter R:22:SSH_SERVER (NOTE: no spaces allowed!)

If a new cluster needs to be added:
1. Add a case to get_targeted_servers() function
2. add the case to parameter ENV_TYPES

For help run:
	./runall.sh -h