# ODSX automation

## Explanation

The filenames are self evident:
e.g. auto_spacestop - an odsx option implemented using expect binary that will stop all space servers 
according to the host.yaml file.

e.g. auto_spacerefresh - "refresh" is not an option in odsx so it is a script which includes several 
odsx options (i.e. auto_spacestop) plus some logic so as to refresh the space with following stops:
- stop spaces then managers
- clear spaces' and managers' directories
- start managers then spaces
- TS only after all bllspace GSC's are up and running
- load date using benchmark
The Leumi version includes logic concerning GARAGE environment and also the followin actions instead of benchmark
- run dll-parser
- run db2-feeder
- run mssql-feeder
