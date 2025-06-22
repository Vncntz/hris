package com.vincent.hris.modules.userandroles.window;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vincent.hris.modules.userandroles.model.Modules;
import com.vincent.hris.modules.userandroles.service.ModuleService;
import com.vincent.hris.util.NotificationUtil;

public class AddEditModuleDialog extends Dialog {

	private static final long serialVersionUID = 1L;

	private ModuleService moduleService;
	private Modules modules;
	private Binder<Modules> binder = new Binder<>(Modules.class);
	private TextField nameField = new TextField("Module Name");
	private TextField descriptionField = new TextField("Description");
	private Checkbox activeField = new Checkbox("Active");
	private TextField iconField = new TextField("Icon");
	private IntegerField sequenceField = new IntegerField("Sequence");

	public AddEditModuleDialog(ModuleService moduleService, Modules module) {
		this.moduleService = moduleService;
		this.modules = module != null ? module : new Modules();

		setWidth("500px");
		setHeaderTitle(module != null ? "Edit Module" : "Add Module");

		FormLayout formLayout = new FormLayout();
		formLayout.add(nameField, descriptionField, activeField, iconField, sequenceField);

		binder.forField(nameField).asRequired("Module name is required").bind("name");
		binder.forField(descriptionField).bind("description");
		binder.forField(activeField).bind("active");
		binder.forField(iconField).bind("icon");
		binder.forField(sequenceField).asRequired("Sequence is required").bind("sequence");

		binder.readBean(this.modules);

		Button saveButton = new Button("Save", e -> save());
		saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

		Button cancelButton = new Button("Cancel", e -> close());

		HorizontalLayout actions = new HorizontalLayout(saveButton, cancelButton);
		add(formLayout);
		getFooter().add(actions);
	}

	private void save() {
		try {
			binder.writeBean(modules);
			moduleService.save(modules);
			NotificationUtil.success("Module saved: " + modules.getName());
			close();
		} catch (ValidationException e) {
			NotificationUtil.error("Please correct the errors.");
		}
	}
}
