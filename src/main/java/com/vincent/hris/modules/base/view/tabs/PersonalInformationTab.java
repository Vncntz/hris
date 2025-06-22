package com.vincent.hris.modules.base.view.tabs;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vincent.hris.modules.base.model.Employee;
import com.vincent.hris.modules.base.model.PersonalInformation;
import com.vincent.hris.modules.base.service.PersonalInformationService;
import com.vincent.hris.util.NotificationUtil;

public class PersonalInformationTab extends VerticalLayout {

	private static final long serialVersionUID = 1L;

	private final PersonalInformationService personalInformationService;

	// Basic Information Fields
	private TextField employeeCodeField = new TextField("Employee Code");
	private Select<String> titleField = new Select<>();
	private TextField firstNameField = new TextField("First Name");
	private TextField middleNameField = new TextField("Middle Name");
	private TextField lastNameField = new TextField("Last Name");
	private TextField extField = new TextField("Extension");

	// Personal Details Fields
	private DatePicker birthDateField = new DatePicker("Birth Date");
	private TextField birthPlaceField = new TextField("Birth Place");
	private NumberField ageField = new NumberField("Age");
	private Select<String> genderField = new Select<>();
	private Select<String> civilStatusField = new Select<>();

	// Employment Information Fields
	private DatePicker dateHiredField = new DatePicker("Date Hired");
	private Checkbox isEmployeeActiveField = new Checkbox("Active Employee");
	private DatePicker contractExpiryField = new DatePicker("Contract Expiry");
	private DatePicker dateSeparatedField = new DatePicker("Date Separated");

	// Job Information Fields
	private TextField departmentField = new TextField("Department");
	private TextField positionField = new TextField("Position");
	private Select<String> employmentTypeField = new Select<>();
	private TextField divisionField = new TextField("Division");

	// Employment Status Fields
	private DatePicker probationaryDateField = new DatePicker("Probationary Date");
	private DatePicker regularizationDateField = new DatePicker("Regularization Date");

	// Additional Information Fields
	private TextField religionField = new TextField("Religion");
	private NumberField heightField = new NumberField("Height (cm)");
	private NumberField weightField = new NumberField("Weight (kg)");
	private TextField branchField = new TextField("Branch");

	private Button saveButton = new Button("Save Details");
	private Button clearButton = new Button("Clear");

	private PersonalInformation personalInformation;

	public PersonalInformationTab(PersonalInformationService personalInformationService) {
		this.personalInformationService = personalInformationService;

		initializeComponents();
		setupLayout();
	}

	private void initializeComponents() {
		setupSelectFields();
		setupNumberFields();

		// Create form sections
		FormLayout basicInfoLayout = createBasicInfoSection();

		FormLayout personalDetailsLayout = createPersonalDetailsSection();
		FormLayout employmentInfoLayout = createEmploymentInfoSection();
		FormLayout jobInfoLayout = createJobInfoSection();
		FormLayout additionalInfoLayout = createAdditionalInfoSection();

		// Setup button actions
		saveButton.addClickListener(e -> saveEmployeeDetails());
		clearButton.addClickListener(e -> clearFields());

		// Add all sections to the main layout
		add(basicInfoLayout, personalDetailsLayout, employmentInfoLayout, jobInfoLayout, additionalInfoLayout,
				createButtonLayout());
	}

	private void setupSelectFields() {
		// Title options
		titleField.setLabel("Title");
		titleField.setItems("Mr.", "Ms.", "Mrs.", "Dr.", "Engr.", "Atty.");
		titleField.setPlaceholder("Select title");

		// Gender options
		genderField.setLabel("Gender");
		genderField.setItems("Male", "Female");
		genderField.setPlaceholder("Select gender");

		// Civil Status options
		civilStatusField.setLabel("Civil Status");
		civilStatusField.setItems("Single", "Married", "Divorced", "Widowed", "Separated");
		civilStatusField.setPlaceholder("Select civil status");

		// Employment Type options
		employmentTypeField.setLabel("Employment Type");
		employmentTypeField.setItems("Full-time", "Part-time", "Contract", "Probationary", "Regular");
		employmentTypeField.setPlaceholder("Select employment type");
	}

