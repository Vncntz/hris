package com.vincent.hris.modules.userandroles.window;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vincent.hris.modules.userandroles.model.Modules;
import com.vincent.hris.modules.userandroles.model.SubMenu;
import com.vincent.hris.modules.userandroles.service.ModuleService;
import com.vincent.hris.modules.userandroles.service.SubMenuService;
import com.vincent.hris.util.NotificationUtil;

public class AddEditSubMenuDialog extends Dialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final SubMenuService service;
	private final Binder<SubMenu> binder = new Binder<>(SubMenu.class);
	private final SubMenu subMenu;
	private final TextField name = new TextField("Name");
	private ComboBox<Modules> modules = new ComboBox<>("Modules");
	private TextField description = new TextField("Description");
	private TextField route = new TextField("Route");
	private TextField icon = new TextField("Icon");
	private Checkbox active = new Checkbox("Active");
	private IntegerField sequence = new IntegerField("Sequence");

	public AddEditSubMenuDialog(SubMenuService service, SubMenu subMenu, ModuleService moduleService) {
		this.service = service;
		this.subMenu = subMenu != null ? subMenu : new SubMenu();

		setHeaderTitle(subMenu != null ? "Edit SubMenu" : "Add SubMenu");
		setWidth("500px");

		modules.setItems(moduleService.findAll());
		modules.setItemLabelGenerator(Modules::getName);

		FormLayout form = new FormLayout(name, modules, description, route, sequence, icon, active);

		binder.forField(name).asRequired("Submenu name is required").bind(SubMenu::getName, SubMenu::setName);
		binder.forField(modules).asRequired("Module is required").bind(SubMenu::getModule, SubMenu::setModule);
		binder.forField(description).asRequired("Description is required").bind(SubMenu::getDescription,
				SubMenu::setDescription);
		binder.forField(route).asRequired("Route is required").bind(SubMenu::getRoute, SubMenu::setRoute);
		binder.forField(icon).bind(SubMenu::getIcon, SubMenu::setIcon);
		binder.forField(active).bind(SubMenu::isActive, SubMenu::setActive);
		binder.forField(sequence).asRequired("Sequence is required").bind(SubMenu::getSequence, SubMenu::setSequence);

		binder.readBean(this.subMenu);

		Button saveBtn = new Button("Save", e -> save());
		saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

		Button cancelBtn = new Button("Cancel", e -> close());

		getFooter().add(new HorizontalLayout(saveBtn, cancelBtn));
		add(form);
	}

	private void save() {
		try {
			binder.writeBean(subMenu);
			service.save(subMenu);
			NotificationUtil.success("Saved SubMenu: " + subMenu.getName());
			close();
		} catch (ValidationException e) {
			NotificationUtil.error("Please correct errors.");
		}
	}
}
