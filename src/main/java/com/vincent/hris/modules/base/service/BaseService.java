package com.vincent.hris.modules.base.service;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vincent.hris.modules.base.model.Employee;

public class BaseService<T1, T2> {

	protected JpaRepository<Employee, Long> getRepository() {
		// TODO Auto-generated method stub
		return null;
	}

}
