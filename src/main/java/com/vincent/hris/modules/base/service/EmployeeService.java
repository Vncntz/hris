package com.vincent.hris.modules.base.service;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.vincent.hris.modules.base.model.Employee;
import com.vincent.hris.modules.base.repository.EmployeeRepository;

@Service
public class EmployeeService extends BaseService<Employee, Long> {

	private final EmployeeRepository employeeRepository;

	public EmployeeService(EmployeeRepository employeeRepository) {
		this.employeeRepository = employeeRepository;
	}

	@Override
	protected JpaRepository<Employee, Long> getRepository() {
		return employeeRepository;
	}

	public List<Employee> findAll() {
		return employeeRepository.findAll();
	}

	public Employee save(Employee employee) {
		return employeeRepository.save(employee);

	}

	public void delete(Employee employee) {
		employeeRepository.delete(employee);
	}

	public Employee findById(long id) {
		return employeeRepository.findById(id);
	}

}
