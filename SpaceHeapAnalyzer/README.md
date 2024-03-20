
### app.config file path should be stored in - "ENV_CONFIG" environment variable

In app.config file store these 5 values for default configurations

app.spaceheapanalyzer.managerip=localhost

app.spaceheapanalyzer.managerusername=username

app.spaceheapanalyzer.managerpassword=password
    
app.spaceheapanalyzer.jmappath=outputpath
    
app.spaceheapanalyzer.javajarpath=path/SpaceHeapAnalyzer-1.0-SNAPSHOT-jar-with-dependencies.jar

    run ./SpaceHeapAnalyzer.py

### For Partition selection

For single partition selection just enter specific number

For Multiple partitions range selection enter '0-2' this with select 0, 1 and 2

For Multiple partitions selection enter '0,2,4' this with select 0, 2 and 4

Note if you select the Select ALL menu number in the Multiple partitions selection so report will be generated for ALL partitions