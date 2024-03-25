## Space Heap Analyzer

This tool let you run heap analyze on the cluster backups containers

and get a nice report in an Excel file in addition to json file.

The cluster could be run on your local machine or remotely.

It can also be a secured or non-secured Grid

#### Prerequisite

This tool is written in Python and in order to run it you need to have Python installed + modules like pandas and more, 
so please start by downloading Python.

In case you will encounter missing modules while execute, you can download it afterwords. 

#### Configuration
The tool can be run in 2 modes:
* interactive
* pre define configuration (app.config file)

##### app.config
In order to use this file you first need to configure the path for its location (without the file name).

For example, if the file location is /Users/david/app.config set ENV_CONFIG env variable as follows:


    export ENV_CONFIG=/Users/david/

#### Here is the app.config content example:

app.spaceheapanalyzer.managerip=localhost

app.spaceheapanalyzer.managerusername=username

app.spaceheapanalyzer.managerpassword=password
    
app.spaceheapanalyzer.jmappath=/Users/david/output
    
app.spaceheapanalyzer.javajarpath=/Users/david/SpaceHeapAnalyzer-1.0-SNAPSHOT-jar-with-dependencies.jar

#### How to run?
    python SpaceHeapAnalyzer.py

#### For Partition selection

For single partition selection just enter specific number

For Multiple partitions range selection enter '0-2' this with select 0, 1 and 2

For Multiple partitions selection enter '0,2,4' this with select 0, 2 and 4

Note if you select the Select ALL menu number in the Multiple partitions selection so report will be generated for ALL partitions