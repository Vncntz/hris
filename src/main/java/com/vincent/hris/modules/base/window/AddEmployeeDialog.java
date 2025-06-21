package com.vincent.hris.modules.base.window;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vincent.hris.modules.base.model.Employee;
import com.vincent.hris.modules.base.service.EmployeeService;
import com.vincent.hris.util.NotificationUtil;

public class AddEmployeeDialog extends Dialog {

	private static final long serialVersionUID = 1L;

	private final FormLayout layoutForm;

	private final TextField fieldFirstName = new TextField("First Name");
	private final TextField fieldMiddleName = new TextField("Middle Name");
	private final TextField fieldLastName = new TextField("Last Name");

	private final EmailField emailField = new EmailField("Email");
	private final TextField phoneField = new TextField("Phone");
	private final TextArea addressField = new TextArea("Address");

	private final Button saveButton = new Button("Save");
	private final Button cancelButton = new Button("Cancel");

	private final Binder<Employee> binder = new Binder<>(Employee.class);
	private Employee employee;

	private final EmployeeService employeeService;

	public AddEmployeeDialog(EmployeeService employeeService) {
		this(employeeService, new Employee());
	}

	public AddEmployeeDialog(EmployeeService employeeService, Employee employee) {
		this.employeeService = employeeService;
		this.employee = employee;

		setWidth("800px");
		setHeaderTitle(employee.getId() == null ? "Add New Employee" : "Edit Employee");

		layoutForm = new FormLayout();
		layoutForm.setSizeFull();
		layoutForm.add(fieldFirstName, fieldMiddleName, fieldLastName, emailField, phoneField, addressField);

		layoutForm.setResponsiveSteps(
			new FormLayout.ResponsiveStep("0", 1),
			new FormLayout.ResponsiveStep("400px", 2),
			new FormLayout.ResponsiveStep("600px", 3)
		);
		layoutForm.setColspan(addressField, 3);

		binder.forField(fieldFirstName).asRequired("First name is required").bind("firstName");
		binder.forField(fieldMiddleName).bind("middleName");
		binder.forField(fieldLastName).asRequired("Last name is required").bind("lastName");
		binder.forField(emailField).withValidator(new EmailValidator("Enter a valid email address")).bind("email");
		binder.forField(phoneField).bind("phone");
		binder.forField(addressField).bind("address");

		// ✅ Pre-populate fields for editing
		binder.readBean(employee);

		saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		saveButton.addClickListener(e -> saveEmployee());

		cancelButton.addClickListener(e -> close());

		HorizontalLayout actions = new HorizontalLayout(saveButton, cancelButton);

		add(layoutForm);
		getFooter().add(actions);
	}

	private void saveEmployee() {
		try {
			binder.writeBean(employee);
			employeeService.save(employee);
			close();

			String msg = (employee.getId() == null ? "Added" : "Updated") + ": " + employee.getFirstName() + " " + employee.getLastName();
			NotificationUtil.success(msg);

		} catch (ValidationException e) {
			NotificationUtil.error("Please correct the errors.");
		}
	}
}
