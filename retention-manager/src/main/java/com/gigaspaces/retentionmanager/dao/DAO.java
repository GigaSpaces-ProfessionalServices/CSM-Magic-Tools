package com.gigaspaces.retentionmanager.dao;

import java.util.List;
import java.util.Optional;

public interface DAO<T> {
	
	void add(T t);

	List<T> getAll();

	T getById(long id);

	void update(T t);

	void delete(T t);

}
