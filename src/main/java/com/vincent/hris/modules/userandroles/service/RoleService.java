package com.vincent.hris.modules.userandroles.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.vincent.hris.modules.userandroles.model.Role;
import com.vincent.hris.modules.userandroles.repository.RoleRepository;

@Service
public class RoleService {

	private final RoleRepository roleRepository;

	public RoleService(RoleRepository roleRepository) {
		this.roleRepository = roleRepository;
	}

	public List<Role> findAll() {
		return roleRepository.findAll();
	}

	public Optional<Role> findById(Long id) {
		return roleRepository.findById(id);
	}

	public Role save(Role role) {
		return roleRepository.save(role);
	}

	public void delete(Long id) {
		roleRepository.deleteById(id);
	}

	public Role findByName(String name) {
		return roleRepository.findByName(name);
	}

	public void delete(Role role) {
		roleRepository.delete(role);
	}
}
