package com.gigaspaces.retentionmanager.dao;

import java.util.Date;
import java.util.List;

import com.gigaspaces.retentionmanager.model.ObjectRetentionPolicy;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

@Repository
public class ObjectRetentionPolicyDAOImpl implements ObjectRetentionPolicyDAO{

	@Autowired
	private SessionFactory sessionFactory;

	public void setSessionFactory(SessionFactory sf) {
		this.sessionFactory = sf;
	}

	@Override
	public List getByObjType(String objType) {
		Session  session= this.sessionFactory.getCurrentSession();
		CriteriaBuilder cb = session.getCriteriaBuilder();
		CriteriaQuery<Object> crtQ = cb.createQuery(Object.class);
		Root<ObjectRetentionPolicy> root = crtQ.from(ObjectRetentionPolicy.class);
		crtQ.select(root).where(cb.equal(root.get("objectType"), objType));;
		Query query = session.createQuery(crtQ);
		List list = query.getResultList();

		return list;
	}

	@Override
	public void deleteByObjType(String objType) {
		Session session = this.sessionFactory.getCurrentSession();
		List<ObjectRetentionPolicy> list = this.getByObjType(objType);
		for(ObjectRetentionPolicy objectRetentionPolicy: list) {
			if (objectRetentionPolicy != null) {
				session.delete(objectRetentionPolicy);
			}
		}
	}

	@Override
	public List executeQuery(String queryStr) {
		Session session = this.sessionFactory.getCurrentSession();
		List<ObjectRetentionPolicy>  list = session.createQuery(queryStr).list();
		return list;
	}

	@Override
	public void add(ObjectRetentionPolicy objectRetentionPolicy) {
		Session session = this.sessionFactory.getCurrentSession();
		session.save(objectRetentionPolicy);

	}

	@Override
	public List getAll() {
		Session session = this.sessionFactory.getCurrentSession();
		List<ObjectRetentionPolicy>  list = session.createQuery("from com.gigaspaces.retentionmanager.model.ObjectRetentionPolicy").list();
		return list;
	}

	@Override
	public ObjectRetentionPolicy getById(long id) {
		Session session = this.sessionFactory.getCurrentSession();
		ObjectRetentionPolicy objectRetentionPolicy = (ObjectRetentionPolicy) session.get(ObjectRetentionPolicy.class, id);
		return objectRetentionPolicy;
	}

	@Override
	public void update(ObjectRetentionPolicy objectRetentionPolicy) {
		Session session = this.sessionFactory.getCurrentSession();
		session.update(objectRetentionPolicy);
	}

	@Override
	public void delete(ObjectRetentionPolicy objectRetentionPolicy) {

		Session session = this.sessionFactory.getCurrentSession();
		session.delete(objectRetentionPolicy);
	}
}
