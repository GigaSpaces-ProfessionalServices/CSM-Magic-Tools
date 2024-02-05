package com.gigaspaces.common.test;

import com.gigaspaces.async.AsyncFuture;
import com.gigaspaces.common.task.MaxLocalDateTimeValueTask;
import com.gigaspaces.common.task.MaxValueTask;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.concurrent.ExecutionException;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.space.CannotFindSpaceException;
import org.openspaces.core.space.SpaceProxyConfigurer;

public class HowToDo {

    /*
    Set below parameters to connect to space
    */
    private static String spaceName="dih-tau-space";
    private static String lookupLocator = "localhost";
    private static String lookupGroup = "xap-16.4.0";


    public static void main(String[] args) throws ExecutionException, InterruptedException {
        GigaSpace gigaSpace = getOrCreateSpace(spaceName,lookupLocator,lookupGroup);

        // ### Example-1 Data Type 'Long'
        AsyncFuture<Long> future = gigaSpace.execute(new MaxValueTask<Long>(Long.MAX_VALUE ,
                "T_Long",  "T_IDKUN"));
        Long colVal = future.get();
        System.out.println("Max value (Long) for field 'T_IDKUN' is "+colVal);

        // ### Example-2 Data Type 'LocalDateTime'
        LocalDateTime maxDateTime = LocalDateTime.of(2099,
                Month.DECEMBER, 31, 23, 59, 59);

        AsyncFuture<LocalDateTime> future2 = gigaSpace.execute(new MaxLocalDateTimeValueTask(maxDateTime ,
                "STUD.TL_KURS",  "T_IDKUN"));
        LocalDateTime dateVal = future2.get();
        System.out.println("Max value (LocalDateTime) for field 'T_IDKUN' is "+dateVal);
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
