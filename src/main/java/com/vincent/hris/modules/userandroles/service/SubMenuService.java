package com.vincent.hris.modules.userandroles.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.vincent.hris.modules.userandroles.model.Modules;
import com.vincent.hris.modules.userandroles.model.SubMenu;
import com.vincent.hris.modules.userandroles.repository.SubMenuRepository;

@Service
public class SubMenuService {

	private final SubMenuRepository repository;

	public SubMenuService(SubMenuRepository repository) {
		this.repository = repository;
	}

	public List<SubMenu> findAll() {
		return repository.findAll();
	}

	public SubMenu save(SubMenu subMenu) {
		return repository.save(subMenu);
	}

	public void delete(SubMenu subMenu) {
		repository.delete(subMenu);
	}

	public List<SubMenu> findByModule(Modules module) {
		return repository.findByModule(module);
	}
}
