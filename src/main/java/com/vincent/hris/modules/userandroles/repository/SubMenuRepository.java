package com.vincent.hris.modules.userandroles.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vincent.hris.modules.userandroles.model.Modules;
import com.vincent.hris.modules.userandroles.model.SubMenu;

public interface SubMenuRepository extends JpaRepository<SubMenu, Long> {

	List<SubMenu> findByModule(Modules module);
}
