package com.gigaspaces.datavalidator;

import com.gigaspaces.datavalidator.model.MeasurementRequestModel;
import org.springframework.context.annotation.PropertySource;
import com.gigaspaces.datavalidator.model.*;


@PropertySource(value = "file:application.properties", ignoreResourceNotFound = true )
public class DataValidatorApplication {
    public static void main(String[] args) {
        if(args.length<4){
            System.out.println("Missing arguments");
            System.out.println("Please pass 3 arguments <GSManagerHostIP> <GSLookupGroup> <GSSpaceName> <GSTableName>");
            return;
        }
        System.out.println("Hello");
        String dataSourceType="gigaspaces";
        String managerHostIp = args[0];
        String gsLookupGroup = args[1];
        String spaceName=args[2];
        String tableName=args[3];
        String field="*";//""T_IDKUN";

        MeasurementRequestModel m = new MeasurementRequestModel();

        m.setTest("count");
        m.setType("count");
        m.setSchemaName(spaceName);
        m.setTableName(tableName);
        m.setFieldName(field);
        m.setWhereCondition("");
        m.setLimitRecords("-1");

        m.setDataSourceType(dataSourceType);
        m.setDataSourceHostIp(managerHostIp);
        m.setDataSourcePort("4174");
        m.setUsername("");
        m.setPassword("");
        m.setIntegratedSecurity("");
        m.setProperties("");
        m.setAuthenticationScheme("");
        m.setGsLookupGroup(gsLookupGroup);

        TestTask testTask = new TestTask("1",m);
        String result = testTask.executeTask();
        System.out.println("Result: "+result);
        System.out.println("---------END---------");

    }

}
