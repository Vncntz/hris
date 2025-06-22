package com.vincent.hris.modules.base.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.vincent.hris.modules.base.model.Employee;
import com.vincent.hris.modules.base.model.PersonalInformation;
import com.vincent.hris.modules.base.repository.PersonalInformationRepository;

@Service
public class PersonalInformationService {

	private final PersonalInformationRepository repository;

	public PersonalInformationService(PersonalInformationRepository repository) {
		this.repository = repository;
	}

	public List<PersonalInformation> findAll() {
		return repository.findAll();
	}

	public Optional<PersonalInformation> findById(Long id) {
		return repository.findById(id);
	}

	public PersonalInformation findByEmployee(Employee employee) {
		return repository.findByEmployee(employee);
	}

	public PersonalInformation findByEmployeeId(Long employeeId) {
		return repository.findByEmployeeId(employeeId);
	}

//	public List<PersonalInformation> findByEmployee(Employee employee) {
//		return repository.findByEmployee(employee);
//	}

	public PersonalInformation save(PersonalInformation personalInformation) {
		return repository.save(personalInformation);
	}

	public void delete(PersonalInformation personalInformation) {
		repository.delete(personalInformation);
	}
}
