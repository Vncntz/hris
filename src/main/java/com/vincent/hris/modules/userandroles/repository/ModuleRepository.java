package com.vincent.hris.modules.userandroles.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vincent.hris.modules.userandroles.model.Modules;

public interface ModuleRepository extends JpaRepository<Modules, Long> {
	boolean existsByName(String name);
}
