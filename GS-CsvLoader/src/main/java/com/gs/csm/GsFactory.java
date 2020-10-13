package com.gs.csm;

import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.space.CannotFindSpaceException;
import org.openspaces.core.space.EmbeddedSpaceConfigurer;
import org.openspaces.core.space.SpaceProxyConfigurer;

import java.io.File;

public class GsFactory {
    public static String spaceName = "demo";
    public static String locators = "localhost";
    public static String groups = "xap-15.5.1";
    public static int timeout = 60_000;
    public static String csvPojo = "com.gs.csm.data.IBMStock";
    public static String csvFile = "src/main/resources/ibm.us.csv";
    public static String packageName = "com.gs.csm.data";
    public static String pojoOutputDirectory = "src/main/java";
    public static int limitRows = 10000000;
    public static int numberOfIterations = 1;


    public static GigaSpace getOrCreateSpace(String spaceName, boolean embedded) {
        if (embedded) {
            System.out.printf("Creating an embedded space %s...%n", spaceName);
            return new GigaSpaceConfigurer(new EmbeddedSpaceConfigurer(GsFactory.spaceName)
                    .lookupLocators(GsFactory.locators).lookupGroups(GsFactory.groups)).create();
        } else {
            System.out.printf("Connecting to space %s...%n", spaceName);
            try {
                return new GigaSpaceConfigurer(new SpaceProxyConfigurer(spaceName)
                        .lookupLocators(GsFactory.locators).lookupGroups(GsFactory.groups)
                        .lookupTimeout(GsFactory.timeout)).create();
            } catch (CannotFindSpaceException e) {
                System.err.println("Failed to find space: " + e.getMessage());
                throw e;
            }
        }
    }
}
