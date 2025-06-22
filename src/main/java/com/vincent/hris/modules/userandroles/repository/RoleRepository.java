package com.vincent.hris.modules.userandroles.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vincent.hris.modules.userandroles.model.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {

	// Example: find by role name
	Role findByName(String name);
}
