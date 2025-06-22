package com.vincent.hris.modules.userandroles.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vincent.hris.modules.userandroles.model.Access;
import com.vincent.hris.modules.userandroles.model.Modules;
import com.vincent.hris.modules.userandroles.model.Role;

public interface AccessRepository extends JpaRepository<Access, Long> {

	List<Access> findByRole(Role role);

	void deleteByRole(Role role);

	List<Access> findByRoleAndModule(Role role, Modules module);
}
