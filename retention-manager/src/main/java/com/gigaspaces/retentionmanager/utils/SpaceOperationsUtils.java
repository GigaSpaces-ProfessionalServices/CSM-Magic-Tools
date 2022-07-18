package com.gigaspaces.retentionmanager.utils;

import com.gigaspaces.internal.cluster.node.impl.groups.sync.ISyncReplicationGroupOutContext;
import com.gigaspaces.retentionmanager.service.RetentionManagerService;
import com.j_spaces.jdbc.driver.GConnection;
import org.openspaces.core.GigaSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Component
public class SpaceOperationsUtils {
    private static final Logger log = LoggerFactory.getLogger(RetentionManagerService.class);
    @Autowired
    private GigaSpace gigaSpace;

    @Value("${gigaspaces.jdbc.url}")
    private String JDBC_URL;
    public Integer executeSqlUpdate(String sql, Object obj){
        log.info("Entering into -> executeSqlUpdate");
        sql = sql.replace("?",obj.toString());
        log.info("sql----------------->"+sql);
        Connection con=null;
        try{
            con = getConnection();
            //Statement stmt = con.createStatement();
            PreparedStatement preparedStatement = con.prepareStatement(sql);
            preparedStatement.setObject(1,obj);
            int recordsUpdated = preparedStatement.executeUpdate();

            log.info("Exiting from -> executeSqlUpdate");
            return recordsUpdated;
        }catch (Exception e){
            e.printStackTrace();
            return 0;
        }
    }

    public void executeQuery(String sql){
        log.info("Entering into -> executeQuery");
        log.info("sql----------------->"+sql);
        Connection con=null;
        try{
            con = getConnection();
            Statement stmt = con.createStatement();
            ArrayList<String> result = new ArrayList<String> ();
            ResultSet rs = stmt.executeQuery(sql);

            int columnsNumber = rs.getMetaData().getColumnCount();

            List<String> rows = new ArrayList<>();
            while(rs.next()){
                for (int i = 1; i <= columnsNumber; i++) {
                    if (i > 1) System.out.print(",  ");
                    String columnValue = rs.getString(i);
                    rows.add(columnValue);
                }

            }
            log.info("Total records returned ->"+rows.size());
            con.close();
            log.info("Exiting from -> executeQuery");
            //return recordsUpdated;
        }catch (Exception e){
            e.printStackTrace();
            //return 0;
        }
    }

    public Connection getConnection()
    {
        log.info("Entering into -> getConnection");
        Properties properties = new Properties();
        try {
            log.info("Exiting from -> getConnection");
            return DriverManager.getConnection(JDBC_URL, properties);

        } catch(SQLException ex) {
            System.err.println("SQLException: " + ex.getMessage());
        }

        return null;
    }

    public Connection getConnectionV1()
    {
        GConnection connection = null;
        try {
            Class.forName("com.j_spaces.jdbc.driver.GDriver");

        } catch(java.lang.ClassNotFoundException e) {
            System.err.print("ClassNotFoundException: ");
            System.err.println(e.getMessage());
        }

        try {
            connection = GConnection.getInstance(gigaSpace.getSpace());
            connection.setUseSingleSpace(false); //false = cluster, true=single

        } catch(SQLException ex) {
            System.err.println("SQLException: " + ex.getMessage());
        }

        return connection;
    }
}
