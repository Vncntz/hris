package com.vincent.hris.modules.base.view;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vincent.hris.master.service.RefBrgyService;
import com.vincent.hris.master.service.RefCityMunicipalityService;
import com.vincent.hris.master.service.RefProvinceService;
import com.vincent.hris.modules.base.model.Employee;
import com.vincent.hris.modules.base.service.EmployeeService;
import com.vincent.hris.modules.base.view.tabs.PersonalInformationTab;
import com.vincent.hris.util.NotificationUtil;

import jakarta.annotation.PostConstruct;

@Route(value = "employee")
@PageTitle("Employee Management")
public class EmployeeView extends VerticalLayout implements HasUrlParameter<Long> {

	private static final long serialVersionUID = 1L;

	@Autowired
	private PersonalInformationTab personalInfoTab;

	private Div contentArea = new Div();
	@Autowired
	private EmployeeService employeeService;
	@Autowired
	private RefBrgyService refBrgyService;
	@Autowired
	private RefProvinceService refProvinceService;
	@Autowired
	private RefCityMunicipalityService refCityMunicipalityService;

	public EmployeeView() {
	}

	@PostConstruct
	public void init() {
		initializeLayout();
	}

	private void initializeLayout() {
		setSizeFull();

		Tab personalInfoTabComponent = new Tab("Personal Information");
		Tabs tabs = new Tabs(personalInfoTabComponent);

		tabs.addSelectedChangeListener(event -> showPersonalInfoTab());

		contentArea.setWidthFull();
		add(tabs, contentArea);

		showPersonalInfoTab();
	}

	private void showPersonalInfoTab() {
		contentArea.removeAll();
		contentArea.add(personalInfoTab);
	}

	@Override
	public void setParameter(BeforeEvent event, Long id) {
		if (id == null) {
			NotificationUtil.error("No employee ID provided.");
			return;
		}

		Employee employee = employeeService.findById(id);
		if (employee == null) {
			NotificationUtil.error("Employee not found with ID: " + id);
			return;
		}

		personalInfoTab.loadEmployeeData(employee);
		NotificationUtil.success("Employee ID: " + id);
	}

}
