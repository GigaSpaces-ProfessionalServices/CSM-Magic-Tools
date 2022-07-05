package com.gigaspaces.retentionmanager.dao;

import com.gigaspaces.retentionmanager.model.ObjectRetentionPolicy;

import java.util.Date;
import java.util.List;

public interface ObjectRetentionPolicyDAO extends DAO<ObjectRetentionPolicy>{

	public List getByObjType(String objType) ;

	public void deleteByObjType(String objType) ;

	public List executeQuery(String queryStr);
}
