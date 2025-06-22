package com.vincent.hris.modules.userandroles.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.vincent.hris.modules.userandroles.model.Modules;
import com.vincent.hris.modules.userandroles.repository.ModuleRepository;

@Service
public class ModuleService {

	@Autowired
	private ModuleRepository moduleRepository;

	public List<Modules> findAll() {
		return moduleRepository.findAll();
	}

	public Modules save(Modules module) {
		return moduleRepository.save(module);
	}

	public void delete(Modules module) {
		moduleRepository.delete(module);
	}

	public boolean existsByName(String name) {
		return moduleRepository.existsByName(name);
	}
}
