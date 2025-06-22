package com.vincent.hris.modules.userandroles.window;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vincent.hris.modules.userandroles.model.Role;
import com.vincent.hris.modules.userandroles.service.RoleService;
import com.vincent.hris.util.NotificationUtil;

public class AddEditRoleDialog extends Dialog {

	private static final long serialVersionUID = 1L;
	private final RoleService roleService;

	private Role role;
	private Binder<Role> binder = new Binder<>(Role.class);
	private TextField nameField = new TextField("Role Name");
	private TextField descriptionField = new TextField("Description");

	public AddEditRoleDialog(RoleService roleService, Role role) {
		this.roleService = roleService;
		this.role = role != null ? role : new Role();

		setWidth("500px");
		setHeaderTitle(role != null ? "Edit Role" : "Add Role");

		FormLayout formLayout = new FormLayout();
		formLayout.add(nameField, descriptionField);

		binder.forField(nameField).asRequired("Role name is required").bind("name");
		binder.forField(descriptionField).bind("description");
		binder.readBean(this.role);

		Button saveButton = new Button("Save", e -> save());
		saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

		Button cancelButton = new Button("Cancel", e -> close());

		HorizontalLayout actions = new HorizontalLayout(saveButton, cancelButton);
		add(formLayout);
		getFooter().add(actions);
	}

	private void save() {
		try {
			binder.writeBean(role);
			roleService.save(role);
			NotificationUtil.success("Saved role: " + role.getName());
			close();
		} catch (ValidationException e) {
			NotificationUtil.error("Please correct the errors.");
		}
	}
}
