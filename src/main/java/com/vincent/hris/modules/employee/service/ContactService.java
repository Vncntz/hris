package com.vincent.hris.modules.employee.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.vincent.hris.modules.base.model.Contact;
import com.vincent.hris.modules.base.model.Employee;
import com.vincent.hris.modules.base.repository.ContactRepository;

@Service
public class ContactService {

	private final ContactRepository repository;

	public ContactService(ContactRepository repository) {
		this.repository = repository;
	}

	public List<Contact> findAll() {
		return repository.findAll();
	}

	public Optional<Contact> findById(Long id) {
		return repository.findById(id);
	}

	public List<Contact> findByEmployee(Employee employee) {
		return repository.findByEmployee(employee);
	}

	public Contact save(Contact contact) {
		return repository.save(contact);
	}

	public void delete(Contact contact) {
		repository.delete(contact);
	}
}
