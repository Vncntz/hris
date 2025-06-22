package com.vincent.hris.modules.userandroles.view;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vincent.hris.modules.base.view.BaseGridView;
import com.vincent.hris.modules.userandroles.model.SubMenu;
import com.vincent.hris.modules.userandroles.service.ModuleService;
import com.vincent.hris.modules.userandroles.service.SubMenuService;
import com.vincent.hris.modules.userandroles.window.AddEditSubMenuDialog;
import com.vincent.hris.util.NotificationUtil;

import jakarta.annotation.PostConstruct;

@PageTitle("Sub Menus")
@Route("configure-submenu")
public class SubMenuListView extends BaseGridView<SubMenu> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Autowired
	private SubMenuService service;
	@Autowired
	private ModuleService moduleService;

	public SubMenuListView() {
		super();
		configureGrid();
	}

	@Override
	protected void configureGrid() {
		grid.addColumn(SubMenu::getName).setHeader("Name").setWidth("200px").setFlexGrow(0);
		grid.addColumn(sub -> sub.getModule() != null ? sub.getModule().getName() : "").setHeader("Module")
				.setWidth("200px").setFlexGrow(0);
		grid.addColumn(SubMenu::getRoute).setHeader("Route").setWidth("200px").setFlexGrow(0);
		grid.addColumn(SubMenu::getIcon).setHeader("Icon").setWidth("100px").setFlexGrow(0);
		grid.addColumn(SubMenu::getSequence).setHeader("Order").setWidth("100px").setFlexGrow(0);
		grid.addColumn(SubMenu::isActive).setHeader("Active").setWidth("100px").setFlexGrow(0);
		grid.addColumn(SubMenu::getDescription).setHeader("Description");
	}

	@Override
	protected void onAdd() {
		new AddEditSubMenuDialog(service, null, moduleService).open();
	}

	@Override
	protected void onEdit(SubMenu subMenu) {
		new AddEditSubMenuDialog(service, subMenu, moduleService).open();
	}

	@Override
	protected void onDelete(SubMenu subMenu) {
		service.delete(subMenu);
		NotificationUtil.success("Deleted: " + subMenu.getName());
	}

	@PostConstruct
	private void onLoad() {
		grid.setItems(service.findAll());
	}
}
