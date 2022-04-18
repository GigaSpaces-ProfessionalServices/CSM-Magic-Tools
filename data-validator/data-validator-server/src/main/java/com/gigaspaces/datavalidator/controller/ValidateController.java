package com.gigaspaces.datavalidator.controller;

import com.gigaspaces.datavalidator.TaskProcessor.CompleteTaskQueue;
import com.gigaspaces.datavalidator.TaskProcessor.TaskQueue;
import com.gigaspaces.datavalidator.db.HibernateProxyTypeAdapter;
import com.gigaspaces.datavalidator.db.service.*;
import com.gigaspaces.datavalidator.model.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonMethod;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

@RestController
public class ValidateController {

    @Autowired
    private TestTaskService odsxTaskService;
    @Autowired
    private MeasurementService measurementService;
    @Autowired
    private DataSourceService dataSourceService;
    @Autowired
    private AgentService agentService;
    
    private Logger logger = Logger.getLogger(this.getClass().getName());

    @GetMapping("/compare/{measurementId1}/{measurementId2}")
    public Map<String,String> compareMeasurement(@PathVariable String measurementId1
            ,@PathVariable String measurementId2
            ,@RequestParam(defaultValue="0") int executionTime) {

        Map<String,String> response = new HashMap<>();

        try {

            Measurement measurement1 = measurementService.getMeasurement(Integer.parseInt(measurementId1));
            Measurement measurement2 = measurementService.getMeasurement(Integer.parseInt(measurementId2));

            TestTask task;
            List<Measurement> measurementList = new LinkedList<>();
            measurementList.add(measurement1);
            measurementList.add(measurement2);

            if(executionTime == 0){

                task = new TestTask(odsxTaskService.getUniqueId(), System.currentTimeMillis()
                        ,"Compare", measurementList);
                String result = task.executeTask();
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.setVisibility(JsonMethod.FIELD, JsonAutoDetect.Visibility.ANY);
                objectMapper.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false);
                String jsonTask = objectMapper.writeValueAsString(task);
                logger.info("task result: "+task.getResult());
                logger.info("task result2: "+result);
                response.put("response", jsonTask);

            }else{

                Calendar calScheduledTime = Calendar.getInstance();
                calScheduledTime.add(Calendar.MINUTE, executionTime);
                task =new TestTask(odsxTaskService.getUniqueId(), calScheduledTime.getTimeInMillis()
                        ,"Compare", measurementList);
                TaskQueue.setTask(task);
                logger.info("Task scheduled and will be executed at "+calScheduledTime.getTime());
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.setVisibility(JsonMethod.FIELD, JsonAutoDetect.Visibility.ANY);
                objectMapper.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false);
                String jsonTask = objectMapper.writeValueAsString(task);
                response.put("response", jsonTask);
                //response.put("response", "scheduled");

            }

