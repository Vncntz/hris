package com.vincent.hris.modules.base.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vincent.hris.modules.base.model.Contact;
import com.vincent.hris.modules.base.model.Employee;

public interface ContactRepository extends JpaRepository<Contact, Long> {
	List<Contact> findByEmployee(Employee employee);
}
