package com.vincent.hris.modules.userandroles.view;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vincent.hris.modules.base.view.BaseGridView;
import com.vincent.hris.modules.userandroles.model.User;
import com.vincent.hris.modules.userandroles.service.RoleService;
import com.vincent.hris.modules.userandroles.service.UserService;
import com.vincent.hris.modules.userandroles.window.AddEditUserDialog;
import com.vincent.hris.util.NotificationUtil;

import jakarta.annotation.PostConstruct;

@PageTitle("Users")
@Route("configure-user")
public class UserListView extends BaseGridView<User> {

	private static final long serialVersionUID = 1L;

	@Autowired
	private UserService userService;
	@Autowired
	private RoleService roleService;

	public UserListView() {
		super();
		configureGrid();
	}

	@Override
	protected void configureGrid() {
		grid.addColumn(User::getUsername).setHeader("Username").setAutoWidth(true);
		grid.addColumn(user -> user.getRole() != null ? user.getRole().getName() : "").setHeader("Role")
				.setAutoWidth(true);
		grid.addComponentColumn(user -> {
			Button viewBtn = new Button("View");
			viewBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
			viewBtn.addClickListener(e -> onView(user));
			return viewBtn;
		}).setHeader("Actions").setWidth("80px").setFlexGrow(0);
	}

	@Override
	protected void onAdd() {
		AddEditUserDialog dialog = new AddEditUserDialog(userService, roleService, null);
		dialog.open();
	}

	@Override
	protected void onEdit(User user) {
		if (user == null) {
			NotificationUtil.error("Please select a user to edit.");
			return;
		}
		AddEditUserDialog dialog = new AddEditUserDialog(userService, roleService, user);
		dialog.open();
	}

	@Override
	protected void onDelete(User user) {
		if (user == null) {
			NotificationUtil.error("Please select a user to delete.");
			return;
		}
		userService.delete(user);
		NotificationUtil.success("Deleted User: " + user.getUsername());
		refreshGrid();
	}

	private void onView(User user) {
		if (user == null) {
			NotificationUtil.error("Please select a user to view.");
			return;
		}
		NotificationUtil.info("View User: " + user.getUsername());
	}

	private void refreshGrid() {
		List<User> users = userService.findAll();
		grid.setItems(users);
	}

	@PostConstruct
	private void onLoad() {
		refreshGrid();
	}
}
