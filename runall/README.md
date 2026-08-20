# runall.sh utility script

Execute commands or run health checks on remote servers across all clusters

The function get_targeted_servers() holds the parameters necessary

Every cluster of servers has 3 parameters:<br>1. ENV_NAME :: Discriptive cluster name (e.g Spaces / management servers / DI servers etc)<br>2. SERVER_LIST :: space delimited list of host names (as resolved in DNS)<br>3. SERVICES :: every service is constructed of 3 fields delimited by a colon
  
 	field 1 ::	the type of check R (=Remote) or L (=Local) <br>
			*** some services have to be tested remotely (e.g. PING or SSH) to ensure connectivity
	
	field 2 ::	the number of the service port
	
	field 3 ::	the name of the service<br>
	*** Example:  to test ssh connectivity one would enter R:22:SSH_SERVER (NOTE: no spaces allowed!)

If a new cluster needs to be added:<br>1.	Add a case to get_targeted_servers() function<br>2.	Add the case to parameter ENV_TYPES

For help run:<br>
	
	./runall.sh -h
