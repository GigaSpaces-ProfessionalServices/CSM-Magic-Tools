# gcom - Gigaspaces Components Orchestration Manager
Unattended / Interactive GigaSpaces Component D eployment

## Usage:
run
```
gcom -h
```

## Changelog

### Next versions planned changes
* calculate and warn against exceeding available memory

### Version 0.2.2
* added services list under Information category

### Version 0.2.1
* added option to bulk change presets defaults

### Version 0.2.0
* removed clear screen for unattended mode
* added wizard-like dictionary based menu with categories tree

### Version 0.1.4
* removed clear screen for unattended execution

### Version 0.1.0
* menu supports multiple selection of services: ranges and comma separated values
* added script generator option

### Version 0.0.2
* changed container deployment logic per free RAM on host
* added information to list function
* bash version verification for supported features

### Version 0.0.1
* initial release
* microservices deployment operations
* interactive 'wizard-like' mode using presets to streamline executions
* unattended mode for full automation
* choose between complete deployment or selected operations for a more granular control of execution
* supports multi-instance deployments with smart container distribution across grid space nodes