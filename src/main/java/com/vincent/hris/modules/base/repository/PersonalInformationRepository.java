package com.vincent.hris.modules.base.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vincent.hris.modules.base.model.Employee;
import com.vincent.hris.modules.base.model.PersonalInformation;

public interface PersonalInformationRepository extends JpaRepository<PersonalInformation, Long> {
	PersonalInformation findByEmployee(Employee employee);

	PersonalInformation findByEmployeeId(Long employeeId);
}