            odsxTaskService.add(task);

        } catch (Exception exe) {
            exe.printStackTrace();
            response.put("response", exe.getMessage());
        }
        return response;
    }
    @PostMapping(path = "/measurement/register", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String,String> registerMeasurement(@RequestBody MeasurementRequestModel measurementRequestModel) {
        Map<String,String> response = new HashMap<>();
        try {
            Measurement measurement = new Measurement(measurementService.getAutoIncId()
                    , Long.parseLong(measurementRequestModel.getDataSourceId())
                    ,measurementRequestModel.getTest()
                    , measurementRequestModel.getSchemaName(), measurementRequestModel.getTableName()
                    , measurementRequestModel.getFieldName()
                    , "-1",measurementRequestModel.getWhereCondition());
            measurement.setStatus(ModelConstant.ACTIVE);
            measurementService.add(measurement);
            response.put("response", "Measurement Registered successfully");
            return response;

        } catch (Exception exe) {
            exe.printStackTrace();
            response.put("response", exe.getMessage());
            return response;
        }
    }
    @GetMapping("/measurement/run/{measurementId}")
    public Map<String,String> runMeasurement(@PathVariable String measurementId
            , @RequestParam(defaultValue="0") int executionTime
    ) {

        Map<String,String> response = new HashMap<>();

        try {

            Measurement measurement = measurementService.getMeasurement(Long.parseLong(measurementId));
            List<Measurement> measurementList = new LinkedList<Measurement>();
            measurementList.add(measurement);
            TestTask task;

            if(executionTime == 0){

                task = new TestTask(odsxTaskService.getUniqueId(), System.currentTimeMillis()
                        ,"Measure",measurementList);
                task.executeTask();

                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.setVisibility(JsonMethod.FIELD, JsonAutoDetect.Visibility.ANY);
                objectMapper.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false);
                String jsonTask = objectMapper.writeValueAsString(task);
                response.put("response", jsonTask);

            }else{

                Calendar calScheduledTime = Calendar.getInstance();
                calScheduledTime.add(Calendar.MINUTE, executionTime);
                task = new TestTask(odsxTaskService.getUniqueId(), calScheduledTime.getTimeInMillis()
                        ,"Measure", measurementList);
                logger.info("Task scheduled and will be executed at "+calScheduledTime.getTime());
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.setVisibility(JsonMethod.FIELD, JsonAutoDetect.Visibility.ANY);
                objectMapper.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false);
                String jsonTask = objectMapper.writeValueAsString(task);
                response.put("response", jsonTask);
            }
            TaskQueue.setTask(task);
            odsxTaskService.add(task);

        } catch (Exception exe) {
            exe.printStackTrace();
            response.put("response", exe.getMessage());
        }

        return response;
    }
    @DeleteMapping("/measurement/remove/{measurementId}")
    public Map<String,String> removeMeasurement(@PathVariable String measurementId
            , @RequestParam(defaultValue="0") int executionTime
    ) {
        Map<String,String> response = new HashMap<>();
        try {
        	 Measurement measurement = measurementService.getMeasurement(Long.parseLong(measurementId));
        	 measurement.setStatus(ModelConstant.DELETED);
            measurementService.update(measurement);
            response.put("response", "Measurement with id '"+measurementId+"' is removed");
            return response;

        } catch (Exception exe) {
            exe.printStackTrace();
            response.put("response", exe.getMessage());
            return response;
        }
    }
    @PostMapping(path = "/measurement/update", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String,String> updateMeasurement(@RequestBody MeasurementRequestModel measurementRequestModel) {
        Map<String,String> response = new HashMap<>();
        try {
            Measurement measurement = measurementService.getMeasurement(Long.parseLong(measurementRequestModel.getMeasurementId()));
            measurement.setType(measurementRequestModel.getTest());
//            measurement.setDataSourceType(measurementRequestModel.getDataSourceType());
//            measurement.setDataSourceHostIp(measurementRequestModel.getDataSourceHostIp());
//            measurement.setDataSourcePort(measurementRequestModel.getDataSourcePort());
//            measurement.setUsername(measurementRequestModel.getUsername());
//            measurement.setPassword(measurementRequestModel.getPassword());
            measurement.setSchemaName(measurementRequestModel.getSchemaName());
            measurement.setTableName(measurementRequestModel.getTableName());
            measurement.setFieldName(measurementRequestModel.getFieldName());
            measurement.setWhereCondition(measurementRequestModel.getWhereCondition());
            measurement.setLimitRecords("-1");
            //measurement.setIntegratedSecurity(measurementRequestModel.getIntegratedSecurity());
            measurementService.update(measurement);
            response.put("response", "Measurement with id '"+measurementRequestModel.getMeasurementId()+"' updated successfully");
            return response;
        } catch (Exception exe) {
            exe.printStackTrace();
            response.put("response", exe.getMessage());
            return response;
        }
    }
    @GetMapping("/scheduledtasks")
    public Map<String,String> getScheduledTasks() throws IOException {

        Map<String,String> response = new HashMap<>();
        List<TestTask> allTasks = new ArrayList<TestTask>();
        allTasks.addAll(TaskQueue.getTasks());
        allTasks.addAll(CompleteTaskQueue.getTasks());
        //Collections.sort(allTasks, (left, right) -> Math.toIntExact(left.getId() - right.getId()));
        Collections.sort(allTasks, Comparator.comparing(TestTask::getUuid));
        Gson gson = new Gson();
        //String jsonTaskList = gson.toJson(allTasks);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(JsonMethod.FIELD, JsonAutoDetect.Visibility.ANY);
        objectMapper.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false);
        String jsonTaskList = objectMapper.writeValueAsString(allTasks);
        response.put("response", jsonTaskList);
        return response;
    }

    @GetMapping("/measurement/list")
    public Map<String,String> getMeasurementList(){
        logger.info("In MeasurementList");
        Map<String,String> response = new HashMap<>();
        List<Measurement> measurementList = measurementService.getActiveMeasurement();
        logger.info("measurementList size: "+measurementList.size());
        Collections.sort(measurementList, (left, right) -> Math.toIntExact(left.getId() - right.getId()));
        //Gson gson = new Gson();
        //GsonBuilder b = new GsonBuilder();
        //b.registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY);
        //Gson gson = b.create();
        //String jsonTaskList = gson.toJson(measurementList);
        //response.put("response", jsonTaskList);

        for (Measurement m : measurementList) {
            logger.info("M id:"+m.getId());
            logger.info("M->D id: "+m.getDataSource().getDataSourceHostIp());
            logger.info("M->D->A id: "+m.getDataSource().getAgent().getId());
            for(DataSource ds: m.getDataSource().getAgent().getDataSources()){
                logger.info("loop::M->D->A->D : "+ds.getId());
            }
        }
        String jsonTaskList = null;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setVisibility(JsonMethod.FIELD, JsonAutoDetect.Visibility.ANY);
            objectMapper.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false);
            jsonTaskList = objectMapper.writeValueAsString(measurementList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        response.put("response",jsonTaskList );
        return response;
    }

    @GetMapping("/query/{tableName}/{columnName}")
    public Map<String,String> query(@PathVariable String tableName,@PathVariable String columnName
            , @RequestParam String dataSourceHostIp
            , @RequestParam String dataSourcePort
            , @RequestParam String username
            , @RequestParam String password
            , @RequestParam String schemaName
            , @RequestParam(defaultValue="0") int executionTime
            , @PathVariable String integratedSecurity){

        String dataSourceType="gigaspaces";
        String test="max";
        String testType = "Measure";
        
        Measurement measurement = new Measurement(Measurement.getMaxId(), test
                , dataSourceType, dataSourceHostIp, dataSourcePort
                , username, password, schemaName
                , tableName, columnName,"-1", "");

        measurementService.add(measurement);

        List<Measurement> measurementList = new LinkedList<Measurement>();
        measurementList.add(measurement);

        TestTask task;
        Map<String,String> response = new HashMap<>();

        if(executionTime == 0){

            task = new TestTask(odsxTaskService.getUniqueId(), System.currentTimeMillis()
                    ,testType, measurementList);
            String result = task.executeTask();
            response.put("response", result);

        }else{

            Calendar calScheduledTime = Calendar.getInstance();
            calScheduledTime.add(Calendar.MINUTE, executionTime);
            task=new TestTask(odsxTaskService.getUniqueId(), calScheduledTime.getTimeInMillis()
                    ,testType, measurementList);
            TaskQueue.setTask(task);
            logger.info("Task scheduled and will be executed at "+calScheduledTime.getTime());
            response.put("response", "scheduled");

        }
        odsxTaskService.add(task);

        return response;
    }

    
    @PostMapping(path = "/datasource/register", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String,String> registerDatasource(@RequestBody DataSourceRequestModel dataSourceRequestModel) {
        Map<String,String> response = new HashMap<>();
        try {
        	DataSource dataSource=new DataSource(
        			 dataSourceService.getAutoIncId()
        			,dataSourceRequestModel.getDataSourceName()
        			,dataSourceRequestModel.getDataSourceType()
                    ,dataSourceRequestModel.getDataSourceHostIp()
        			,dataSourceRequestModel.getDataSourcePort()
        			,dataSourceRequestModel.getUsername()
        			,dataSourceRequestModel.getPassword()
        			);
        	dataSource.setAuthenticationScheme(dataSourceRequestModel.getAuthenticationScheme());
        	dataSource.setIntegratedSecurity(dataSourceRequestModel.getIntegratedSecurity());
        	dataSource.setProperties(dataSourceRequestModel.getProperties());
        	dataSource.setStatus(ModelConstant.ACTIVE);
        	dataSourceService.add(dataSource);
            response.put("response", "DataSource added successfully");
            return response;

        } catch (Exception exe) {
            exe.printStackTrace();
            response.put("response", exe.getMessage());
            return response;
        }
    }

    @GetMapping("/datasource/list")
    public Map<String,String> getDatasourceList(){
        Map<String,String> response = new HashMap<>();
        List<DataSource> dataSourceList = dataSourceService.getActiveDataSources();
        logger.info("datasourceList size: "+dataSourceList.size());
        //Gson gson = new Gson();
        /*
        gson with new type descriptor

        GsonBuilder b = new GsonBuilder();
        b.registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY);
        Gson gson = b.create();
        String jsonTaskList = gson.toJson(dataSourceList);
         */
        //==========

        List<Properties> dataSourcePropList = new ArrayList<>();
        for(DataSource dataSource: dataSourceList) {
            Properties properties = new Properties();
            properties.put("id", dataSource.getId());
            properties.put("dataSourceName", dataSource.getDataSourceName());
            properties.put("dataSourceType", dataSource.getDataSourceType());
            properties.put("dataSourceHostIp",dataSource.getDataSourceHostIp());
            properties.put("dataSourcePort",dataSource.getDataSourcePort());
            properties.put("authenticationScheme",dataSource.getAuthenticationScheme());
            properties.put("username",dataSource.getUsername());
            properties.put("password",dataSource.getPassword());
            properties.put("integratedSecurity",dataSource.getIntegratedSecurity());
            properties.put("properties",dataSource.getProperties());
            properties.put("agentHostIp",dataSource.getAgent()!=null?dataSource.getAgent().getHostIp():"-1");
            dataSourcePropList.add(properties);
        }
        Gson gson = new Gson();
        String dataSourcePropListJson = gson.toJson(dataSourcePropList);
        response.put("response", dataSourcePropListJson);
        return response;
    }
    
    @PostMapping(path = "/datasource/update", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public Map<String, String> updateDatasource(@RequestBody DataSourceRequestModel dataSourceRequestModel) {
		Map<String, String> response = new HashMap<>();
		try {
			DataSource dataSource = dataSourceService.getDataSource(Long.parseLong(dataSourceRequestModel.getDataSourceId()));
			dataSource.setDataSourceName(dataSourceRequestModel.getDataSourceName());
			dataSource.setDataSourceType(dataSourceRequestModel.getDataSourceType());
			dataSource.setDataSourceHostIp(dataSourceRequestModel.getDataSourceHostIp());
			dataSource.setDataSourcePort(dataSourceRequestModel.getDataSourcePort());
			dataSource.setUsername(dataSourceRequestModel.getUsername());
			dataSource.setPassword(dataSourceRequestModel.getPassword());
			dataSource.setAuthenticationScheme(dataSourceRequestModel.getAuthenticationScheme());
        	dataSource.setIntegratedSecurity(dataSourceRequestModel.getIntegratedSecurity());
        	dataSource.setProperties(dataSourceRequestModel.getProperties());
			dataSourceService.update(dataSource);
			response.put("response","Datasource with id '" + dataSourceRequestModel.getDataSourceId() + "' updated successfully");
		} catch (Exception exe) {
			exe.printStackTrace();
			response.put("response", exe.getMessage());
			return response;
		}
		return response;
	}
    
    @DeleteMapping("/datasource/remove/{datasourceId}")
    public Map<String,String> removeDataSource(@PathVariable String datasourceId
            , @RequestParam(defaultValue="0") int executionTime
    ) {
        Map<String,String> response = new HashMap<>();
        try {
        	DataSource dataSource = dataSourceService.getDataSource(Long.parseLong(datasourceId));
        	dataSource.setStatus(ModelConstant.DELETED);
        	dataSourceService.update(dataSource);
            response.put("response", "DataSource with id '"+datasourceId+"' is removed");
            return response;
        } catch (Exception exe) {
            exe.printStackTrace();
            response.put("response", exe.getMessage());
            return response;
        }
    }
    @PostMapping(path = "/assignment/add/{agentId}/{dataSourceIds}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String,String> assignAgents(@PathVariable String dataSourceIds
            ,@PathVariable String agentId) {
        Map<String,String> response = new HashMap<>();
        try {
            String dataSourceIdArr[] = dataSourceIds.split(",");
            Agent agent=agentService.getById(Long.parseLong(agentId));
            DataSource dataSource=null;
            for(String dataSourceIdStr: dataSourceIdArr) {
                dataSource=dataSourceService.getById(Long.parseLong(dataSourceIdStr));
                if(dataSource != null)agent.getDataSources().add(dataSource);
                dataSource.setAgent(agent);
                dataSourceService.update(dataSource);
            }
            agentService.update(agent);
            response.put("response", "Agent and Data Source(s) assigned successfully");

        } catch (Exception exe) {
            exe.printStackTrace();
            response.put("response", exe.getMessage());
        }
        return response;
    }
    @PostMapping(path = "/assignment/update/{agentId}/{dataSourceIds}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String,String> updateAssignment(@PathVariable String dataSourceIds
            ,@PathVariable String agentId) {
        Map<String,String> response = new HashMap<>();
        try {
            String dataSourceIdArr[] = dataSourceIds.split(",");
            Agent agent=agentService.getById(Long.parseLong(agentId));
            // 1. Remove existing assignments
            for (DataSource ds:agent.getDataSources()){
                ds.setAgent(null);
                dataSourceService.update(ds);
            }
            agent.getDataSources().clear();
            agentService.update(agent);

            // 2. Create new assignments
            DataSource dataSource=null;
            for(String dataSourceIdStr: dataSourceIdArr) {
                dataSource=dataSourceService.getById(Long.parseLong(dataSourceIdStr));
                if(dataSource != null)agent.getDataSources().add(dataSource);
                dataSource.setAgent(agent);
                dataSourceService.update(dataSource);
            }
            agentService.update(agent);
            response.put("response", "Agent and Data Source(s) assigned successfully");

        } catch (Exception exe) {
            exe.printStackTrace();
            response.put("response", exe.getMessage());
        }
        return response;
    }
    @DeleteMapping("/assignment/remove/{agentId}")
    public Map<String,String> removeAssignment(@PathVariable String agentId
            , @RequestParam(defaultValue="0") int executionTime
    ) {
        Map<String,String> response = new HashMap<>();
        try {
            Agent agent=agentService.getById(Long.parseLong(agentId));
            for(DataSource ds: agent.getDataSources()){
                ds.setAgent(null);
                dataSourceService.update(ds);
            }
            agent.getDataSources().clear();
            agentService.update(agent);
            response.put("response", "Agent and Data Source assignment removed for agent id '"+agentId+"'");
            return response;
        } catch (Exception exe) {
            exe.printStackTrace();
            response.put("response", exe.getMessage());
            return response;
        }
    }
    @GetMapping("/assignment/list")
    public Map<String,String> getAssignmentList(){
        Map<String,String> response = new HashMap<>();
        List<Properties> agentDSList = new ArrayList<>();
        List<Agent> agentList=agentService.getAll();
        for (Agent agent: agentList) {
            for (DataSource dataSource: agent.getDataSources()) {
                Properties properties = new Properties();
                properties.put("agentId",agent.getId());
                properties.put("agentHostIp",agent.getHostIp());
                properties.put("dataSourceId",dataSource.getId());
                properties.put("dataSourceName",dataSource.getDataSourceName());
                properties.put("dataSourceType",dataSource.getDataSourceType());
                properties.put("dataSourceHostIp",dataSource.getDataSourceHostIp());
                agentDSList.add(properties);
            }
        }
        Gson gson = new Gson();
        String agentWiseDSStr = gson.toJson(agentDSList);
        response.put("response", agentWiseDSStr);
        return response;
    }
    @PostMapping(path = "/agent/register", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String,String> registerAgent(@RequestBody AgentRequestModel agentRequestModel) {
        Map<String,String> response = new HashMap<>();
        try {
            Agent agent = new Agent(agentService.getAutoIncId()
                    ,agentRequestModel.getAgentHostIp()
                    ,agentRequestModel.getAgentUser());
            agentService.add(agent);
            response.put("response", "Agent added successfully");
            return response;

        } catch (Exception exe) {
            exe.printStackTrace();
            response.put("response", exe.getMessage());
            return response;
        }
    }
    @GetMapping("/agent/list")
    public Map<String,String> getAgentList(){
        Map<String,String> response = new HashMap<>();
        List<Agent> agentList = agentService.getActiveAgents();
        logger.info("agentList size: "+agentList.size());
        //Gson gson = new Gson();
        //String jsonTaskList = gson.toJson(agentList);
        String jsonTaskList="";
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setVisibility(JsonMethod.FIELD, JsonAutoDetect.Visibility.ANY);
            objectMapper.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false);
            jsonTaskList = objectMapper.writeValueAsString(agentList);
        } catch (IOException e) {
            e.printStackTrace();
        }

        response.put("response", jsonTaskList);
        return response;
    }
    /*@GetMapping("/agentdatasource/list")
    public Map<String,String> getAgentAndDataSourceList(){
        Map<String,String> response = new HashMap<>();
        List<DataSourceAgentAssignment> dataSourceAgentAssignmentsList = dataSourceAgentAssignmentService.getActiveDataSourceAgents();
        logger.info("dataSourceAgentAssignmentsList size: "+dataSourceAgentAssignmentsList.size());
        Map<Agent,List<DataSource>> agentWiseDS = new HashMap<>();
        List<Properties> agentDSList = new ArrayList<>();
        Agent agent =null;
        DataSource dataSource=null;
        for(DataSourceAgentAssignment d: dataSourceAgentAssignmentsList){
            agent = agentService.getById(d.getAgentId());
            dataSource = dataSourceService.getById(d.getDataSourceId());
            if(agent == null){
                response.put("response", "Agent with id '"+d.getAgentId()+"' not available");
                return response;
            }
            if( dataSource == null){
                response.put("response", "DataSource with id '"+d.getDataSourceId()+"' not available");
                return response;
            }
            if(agent !=null && !agentWiseDS.containsKey(agent)){
                agentWiseDS.put(agent,new ArrayList<>());
            }
            Properties properties = new Properties();
            properties.put("agentId",agent.getId());
            properties.put("agentHostIp",agent.getHostIp());
            properties.put("dataSourceId",dataSource.getId());
            properties.put("dataSourceName",dataSource.getDataSourceName());
            properties.put("dataSourceType",dataSource.getDataSourceType());
            agentDSList.add(properties);
            agentWiseDS.get(agent).add(dataSource);
        }

        Gson gson = new Gson();
        String agentWiseDSStr = gson.toJson(agentDSList);
        response.put("response", agentWiseDSStr);
        return response;
    }*/
}
