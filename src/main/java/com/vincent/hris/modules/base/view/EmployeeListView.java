package com.vincent.hris.modules.base.view;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vincent.hris.modules.base.model.Employee;
import com.vincent.hris.modules.base.service.EmployeeService;
import com.vincent.hris.modules.base.window.AddEmployeeDialog;
import com.vincent.hris.util.NotificationUtil;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@PageTitle("Employee List")
@Route("employee")
@Slf4j
public class EmployeeListView extends BaseGridView<Employee> {

	private static final long serialVersionUID = 1L;

	@Autowired
	private EmployeeService employeeService;

	public EmployeeListView() {
		super();
		configureGrid(); // ✅ Setup grid here
	}

	@Override
	protected void configureGrid() {
		grid.addColumn(Employee::getFullName).setHeader("Name").setResizable(true);
		grid.addColumn(Employee::getEmail).setHeader("Email").setWidth("350px").setFlexGrow(0).setResizable(true);
		grid.addColumn(Employee::getPhone).setHeader("Phone").setWidth("130px").setFlexGrow(0).setResizable(true);
		grid.addComponentColumn(employee -> {
			Button viewBtn = new Button("View", VaadinIcon.EYE.create());
			viewBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
			viewBtn.addClickListener(e -> onView(employee));
			return viewBtn;
		}).setHeader("Action").setWidth("120px").setFlexGrow(0);

	}

	@Override
	protected void onAdd() {
		AddEmployeeDialog dialog = new AddEmployeeDialog(employeeService);
		dialog.addDetachListener(e -> onLoad()); 
		dialog.open();

	}

	@Override
	protected void onEdit(Employee employee) {
		AddEmployeeDialog dialog = new AddEmployeeDialog(employeeService, employee);
		dialog.addDetachListener(e -> onLoad()); 
		dialog.open();
	}

	@Override
	protected void onDelete(Employee employee) {
		NotificationUtil.info("Delete functionality to be implemented for: " + employee.getFullName());
	}

	private void onView(Employee employee) {
		if (employee == null) {
			NotificationUtil.error("Please select an item to view.");
			return;
		}
		NotificationUtil.info("View: " + employee.getFullName());
	}

	@PostConstruct
	private void onLoad() {
		List<Employee> employees = employeeService.findAll();
		log.info("employees: " + employees);
		grid.setItems(employees);
	}
}