	private void setupNumberFields() {
		ageField.setMin(18);
		ageField.setMax(100);
		ageField.setStep(1);

		heightField.setMin(100);
		heightField.setMax(250);
		heightField.setStep(0.1);

		weightField.setMin(30);
		weightField.setMax(200);
		weightField.setStep(0.1);
	}

	private FormLayout createBasicInfoSection() {
		FormLayout layout = new FormLayout();
		layout.setResponsiveSteps(new ResponsiveStep("0", 1), new ResponsiveStep("500px", 2),
				new ResponsiveStep("800px", 3), new ResponsiveStep("1100px", 4));
		layout.add(titleField, firstNameField, middleNameField, lastNameField, employeeCodeField, extField);
		return layout;
	}

	private FormLayout createPersonalDetailsSection() {
		FormLayout layout = new FormLayout();
		layout.setResponsiveSteps(new ResponsiveStep("0", 1), new ResponsiveStep("500px", 2),
				new ResponsiveStep("800px", 3), new ResponsiveStep("1100px", 4));
		layout.add(birthDateField, birthPlaceField, ageField, genderField, civilStatusField);
		return layout;
	}

	private FormLayout createEmploymentInfoSection() {
		FormLayout layout = new FormLayout();
		layout.add(dateHiredField, isEmployeeActiveField, contractExpiryField, dateSeparatedField,
				probationaryDateField, regularizationDateField);
		layout.setResponsiveSteps(new ResponsiveStep("0", 1), new ResponsiveStep("500px", 2),
				new ResponsiveStep("800px", 3), new ResponsiveStep("1100px", 4));
		return layout;
	}

	private FormLayout createJobInfoSection() {
		FormLayout layout = new FormLayout();
		layout.add(departmentField, positionField, employmentTypeField, divisionField, branchField);
		layout.setResponsiveSteps(new ResponsiveStep("0", 1), new ResponsiveStep("500px", 2),
				new ResponsiveStep("800px", 3), new ResponsiveStep("1100px", 4));
		return layout;
	}

	private FormLayout createAdditionalInfoSection() {
		FormLayout layout = new FormLayout();
		layout.add(religionField, heightField, weightField);
		layout.setResponsiveSteps(new ResponsiveStep("0", 1), new ResponsiveStep("500px", 2),
				new ResponsiveStep("800px", 3), new ResponsiveStep("1100px", 4));
		return layout;
	}

	private VerticalLayout createButtonLayout() {
		VerticalLayout buttonLayout = new VerticalLayout();
		buttonLayout.add(saveButton, clearButton);
		buttonLayout.setSpacing(true);
		return buttonLayout;
	}

	private void setupLayout() {
		setPadding(false);
		setSpacing(false);
		setWidthFull();
	}

	public void loadEmployeeData(Employee employee) {
		personalInformation = personalInformationService.findByEmployeeId(employee.getId());
		if (personalInformation == null) {
			personalInformation = new PersonalInformation();
			personalInformation.setEmployee(employee);
		}

		// Load basic information
		employeeCodeField
				.setValue(personalInformation.getEmployeeCode() != null ? personalInformation.getEmployeeCode() : "");
		titleField.setValue(personalInformation.getTitle());
		firstNameField.setValue(personalInformation.getFirstName() != null ? personalInformation.getFirstName() : "");
		middleNameField
				.setValue(personalInformation.getMiddleName() != null ? personalInformation.getMiddleName() : "");
		lastNameField.setValue(personalInformation.getLastName() != null ? personalInformation.getLastName() : "");
		extField.setValue(personalInformation.getExt() != null ? personalInformation.getExt() : "");

		// Load personal details
		birthDateField.setValue(personalInformation.getBirthDate());
		birthPlaceField
				.setValue(personalInformation.getBirthPlace() != null ? personalInformation.getBirthPlace() : "");
		ageField.setValue(personalInformation.getAge() != null ? personalInformation.getAge().doubleValue() : null);
		genderField.setValue(personalInformation.getGender());
		civilStatusField.setValue(personalInformation.getCivilStatus());

		// Load employment information
		dateHiredField.setValue(personalInformation.getDateHired());
		isEmployeeActiveField.setValue(personalInformation.isEmployeeActive());
		contractExpiryField.setValue(personalInformation.getContractExpiry());
		dateSeparatedField.setValue(personalInformation.getDateSeparated());
		probationaryDateField.setValue(personalInformation.getProbationaryDate());
		regularizationDateField.setValue(personalInformation.getRegularizationDate());

		// Load job information
		departmentField
				.setValue(personalInformation.getDepartment() != null ? personalInformation.getDepartment() : "");
		positionField.setValue(personalInformation.getPosition() != null ? personalInformation.getPosition() : "");
		employmentTypeField.setValue(personalInformation.getEmploymentType());
		divisionField.setValue(personalInformation.getDivision() != null ? personalInformation.getDivision() : "");
		branchField.setValue(personalInformation.getBranch() != null ? personalInformation.getBranch() : "");

		// Load additional information
		religionField.setValue(personalInformation.getReligion() != null ? personalInformation.getReligion() : "");
		heightField.setValue(personalInformation.getHeight() != null ? personalInformation.getHeight() : null);
		weightField.setValue(personalInformation.getWeight() != null ? personalInformation.getWeight() : null);

		NotificationUtil.success("Loaded employee: " + employee.getId());
	}

