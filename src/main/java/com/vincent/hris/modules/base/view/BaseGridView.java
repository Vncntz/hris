package com.vincent.hris.modules.base.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vincent.hris.util.NotificationUtil;

public abstract class BaseGridView<T> extends VerticalLayout {

	private static final long serialVersionUID = 1L;

	protected VerticalLayout layoutRoot;

	protected HorizontalLayout layoutFilter;
	protected TextField fieldSearch;
	protected Button buttonSearch;

	protected HorizontalLayout layoutAction;
	protected Button buttonAdd;
	protected Button buttonEdit;
	protected Button buttonDelete;

	protected Grid<T> grid;

	public BaseGridView() {
		setSizeFull();
		setPadding(false);
		setSpacing(false);

		layoutRoot = new VerticalLayout();
		layoutRoot.setWidthFull();

		// ✅ Search Filter Layout
		layoutFilter = new HorizontalLayout();
		fieldSearch = new TextField();
		fieldSearch.setWidthFull();
		fieldSearch.setPlaceholder("Search...");
		buttonSearch = new Button("Search", VaadinIcon.SEARCH.create());
		buttonSearch.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		layoutFilter.add(fieldSearch, buttonSearch);

		// ✅ Grid Table
		grid = new Grid<>();
		grid.setSizeFull();
		grid.addThemeVariants(GridVariant.LUMO_COLUMN_BORDERS, GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);

		// ✅ Action Buttons
		layoutAction = new HorizontalLayout();
		buttonAdd = new Button("Add", VaadinIcon.PLUS.create(), e -> onAdd());
		buttonAdd.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

		buttonEdit = new Button("Edit", VaadinIcon.EDIT.create(), e -> handleEdit());
		buttonEdit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

		buttonDelete = new Button("Delete", VaadinIcon.TRASH.create(), e -> handleDelete());
		buttonDelete.addThemeVariants(ButtonVariant.LUMO_ERROR);
		buttonDelete.addThemeName("error primary");

		layoutAction.add(buttonAdd, buttonEdit, buttonDelete);

		layoutRoot.add(layoutFilter);
		layoutRoot.addAndExpand(grid);
		layoutRoot.add(layoutAction);

		add(layoutRoot);
	}

	private void handleEdit() {
		if (grid.getSelectedItems().isEmpty()) {
			NotificationUtil.error("Please select an item to edit.");
			return;
		}
		onEdit(grid.asSingleSelect().getValue());
	}

	private void handleDelete() {
		if (grid.getSelectedItems().isEmpty()) {
			NotificationUtil.error("Please select an item to delete.");
			return;
		}

		ConfirmDialog confirmDialog = new ConfirmDialog();
		confirmDialog.setHeader("Delete");
		confirmDialog.setText("Are you sure you want to permanently delete this Data?");

		confirmDialog.setCancelable(true);
		confirmDialog.setConfirmText("Delete");
		confirmDialog.setConfirmButtonTheme("error primary");
		confirmDialog.addConfirmListener(event -> {
			onDelete(grid.asSingleSelect().getValue());
		});
		
		confirmDialog.open();
	}

	// ✅ Abstract hooks for specific view implementations
	protected abstract void onAdd();

	protected abstract void onEdit(T item);

	protected abstract void onDelete(T item);

	protected abstract void configureGrid();
}
