package com.vincent.hris.modules.userandroles.window;

import java.util.Set;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vincent.hris.modules.userandroles.model.Access;
import com.vincent.hris.modules.userandroles.model.Modules;
import com.vincent.hris.modules.userandroles.model.Role;
import com.vincent.hris.modules.userandroles.service.AccessService;
import com.vincent.hris.modules.userandroles.service.ModuleService;
import com.vincent.hris.util.NotificationUtil;

public class AddEditAccessDialog extends Dialog {

	private static final long serialVersionUID = 1L;

	private final Grid<Access> grid = new Grid<>(Access.class, false);
	private final AccessService accessService;
	private final Role role;
	private final MultiSelectComboBox<Modules> addModule = new MultiSelectComboBox<>("Modules");

	public AddEditAccessDialog(AccessService accessService, ModuleService moduleService, Role role) {
		this.accessService = accessService;
		this.role = role;

		setWidth("900px");
		setHeaderTitle("Add Access for " + role.getName());

		addModule.setItems(moduleService.findAll());
		addModule.setItemLabelGenerator(Modules::getName);
		addModule.setPlaceholder("Select Module(s)");

		Button saveButton = new Button("Save", e -> save());
		saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

		Button cancelButton = new Button("Cancel", e -> close());

		HorizontalLayout actions = new HorizontalLayout(saveButton, cancelButton);
		add(addModule, actions);

		grid.setWidthFull();
		grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
		grid.addColumn(o -> o.getModule().getName()).setHeader("Module").setAutoWidth(true);
		grid.addColumn(o -> o.getModule().getDescription()).setHeader("Description").setAutoWidth(true);

		refreshGrid();
		add(grid);
	}

	private void save() {
		Set<Modules> selectedModules = addModule.getSelectedItems();

		for (Modules module : selectedModules) {
			if (!accessService.existsByRoleAndModule(role, module)) {
				Access access = new Access();
				access.setRole(role);
				access.setModule(module);
				accessService.save(access);
			}
		}

		NotificationUtil.success("Access updated for role: " + role.getName());
		refreshGrid();
		addModule.clear(); // Reset selection
	}

	private void refreshGrid() {
		grid.setItems(accessService.findByRole(role));
	}
}
