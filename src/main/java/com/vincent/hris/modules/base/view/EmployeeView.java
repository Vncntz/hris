package com.vincent.hris.modules.base.view;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vincent.hris.modules.base.model.Employee;
import com.vincent.hris.modules.base.service.EmployeeService;
import com.vincent.hris.modules.base.service.PersonalInformationService;
import com.vincent.hris.modules.base.view.tabs.PersonalInformationTab;
import com.vincent.hris.util.NotificationUtil;

@Route(value = "employee")
@PageTitle("Employee Management")
public class EmployeeView extends VerticalLayout implements HasUrlParameter<Long> {

	private static final long serialVersionUID = 1L;

	private PersonalInformationTab personalInfoTab;
	private Div contentArea = new Div();
	private EmployeeService employeeService;

	public EmployeeView(PersonalInformationService personalInformationService, EmployeeService employeeService) {
		this.employeeService = employeeService;
		this.personalInfoTab = new PersonalInformationTab(personalInformationService);

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
		Employee employee = employeeService.findById(id);
		NotificationUtil.success("Employee ID: " + id);
		personalInfoTab.loadEmployeeData(employee);
	}
}
