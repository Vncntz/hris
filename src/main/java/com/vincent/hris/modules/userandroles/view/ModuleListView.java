package com.vincent.hris.modules.userandroles.view;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vincent.hris.modules.base.view.BaseGridView;
import com.vincent.hris.modules.userandroles.model.Modules;
import com.vincent.hris.modules.userandroles.service.ModuleService;
import com.vincent.hris.modules.userandroles.window.AddEditModuleDialog;
import com.vincent.hris.util.NotificationUtil;

import jakarta.annotation.PostConstruct;

@PageTitle("Modules")
@Route("configure-module")
public class ModuleListView extends BaseGridView<Modules> {

	private static final long serialVersionUID = 1L;

	@Autowired
	private ModuleService moduleService;

	public ModuleListView() {
		super();
		configureGrid();
	}

	@Override
	protected void configureGrid() {
		grid.addColumn(Modules::getId).setHeader("Id").setWidth("80px").setFlexGrow(0)
				.setTextAlign(ColumnTextAlign.CENTER);
		grid.addColumn(Modules::getName).setHeader("Module Name").setAutoWidth(true);
		grid.addColumn(Modules::getDescription).setHeader("Description").setAutoWidth(true);
		grid.addColumn(module -> module.isActive() ? "✔️" : "❌").setHeader("Active").setWidth("80px").setFlexGrow(0)
				.setTextAlign(ColumnTextAlign.CENTER);
		grid.addColumn(Modules::getSequence).setHeader("Order").setWidth("80px").setFlexGrow(0)
				.setTextAlign(ColumnTextAlign.CENTER);
	}

	@Override
	protected void onAdd() {
		AddEditModuleDialog dialog = new AddEditModuleDialog(moduleService, null);
		dialog.open();
		dialog.addOpenedChangeListener(e -> refreshGrid());
	}

	@Override
	protected void onEdit(Modules module) {
		AddEditModuleDialog dialog = new AddEditModuleDialog(moduleService, module);
		dialog.open();
		dialog.addOpenedChangeListener(e -> refreshGrid());
	}

	protected void onDelete(Modules module) {
		moduleService.delete(module);
		NotificationUtil.success("Deleted module: " + module.getName());
		refreshGrid();
	}

	@PostConstruct
	private void refreshGrid() {
		List<Modules> modules = moduleService.findAll();
		grid.setItems(modules);
	}
}
