/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.gigaspaces.datavalidator.db.service;

import java.util.List;
import java.util.logging.Logger;

import com.gigaspaces.datavalidator.model.Agent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.gigaspaces.datavalidator.db.dao.MeasurementDao;
import com.gigaspaces.datavalidator.model.DataSource;
import com.gigaspaces.datavalidator.model.Measurement;

/**
 *
 * @author alpesh
 */

@Service
public class MeasurementService {

	private static Logger logger = Logger.getLogger(MeasurementService.class.getName());

	@Autowired
	private MeasurementDao measurementDao;
	
	@Autowired
	private DataSourceService dataSourceService;

	@Autowired
	private DataSourceAgentAssignmentService dataSourceAgentAssignmentService;

	public void update(Measurement measurement) {
		measurementDao.update(measurement);	
	}
	
	public void add(Measurement measurement) {
		measurementDao.add(measurement);
	}
	public void deleteById(long measurementId) {
		measurementDao.deleteById(measurementId);
	}


	public long getAutoIncId() {
		return measurementDao.getAutoIncId("MEASUREMENT", "MEASUREMENT_ID");
	}

	public List<Measurement> getActiveMeasurement() {
		List<Measurement> measurementList = measurementDao.getActiveMeasurements();
		Long dataSourceId = 0l;
		for (Measurement measurement : measurementList) {
			dataSourceId = measurement.getDataSourceId();
			DataSource dataSource=dataSourceService.getDataSource(dataSourceId);
			//Agent agent = dataSource.getdataSourceAgentAssignmentService.getAgentByDataSource(dataSourceId);
			//dataSource.setAgent(agent);
			if(dataSource!=null) {
				measurement.setDataSource(dataSource);	
			}else {
				measurement.setDataSource(new DataSource());	
			}

			
		}
		return measurementList;
	}
	
	public List<Measurement> getAll() {
		List<Measurement> measurementList = measurementDao.getAll();
		Long dataSourceId = 0l;
		for (Measurement measurement : measurementList) {
			dataSourceId = measurement.getDataSourceId();
			DataSource dataSource=dataSourceService.getDataSource(dataSourceId);
			//Agent agent = dataSourceAgentAssignmentService.getAgentByDataSource(dataSourceId);
			//dataSource.setAgent(agent);
			if(dataSource!=null) {
				measurement.setDataSource(dataSource);	
			}else {
				measurement.setDataSource(new DataSource());	
			}
			
		}
		return measurementList;
	}

	public Measurement getMeasurement(long parseLong) {
		
		Long dataSourceId = 0l;
		Measurement measurement=measurementDao.getById(parseLong);
		if(measurement != null) {
			dataSourceId = measurement.getDataSourceId();
			logger.info("dataSourceId: " + dataSourceId);
			DataSource dataSource = dataSourceService.getDataSource(dataSourceId);
			logger.info("dataSource: " + dataSource);
			//Agent agent = dataSourceAgentAssignmentService.getAgentByDataSource(dataSourceId);
			logger.info("AAgent: " + dataSource.getAgent());
			//dataSource.setAgent(agent);
			measurement.setDataSource(dataSource);
		}
		return measurement;

	}

}
