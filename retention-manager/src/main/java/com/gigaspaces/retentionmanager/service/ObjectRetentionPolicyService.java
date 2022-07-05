package com.gigaspaces.retentionmanager.service;

import com.gigaspaces.retentionmanager.dao.ObjectRetentionPolicyDAO;
import com.gigaspaces.retentionmanager.model.ObjectRetentionPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;

@Service
public class ObjectRetentionPolicyService {
	private static final Logger log = LoggerFactory.getLogger(ObjectRetentionPolicyService.class);
	@Autowired
	ObjectRetentionPolicyDAO objectRetentionPolicyDAO;

	@Transactional
	public List<ObjectRetentionPolicy> getAllRetentionPolicies() {
		return objectRetentionPolicyDAO.getAll();
	}

	@Transactional
	public ObjectRetentionPolicy getRetentionPolicy(int id) {
		return objectRetentionPolicyDAO.getById(id);
	}

	@Transactional
	public List getRetentionPolicy(String objType) {
		return objectRetentionPolicyDAO.getByObjType(objType);
	}

	@Transactional
	public void addRetentionPolicy(ObjectRetentionPolicy objectRetentionPolicy) {
		objectRetentionPolicyDAO.add(objectRetentionPolicy);
	}

	@Transactional
	public void updateRetentionPolicy(ObjectRetentionPolicy objectRetentionPolicy) {
		objectRetentionPolicyDAO.update(objectRetentionPolicy);

	}

	@Transactional
	public void deleteRetentionPolicy(ObjectRetentionPolicy objectRetentionPolicy) {
		objectRetentionPolicyDAO.delete(objectRetentionPolicy);
	}

	@Transactional
	public void deleteRetentionPolicy(String objType) {
		objectRetentionPolicyDAO.deleteByObjType(objType);
	}

}