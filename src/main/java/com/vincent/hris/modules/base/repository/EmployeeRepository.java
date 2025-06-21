package com.vincent.hris.modules.base.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vincent.hris.modules.base.model.Employee;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
	// ✅ You can add custom queries here if needed
}
