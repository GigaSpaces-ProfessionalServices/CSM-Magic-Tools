package com.gigaspaces.common.test;

import com.gigaspaces.async.AsyncFuture;
import com.gigaspaces.common.task.MaxLocalDateTimeValueTaskNew;
import com.gigaspaces.common.task.MaxValueTask;
import java.io.OutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.concurrent.ExecutionException;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.space.CannotFindSpaceException;
import org.openspaces.core.space.SpaceProxyConfigurer;

public class App {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        if(args.length != 6 ){
            System.out.println("Please pass arguments in following order");
            System.out.println("<space_name> <lookup_locator> <lookup_group> <data_type> <table_name> <column_name>");
            return;
        }
        PrintStream out = System.out;
        System.setOut(new PrintStream(OutputStream.nullOutputStream()));

        String spaceName = args[0];
        String lookupLocator = args[1];
        String lookupGroup = args[2];
        String dataType = args[3];
        String tableName = args[4];
        String columnName = args[5];

        GigaSpace gigaSpace = getOrCreateSpace(spaceName,lookupLocator,lookupGroup);

        switch (dataType.toLowerCase()){
            case "long":
                AsyncFuture<Long> future = gigaSpace.execute(new MaxValueTask<Long>(Long.MAX_VALUE ,
                        tableName,  columnName));
                Long colVal = future.get();
                System.setOut(out);
                System.out.println("Max value ("+dataType+") for field '"+columnName+"' is "+colVal);
                break;
            case "localdatetime":
                LocalDateTime maxDateTime = LocalDateTime.of(2099,
                        Month.DECEMBER, 31, 23, 59, 59);

                AsyncFuture<LocalDateTime> future2 = gigaSpace.execute(new MaxLocalDateTimeValueTaskNew(maxDateTime ,
                        tableName, columnName));
                LocalDateTime dateVal = future2.get();
                System.setOut(out);
                System.out.println("Max value ("+dataType+") for field '"+columnName+"' is "+dateVal);
                break;
        }

    }
    public static GigaSpace getOrCreateSpace(String spaceName
            ,String lookupLocator, String lookupGroup) {
        if (spaceName == null) {
            return null;
        } else {
            System.out.printf("Connecting to space %s...%n", spaceName);
            try {
                return new GigaSpaceConfigurer(new SpaceProxyConfigurer(spaceName)
                        .lookupLocators(lookupLocator)
                        .lookupGroups(lookupGroup)
                        //.credentials("gs-admin","gs-admin")
                ).create();
            } catch (CannotFindSpaceException e) {
                System.err.println("Failed to find space: " + e.getMessage());
                throw e;
            }
        }
    }
}
