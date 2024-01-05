package com.gigaspaces.datavalidator.TaskProcessor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.gigaspaces.datavalidator.db.service.TestTaskService;
import com.gigaspaces.datavalidator.model.TestTask;
import javax.annotation.PostConstruct;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

@Component
public class ExecutorBase {

	@Autowired
	TestTaskService testTaskService;
	@Autowired
	TaskWorker taskWorker;
	
	protected Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	@PostConstruct
	public void atStartup() {
		TaskQueue queue = new TaskQueue();
		CompleteTaskQueue completeTaskQueue = new CompleteTaskQueue();
		taskWorker.setNumThreads(5);
		Thread worker = new Thread(taskWorker);
		worker.start();
		logger.debug("###### Worker startup ok");
		initDb();

	}

	public void initDb() {
		logger.debug("Initialisation of Measurement and TestTask");
		for (TestTask testTask : testTaskService.getAllPendingTask()) {
			TaskQueue.setTask(testTask);
		}

		for (TestTask testTask : testTaskService.getAllCompletedTask()) {
			CompleteTaskQueue.setTask(testTask);
		}
	}

}
