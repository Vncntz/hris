package com.vincent.hris.modules.userandroles.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vincent.hris.modules.userandroles.model.Access;
import com.vincent.hris.modules.userandroles.model.Modules;
import com.vincent.hris.modules.userandroles.model.Role;
import com.vincent.hris.modules.userandroles.repository.AccessRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AccessService {

	private final AccessRepository repository;

	public List<Access> findAll() {
		return repository.findAll();
	}

	public Access save(Access access) {
		return repository.save(access);
	}

	public void delete(Access access) {
		repository.delete(access);
	}

	public Access findById(Long id) {
		return repository.findById(id).orElse(null);
	}

	public List<Access> findByRole(Role role) {
		return repository.findByRole(role);
	}

	public void deleteByRole(Role role) {
		repository.deleteByRole(role);
	}

	public List<Access> findByRoleAndModule(Role role, Modules module) {
		return repository.findByRoleAndModule(role, module);
	}

	public boolean existsByRoleAndModule(Role role, Modules module) {
		return !repository.findByRoleAndModule(role, module).isEmpty();
	}

}
