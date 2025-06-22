package com.vincent.hris.modules.base.view.tabs;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.select.Select;

public class EmployeeShippingTab extends Div {

	private static final long serialVersionUID = 1L;

	private TextField addressLine1Field;
	private TextField addressLine2Field;
	private TextField cityField;
	private TextField stateField;
	private TextField zipCodeField;
	private Select<String> countrySelect;
	private TextField emergencyContactField;
	private TextField emergencyPhoneField;
	private Button saveButton;

	public EmployeeShippingTab() {
		initializeComponents();
//		setupLayout();
	}

	private void initializeComponents() {
		H3 title = new H3("Contact & Shipping Information");

		addressLine1Field = new TextField("Address Line 1");
		addressLine2Field = new TextField("Address Line 2");
		cityField = new TextField("City");
		stateField = new TextField("State/Province");
		zipCodeField = new TextField("ZIP/Postal Code");

		countrySelect = new Select<>();
		countrySelect.setLabel("Country");
		countrySelect.setItems("United States", "Canada", "United Kingdom", "Australia", "Germany", "France");
		countrySelect.setValue("United States");

		emergencyContactField = new TextField("Emergency Contact Name");
		emergencyPhoneField = new TextField("Emergency Contact Phone");

		saveButton = new Button("Save Contact Info");
		saveButton.addClickListener(e -> saveContactInfo());

		FormLayout formLayout = new FormLayout();
		formLayout.add(addressLine1Field, addressLine2Field, cityField, stateField, zipCodeField, countrySelect,
				emergencyContactField, emergencyPhoneField);
		formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("500px", 2));

		add(title, formLayout, saveButton);
	}

//	private void setupLayout() {
//		setPadding(true);
//		setSpacing(true);
//	}

	public void loadEmployeeData(Long employeeId) {
		// TODO: Implement actual data loading from service/repository
		// For now, just placeholder data
		addressLine1Field.setValue("123 Main Street");
		addressLine2Field.setValue("Apt 4B");
		cityField.setValue("New York");
		stateField.setValue("NY");
		zipCodeField.setValue("10001");
		countrySelect.setValue("United States");
		emergencyContactField.setValue("Jane Doe");
		emergencyPhoneField.setValue("+1987654321");
	}

	private void saveContactInfo() {
		// TODO: Implement save logic
		// NotificationUtil.success("Contact information saved successfully");
	}
}