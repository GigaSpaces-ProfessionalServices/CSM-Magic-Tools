# COMMON - Distributed Task to calculate Max value

### Run Example Application

1. Set space configuration parameter in HowToDo.java file
   > spaceName="dih-tau-space"
   
    > lookupLocator = "localhost";
   
    >lookupGroup = "xap-16.4.0";

2. Specify below parametres while creating task object
    > MaxValueTask(maxParameter, tableName, columnName) 
    
    Here Set table name and column name for which max value to be calculated

    Set maxParameter with the maximum value that for sure doesn't exist in the table for the column specified in above step

3. Run the HowToDo.java class