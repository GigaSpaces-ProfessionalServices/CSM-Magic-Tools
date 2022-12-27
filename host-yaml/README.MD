# Bash script for querying odsx file host.yaml
Allows for environment without linux yp util having to be installed
Remove ^M characters before copying to Linux machine (dos2unix)
## Parameters:
Provide only one parameter which is the role name
## Output:
All hosts belonging to role will be displayed as appearing in host.yaml file
## Example:
### The following will display all hosts belonging to "space" role
host-yaml.sh space
