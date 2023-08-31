package com.gigaspaces.Leumi;

import com.gigaspaces.Leumi.dto.FeederConfig;
import com.gigaspaces.Leumi.dto.MSSQLConfig;
import com.gigaspaces.Leumi.handlers.ProgressRequestHandler;
import com.gigaspaces.Leumi.handlers.StartRequestHandler;
import com.gigaspaces.Leumi.handlers.StatusRequestHandler;
import com.gigaspaces.Leumi.handlers.StopRequestHandler;
import com.gigaspaces.Leumi.utils.Progress;
import com.google.gson.Gson;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.space.SpaceProxyConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import spark.Spark;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import static spark.Spark.*;

public class Server {
    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    @Resource // Injected by Spring
    private GigaSpace gigaSpace;

    @Value("${rest.port}")
    private String port;

    @Value("${space.name}")
    private String spaceName;

    @Value("${space.securityConfig.username}")
    private String username;

    @Value("${space.securityConfig.password}")
    private String password;

    @Value("${rest.group}")
    private String group;

    @Value("${rest.secured}")
    private boolean restSecured;

    @Value("${space.secured}")
    private boolean spaceSecured;

    @Value("${mssql.server}")
    private String server;

//    @Value("${mssql.port}")
//    private int mssqlPort;

    @Value("${mssql.integratedSecurity}")
    private String integratedSecurity;

    @Value("${mssql.authenticationScheme}")
    private String authenticationScheme;

    @Value("${mssql.databaseName}")
    private String databaseName;

//    @Value("${mssql.user}")
//    private String mssqlUser;

//    @Value("${mssql.password}")
//    private String mssqlPassword;

    @Value("${feeder.writeBatchSize}")
    private int writeBatchSize;

    @Value("${feeder.sleepAfterWriteInMillis}")
    private int sleepAfterWriteInMillis;

    @Value("${feeder.progressPrintIntervalInSec}")
    private int progressPrintIntervalInSec;

    public Server() {
    }

    public static void main(String[] args) {

        // keyPassword - The password to the private key in the key store.
        // keyStore - Path to the key store file. The path can be absolute or relative to the directory in which the process is started.
        // keyStorePassword - Password to the key store.
        // keyStoreType - The type of the key store (default: JKS)
        // protocol - TLS protocol to use. The protocol must be supported by JVM.
        // needClientAuth - Whether to require client authentication (default: false)
        // trustStore - Path to the trust store file. The path can be absolute or relative to the directory in which the process is started.
        // trustStorePassword - Password for the trust store.
        // trustStoreType - The type of the trust store (default: JKS)

        // https://www.educative.io/edpresso/keystore-vs-truststore
        // Keystore is used to store private key and identity certificates that a specific program should present to both parties (server or client) for verification.
        // Truststore is used to store certificates from Certified Authorities (CA) that verify the certificate presented by the server in SSL connection.

        //Spark.secure("keystore.jks", "changeit", null, "changeit", false);
        //defaults
        String args0_spaceName = "mssqlspace";
        int args1_port = 8020;

        //args override 1st- space Name, 2nd - port
        if(args.length == 1){
            args0_spaceName = args[0];
        } else if (args.length == 2) {
            args0_spaceName = args[0];
            args1_port = Integer.parseInt(args[1]);
        }
        Server server = new Server();
        server.spaceSecured=false;
        server.restSecured=false;
        server.gigaSpace =
                new GigaSpaceConfigurer(
                    new SpaceProxyConfigurer(args0_spaceName)
                    .lookupTimeout(10000)//.lookupLocators("localhost")
                    .lookupGroups(server.group)
                   // .credentials(server.username,server.password)
                ).create();

        server.initKeepAlive();

     //   server.mssqlPort = args1_port;
        server.spaceName = args0_spaceName;
        server.initialize();
    }

    protected void initKeepAlive(){
        if(spaceSecured) {
            gigaSpace = new GigaSpaceConfigurer(
                    new SpaceProxyConfigurer(spaceName)
                            .lookupTimeout(10000)//.lookupLocators("localhost")
                            .lookupGroups(group)
                            .credentials(username,password)
            ).create();
        }
        else
        {
            gigaSpace = new GigaSpaceConfigurer(
                    new SpaceProxyConfigurer(spaceName)
                            .lookupTimeout(10000)
                            .lookupGroups(group)).create();
        }

    }

    @PreDestroy
    public void shutdown() {
        logger.info("Stopping Server");
        stop();
    }

    @PostConstruct
    public void initialize() {
        if (restSecured) {
            Spark.secure("keystore.jks", "changeit", null, null, false);
        }
        final Gson gson = new Gson();
        initKeepAlive();
        StatusRequestHandler statusRequestHandler = new StatusRequestHandler(gigaSpace);

        MSSQLConfig mssqlConfig = new MSSQLConfig(server, true, authenticationScheme, databaseName);
        FeederConfig feederConfig = new FeederConfig(writeBatchSize, sleepAfterWriteInMillis, progressPrintIntervalInSec);
        Progress progress = new Progress(progressPrintIntervalInSec);
        StartRequestHandler startRequestHandler = new StartRequestHandler(gigaSpace, mssqlConfig, feederConfig, progress);
        StopRequestHandler stopRequestHandler = new StopRequestHandler(gigaSpace);
        ProgressRequestHandler progressRequestHandler = new ProgressRequestHandler(gigaSpace, progress);
        port(Integer.parseInt(port));
        initExceptionHandler(Throwable::printStackTrace);
        init();
        logger.info("Batch size: "+writeBatchSize);
        logger.info("Server initialized. " +
                "\n - Running on port: " + port +
                "\n - Connected to Space: " + spaceName);

        path("table-feed/", () -> {
            before("/*", (q, a) -> logger.info("Received api call :" + q.pathInfo()));
            get("/status", statusRequestHandler.process, gson::toJson);
            post("/start", startRequestHandler.process, gson::toJson);
            post("/stop", stopRequestHandler.process, gson::toJson);
            get("/progress", progressRequestHandler.process, gson::toJson);
        });
    }
}