	private void saveEmployeeDetails() {
		try {
			// Save basic information
			personalInformation.setEmployeeCode(employeeCodeField.getValue());
			personalInformation.setTitle(titleField.getValue());
			personalInformation.setFirstName(firstNameField.getValue());
			personalInformation.setMiddleName(middleNameField.getValue());
			personalInformation.setLastName(lastNameField.getValue());
			personalInformation.setExt(extField.getValue());

			// Save personal details
			personalInformation.setBirthDate(birthDateField.getValue());
			personalInformation.setBirthPlace(birthPlaceField.getValue());
			personalInformation.setAge(ageField.getValue() != null ? ageField.getValue().intValue() : null);
			personalInformation.setGender(genderField.getValue());
			personalInformation.setCivilStatus(civilStatusField.getValue());

			// Save employment information
			personalInformation.setDateHired(dateHiredField.getValue());
			personalInformation.setEmployeeActive(isEmployeeActiveField.getValue());
			personalInformation.setContractExpiry(contractExpiryField.getValue());
			personalInformation.setDateSeparated(dateSeparatedField.getValue());
			personalInformation.setProbationaryDate(probationaryDateField.getValue());
			personalInformation.setRegularizationDate(regularizationDateField.getValue());

			// Save job information
			personalInformation.setDepartment(departmentField.getValue());
			personalInformation.setPosition(positionField.getValue());
			personalInformation.setEmploymentType(employmentTypeField.getValue());
			personalInformation.setDivision(divisionField.getValue());
			personalInformation.setBranch(branchField.getValue());

			// Save additional information
			personalInformation.setReligion(religionField.getValue());
			personalInformation.setHeight(heightField.getValue());
			personalInformation.setWeight(weightField.getValue());

			personalInformationService.save(personalInformation);

			NotificationUtil.success("Employee details saved successfully");
		} catch (Exception e) {
			NotificationUtil.error("Failed to save employee details: " + e.getMessage());
		}
	}

	private void clearFields() {
		// Clear basic information
		employeeCodeField.clear();
		titleField.clear();
		firstNameField.clear();
		middleNameField.clear();
		lastNameField.clear();
		extField.clear();

		// Clear personal details
		birthDateField.clear();
		birthPlaceField.clear();
		ageField.clear();
		genderField.clear();
		civilStatusField.clear();

		// Clear employment information
		dateHiredField.clear();
		isEmployeeActiveField.clear();
		contractExpiryField.clear();
		dateSeparatedField.clear();
		probationaryDateField.clear();
		regularizationDateField.clear();

		// Clear job information
		departmentField.clear();
		positionField.clear();
		employmentTypeField.clear();
		divisionField.clear();
		branchField.clear();

		// Clear additional information
		religionField.clear();
		heightField.clear();
		weightField.clear();

		NotificationUtil.success("All fields cleared");
	}
}