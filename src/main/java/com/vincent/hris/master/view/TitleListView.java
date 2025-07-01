package com.vincent.hris.master.view;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vincent.hris.master.model.Title;
import com.vincent.hris.master.service.TitleService;
import com.vincent.hris.modules.base.view.BaseGridView;
import com.vincent.hris.util.NotificationUtil;

import jakarta.annotation.PostConstruct;

@PageTitle("Title")
@Route("title-list")
public class TitleListView extends BaseGridView<Title> {

	private static final long serialVersionUID = 1L;

	@Autowired
	private TitleService titleService;

	public TitleListView() {
		super();
		configureGrid();
	}

	@Override
	protected void configureGrid() {
		grid.addColumn(Title::getId).setHeader("Id").setWidth("80px").setFlexGrow(0)
				.setTextAlign(ColumnTextAlign.CENTER);
		grid.addColumn(Title::getTitle).setHeader("Title").setAutoWidth(true);
		grid.addColumn(Title::getDescription).setHeader("Description").setAutoWidth(true);

	}

//	@Override
//	protected void onAdd() {
//		AddEditModuleDialog dialog = new AddEditModuleDialog(titleService, null);
//		dialog.open();
//		dialog.addOpenedChangeListener(e -> refreshGrid());
//	}
//
//	@Override
//	protected void onEdit(Title title) {
//		AddEditModuleDialog dialog = new AddEditModuleDialog(titleService, title);
//		dialog.open();
//		dialog.addOpenedChangeListener(e -> refreshGrid());
//	}

	protected void onDelete(Title title) {
		titleService.delete(title);
		NotificationUtil.success("Deleted module: " + title.getTitle());
		refreshGrid();
	}

	@PostConstruct
	private void refreshGrid() {
		List<Title> title = titleService.findAll();
		grid.setItems(title);
	}

	@Override
	protected void onAdd() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void onEdit(Title item) {
		// TODO Auto-generated method stub

	}
}
