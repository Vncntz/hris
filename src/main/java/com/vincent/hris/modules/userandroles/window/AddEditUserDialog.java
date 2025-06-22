package com.vincent.hris.modules.userandroles.window;

import java.util.List;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vincent.hris.modules.userandroles.model.Role;
import com.vincent.hris.modules.userandroles.model.User;
import com.vincent.hris.modules.userandroles.service.RoleService;
import com.vincent.hris.modules.userandroles.service.UserService;
import com.vincent.hris.util.NotificationUtil;

public class AddEditUserDialog extends Dialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7256158904503654184L;

	private final UserService userService;
	private final RoleService roleService;
	private User user;
	private final Binder<User> binder = new Binder<>(User.class);

	private final TextField usernameField = new TextField("Username");
	private final PasswordField passwordField = new PasswordField("Password");
	private final ComboBox<Role> roleComboBox = new ComboBox<>("Role");

	public AddEditUserDialog(UserService userService, RoleService roleService, User user) {
		this.userService = userService;
		this.roleService = roleService;
		this.user = user != null ? user : new User();

		setWidth("500px");
		setHeaderTitle(user != null ? "Edit User" : "Add User");

		FormLayout formLayout = new FormLayout();
		formLayout.add(usernameField, passwordField, roleComboBox);

		List<Role> roles = roleService.findAll();
		roleComboBox.setItems(roles);
		roleComboBox.setItemLabelGenerator(Role::getName);

		binder.forField(usernameField).asRequired("Username is required").bind("username");
		binder.forField(passwordField).asRequired("Password is required").bind("password");
		binder.forField(roleComboBox).asRequired("Role is required").bind("role");

		binder.readBean(this.user);

		Button saveButton = new Button("Save", e -> save());
		saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

		Button cancelButton = new Button("Cancel", e -> close());

		HorizontalLayout actions = new HorizontalLayout(saveButton, cancelButton);
		add(formLayout);
		getFooter().add(actions);
	}

	private void save() {
		try {
			binder.writeBean(user);
			userService.save(user);
			NotificationUtil.success("Saved user: " + user.getUsername());
			close();
		} catch (ValidationException e) {
			NotificationUtil.error("Please correct the errors.");
		}
	}
}
