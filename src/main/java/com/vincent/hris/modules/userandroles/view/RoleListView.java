package com.vincent.hris.modules.userandroles.view;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vincent.hris.modules.base.view.BaseGridView;
import com.vincent.hris.modules.userandroles.model.Role;
import com.vincent.hris.modules.userandroles.service.AccessService;
import com.vincent.hris.modules.userandroles.service.ModuleService;
import com.vincent.hris.modules.userandroles.service.RoleService;
import com.vincent.hris.modules.userandroles.window.AddEditAccessDialog;
import com.vincent.hris.modules.userandroles.window.AddEditRoleDialog;
import com.vincent.hris.util.NotificationUtil;

import jakarta.annotation.PostConstruct;

@PageTitle("Roles")
@Route("configure-role")
public class RoleListView extends BaseGridView<Role> {

	private static final long serialVersionUID = 1L;

	@Autowired
	private RoleService roleService;
	@Autowired
	private ModuleService moduleService;
	@Autowired
	private AccessService accessService;

	public RoleListView() {
		super();

		configureGrid();

	}

	@Override
	protected void configureGrid() {
		grid.addColumn(Role::getName).setHeader("Role Name").setAutoWidth(true);
		grid.addColumn(Role::getDescription).setHeader("Description").setAutoWidth(true);
		grid.addComponentColumn(role -> {
			Button buttonAddAccess = new Button("Add Access");
			buttonAddAccess.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
			buttonAddAccess.addClickListener(e -> {
				AddEditAccessDialog dialog = new AddEditAccessDialog(accessService, moduleService, role);
				dialog.open();
			});
			return buttonAddAccess;
		}).setHeader("Actions").setWidth("150px").setFlexGrow(0);

	}

	@Override
	protected void onAdd() {
		AddEditRoleDialog dialog = new AddEditRoleDialog(roleService, null);
		dialog.addDetachListener(e -> refreshGrid());
		dialog.open();
	}

	@Override
	protected void onEdit(Role role) {
		if (role == null) {
			NotificationUtil.error("Please select a role to edit.");
			return;
		}
		AddEditRoleDialog dialog = new AddEditRoleDialog(roleService, role);
		dialog.addDetachListener(e -> refreshGrid());
		dialog.open();
	}

	@Override
	protected void onDelete(Role role) {
		if (role == null) {
			NotificationUtil.error("Please select a role to delete.");
			return;
		}
		roleService.delete(role);
		NotificationUtil.success("Deleted Role: " + role.getName());
		refreshGrid();
	}

	private void refreshGrid() {
		List<Role> roles = roleService.findAll();
		grid.setItems(roles);
	}

	@PostConstruct
	private void onLoad() {
		refreshGrid();
	}
}
