package io.github.vncntz.hris.app.ui;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import io.github.vncntz.hris.clientmanagement.ClientAdministrationQueries;
import io.github.vncntz.hris.clientmanagement.ClientCompanyPage;
import io.github.vncntz.hris.clientmanagement.ClientCompanyReference;
import io.github.vncntz.hris.clientmanagement.ClientManagementException;
import io.github.vncntz.hris.clientmanagement.ClientManagementService;
import io.github.vncntz.hris.clientmanagement.ClientSitePage;
import io.github.vncntz.hris.clientmanagement.ClientSiteReference;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Responsive, accessible administration view for Client Companies and Sites.
 * Protected by server-side {@code client:admin} exact authority.
 */
@Route(value = "clients", layout = MainLayout.class)
@PageTitle("Clients")
@Menu(order = 10, icon = "vaadin:building", title = "Clients")
@RolesAllowed(ClientManagementService.ADMIN_AUTHORITY)
@StyleSheet("hris/clients.css")
public class ClientAdministrationView extends Div {
    static final int PAGE_SIZE = 15;

    private final ClientAdministrationQueries queries;
    private final ClientManagementService service;

    // Company state
    private int companyOffset = 0;
    private boolean companyHasMore = false;
    private UUID selectedCompanyId = null;
    private ClientCompanyReference selectedCompany = null;

    // Site state
    private int siteOffset = 0;
    private boolean siteHasMore = false;
    private UUID selectedSiteId = null;
    private ClientSiteReference selectedSite = null;

    private boolean programmaticSelectionChange = false;

    // Company UI components
    private final Div companyFeedback = new Div();
    private final Grid<ClientCompanyReference> companyGrid = new Grid<>();
    private final Div companyEmpty = new Div();
    private final Button createCompanyBtn = new Button("New company", new Icon(VaadinIcon.PLUS));
    private final Button renameCompanyBtn = new Button("Rename", new Icon(VaadinIcon.EDIT));
    private final Button toggleCompanyBtn = new Button("Deactivate");
    private final Button companyPrevBtn = new Button("Previous", new Icon(VaadinIcon.ARROW_LEFT));
    private final Button companyNextBtn = new Button("Next", new Icon(VaadinIcon.ARROW_RIGHT));
    private final Span companyPageInfo = new Span("Page 1");
    private final Div companyPaging = new Div();

    // Site UI components
    private final Span sitesContext = new Span("Select a company to view its sites");
    private final Span createSiteHint = new Span("Cannot create sites for an inactive company");
    private final Div siteFeedback = new Div();
    private final Div siteNoCompany = new Div();
    private final Grid<ClientSiteReference> siteGrid = new Grid<>();
    private final Div siteEmpty = new Div();
    private final Button createSiteBtn = new Button("New site", new Icon(VaadinIcon.PLUS));
    private final Button renameSiteBtn = new Button("Rename", new Icon(VaadinIcon.EDIT));
    private final Button toggleSiteBtn = new Button("Deactivate");
    private final Span siteActionHint = new Span();
    private final Button sitePrevBtn = new Button("Previous", new Icon(VaadinIcon.ARROW_LEFT));
    private final Button siteNextBtn = new Button("Next", new Icon(VaadinIcon.ARROW_RIGHT));
    private final Span sitePageInfo = new Span("Page 1");
    private final Div sitePaging = new Div();

    @Autowired
    public ClientAdministrationView(ClientAdministrationQueries queries, ClientManagementService service) {
        this.queries = queries;
        this.service = service;

        setId("hris-clients-view");
        addClassName("hris-clients-view");

        Div workspace = new Div(createCompanySection(), createSiteSection());
        workspace.addClassName("hris-clients-workspace");
        add(workspace);

        loadCompanies();
    }

    private Section createCompanySection() {
        Section section = new Section();
        section.setId("hris-company-section");
        section.addClassName("hris-admin-section");
        section.getElement().setAttribute("aria-labelledby", "hris-companies-heading");

        H2 heading = new H2("Companies");
        heading.setId("hris-companies-heading");
        heading.addClassName("hris-section-heading");

        createCompanyBtn.setId("hris-create-company-btn");
        createCompanyBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createCompanyBtn.addClickListener(e -> openCreateCompanyDialog());

        Div header = new Div(heading, createCompanyBtn);
        header.addClassName("hris-section-header");

        companyFeedback.setId("hris-company-feedback");
        companyFeedback.addClassNames("hris-feedback", "hris-hidden");
        companyFeedback.getElement().setAttribute("role", "status");
        companyFeedback.getElement().setAttribute("aria-live", "polite");

        companyGrid.setId("hris-company-grid");
        companyGrid.getElement().setAttribute("aria-label", "Client companies");
        companyGrid.addColumn(ClientCompanyReference::displayName)
                .setHeader("Company name")
                .setKey("name")
                .setAutoWidth(true)
                .setFlexGrow(1)
                .setSortable(false);
        companyGrid.addComponentColumn(this::renderCompanyStatus)
                .setHeader("Status")
                .setKey("status")
                .setAutoWidth(true)
                .setFlexGrow(0)
                .setSortable(false);
        companyGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
        companyGrid.addSelectionListener(event -> {
            if (programmaticSelectionChange) {
                return;
            }
            ClientCompanyReference selected = event.getFirstSelectedItem().orElse(null);
            onCompanySelected(selected);
        });

        companyEmpty.setId("hris-company-empty");
        companyEmpty.addClassNames("hris-empty-state", "hris-hidden");
        companyEmpty.add(new Span("No companies found. Create a company to get started."));

        renameCompanyBtn.setId("hris-rename-company-btn");
        renameCompanyBtn.setEnabled(false);
        renameCompanyBtn.addClickListener(e -> openRenameCompanyDialog());

        toggleCompanyBtn.setId("hris-toggle-company-btn");
        toggleCompanyBtn.setEnabled(false);
        toggleCompanyBtn.addClickListener(e -> toggleCompanyLifecycle());

        Div actionsBar = new Div(renameCompanyBtn, toggleCompanyBtn);
        actionsBar.setId("hris-company-actions");
        actionsBar.addClassName("hris-actions-bar");

        companyPrevBtn.setId("hris-company-prev-btn");
        companyPrevBtn.setEnabled(false);
        companyPrevBtn.addClickListener(e -> prevCompanyPage());

        companyPageInfo.setId("hris-company-page-info");
        companyPageInfo.addClassName("hris-page-info");

        companyNextBtn.setId("hris-company-next-btn");
        companyNextBtn.setIconAfterText(true);
        companyNextBtn.setEnabled(false);
        companyNextBtn.addClickListener(e -> nextCompanyPage());

        companyPaging.setId("hris-company-paging");
        companyPaging.addClassName("hris-paging-bar");
        companyPaging.getElement().setAttribute("role", "navigation");
        companyPaging.getElement().setAttribute("aria-label", "Company page navigation");
        companyPaging.add(companyPrevBtn, companyPageInfo, companyNextBtn);

        section.add(header, companyFeedback, companyGrid, companyEmpty, actionsBar, companyPaging);
        return section;
    }

    private Section createSiteSection() {
        Section section = new Section();
        section.setId("hris-site-section");
        section.addClassName("hris-admin-section");
        section.getElement().setAttribute("aria-labelledby", "hris-sites-heading");

        H2 heading = new H2("Sites");
        heading.setId("hris-sites-heading");
        heading.addClassName("hris-section-heading");

        sitesContext.setId("hris-sites-context");
        sitesContext.addClassName("hris-section-subtitle");

        createSiteBtn.setId("hris-create-site-btn");
        createSiteBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createSiteBtn.setEnabled(false);
        createSiteBtn.addClickListener(e -> openCreateSiteDialog());

        createSiteHint.setId("hris-create-site-hint");
        createSiteHint.addClassNames("hris-hint", "hris-hidden");

        Div headingGroup = new Div(heading, sitesContext);
        Div actionGroup = new Div(createSiteBtn, createSiteHint);
        actionGroup.addClassName("hris-header-action-group");
        Div header = new Div(headingGroup, actionGroup);
        header.addClassName("hris-section-header");

        siteFeedback.setId("hris-site-feedback");
        siteFeedback.addClassNames("hris-feedback", "hris-hidden");
        siteFeedback.getElement().setAttribute("role", "status");
        siteFeedback.getElement().setAttribute("aria-live", "polite");

        siteNoCompany.setId("hris-site-no-company");
        siteNoCompany.addClassName("hris-empty-state");
        siteNoCompany.add(new Span("Select a company from the list to view and manage its sites."));

        siteGrid.setId("hris-site-grid");
        siteGrid.getElement().setAttribute("aria-label", "Client sites");
        siteGrid.addColumn(ClientSiteReference::displayName)
                .setHeader("Site name")
                .setKey("name")
                .setAutoWidth(true)
                .setFlexGrow(1)
                .setSortable(false);
        siteGrid.addComponentColumn(this::renderSiteStoredStatus)
                .setHeader("Stored status")
                .setKey("storedStatus")
                .setAutoWidth(true)
                .setFlexGrow(0)
                .setSortable(false);
        siteGrid.addComponentColumn(this::renderSiteEffectiveStatus)
                .setHeader("Effective status")
                .setKey("effectiveStatus")
                .setAutoWidth(true)
                .setFlexGrow(0)
                .setSortable(false);
        siteGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
        siteGrid.addClassName("hris-hidden");
        siteGrid.addSelectionListener(event -> {
            if (programmaticSelectionChange) {
                return;
            }
            ClientSiteReference selected = event.getFirstSelectedItem().orElse(null);
            onSiteSelected(selected);
        });

        siteEmpty.setId("hris-site-empty");
        siteEmpty.addClassNames("hris-empty-state", "hris-hidden");
        siteEmpty.add(new Span("No sites found for this company."));

        renameSiteBtn.setId("hris-rename-site-btn");
        renameSiteBtn.setEnabled(false);
        renameSiteBtn.addClickListener(e -> openRenameSiteDialog());

        toggleSiteBtn.setId("hris-toggle-site-btn");
        toggleSiteBtn.setEnabled(false);
        toggleSiteBtn.addClickListener(e -> toggleSiteLifecycle());

        siteActionHint.setId("hris-site-action-hint");
        siteActionHint.addClassNames("hris-hint", "hris-hidden");

        Div actionsBar = new Div(renameSiteBtn, toggleSiteBtn, siteActionHint);
        actionsBar.setId("hris-site-actions");
        actionsBar.addClassName("hris-actions-bar");

        sitePrevBtn.setId("hris-site-prev-btn");
        sitePrevBtn.setEnabled(false);
        sitePrevBtn.addClickListener(e -> prevSitePage());

        sitePageInfo.setId("hris-site-page-info");
        sitePageInfo.addClassName("hris-page-info");

        siteNextBtn.setId("hris-site-next-btn");
        siteNextBtn.setIconAfterText(true);
        siteNextBtn.setEnabled(false);
        siteNextBtn.addClickListener(e -> nextSitePage());

        sitePaging.setId("hris-site-paging");
        sitePaging.addClassNames("hris-paging-bar", "hris-hidden");
        sitePaging.getElement().setAttribute("role", "navigation");
        sitePaging.getElement().setAttribute("aria-label", "Site page navigation");
        sitePaging.add(sitePrevBtn, sitePageInfo, siteNextBtn);

        section.add(header, siteFeedback, siteNoCompany, siteGrid, siteEmpty, actionsBar, sitePaging);
        return section;
    }

    private Component renderCompanyStatus(ClientCompanyReference company) {
        Span badge = new Span(company.active() ? "Active" : "Inactive");
        badge.addClassNames("hris-status-badge", company.active() ? "hris-status-active" : "hris-status-inactive");
        badge.getElement().setAttribute("aria-label", "Status: " + (company.active() ? "Active" : "Inactive"));
        return badge;
    }

    private Component renderSiteStoredStatus(ClientSiteReference site) {
        Span badge = new Span(site.active() ? "Active" : "Inactive");
        badge.addClassNames("hris-status-badge", site.active() ? "hris-status-active" : "hris-status-inactive");
        badge.getElement().setAttribute("aria-label", "Stored status: " + (site.active() ? "Active" : "Inactive"));
        return badge;
    }

    private Component renderSiteEffectiveStatus(ClientSiteReference site) {
        Span badge;
        if (site.effectiveActive()) {
            badge = new Span("Active");
            badge.addClassNames("hris-status-badge", "hris-status-active");
            badge.getElement().setAttribute("aria-label", "Effective status: Active");
        } else if (site.active() && !site.companyActive()) {
            badge = new Span("Inactive (Company inactive)");
            badge.addClassNames("hris-status-badge", "hris-status-effectively-inactive");
            badge.getElement().setAttribute("aria-label", "Effective status: Inactive because company is inactive");
        } else {
            badge = new Span("Inactive");
            badge.addClassNames("hris-status-badge", "hris-status-inactive");
            badge.getElement().setAttribute("aria-label", "Effective status: Inactive");
        }
        return badge;
    }

    private void loadCompanies() {
        try {
            ClientCompanyPage page = queries.companies(companyOffset, PAGE_SIZE);
            companyHasMore = page.hasMore();
            List<ClientCompanyReference> rows = page.rows();

            UUID targetId = selectedCompanyId;
            programmaticSelectionChange = true;
            try {
                companyGrid.setItems(rows);
            } finally {
                programmaticSelectionChange = false;
            }

            boolean empty = rows.isEmpty();
            if (empty && companyOffset == 0) {
                companyEmpty.removeClassName("hris-hidden");
                companyGrid.addClassName("hris-hidden");
            } else {
                companyEmpty.addClassName("hris-hidden");
                companyGrid.removeClassName("hris-hidden");
            }

            int pageNum = (companyOffset / PAGE_SIZE) + 1;
            companyPageInfo.setText("Page " + pageNum);
            companyPrevBtn.setEnabled(companyOffset > 0);
            companyNextBtn.setEnabled(companyHasMore);

            if (targetId != null) {
                Optional<ClientCompanyReference> matching = rows.stream()
                        .filter(c -> c.publicId().equals(targetId))
                        .findFirst();
                if (matching.isPresent()) {
                    selectedCompany = matching.get();
                    selectedCompanyId = selectedCompany.publicId();
                    programmaticSelectionChange = true;
                    try {
                        companyGrid.asSingleSelect().setValue(selectedCompany);
                    } finally {
                        programmaticSelectionChange = false;
                    }
                    updateCompanyActionButtons();
                } else {
                    clearCompanySelection();
                }
            } else {
                clearCompanySelection();
            }
        } catch (ClientManagementException ex) {
            handleCompanyException(ex);
        } catch (Exception ex) {
            showCompanyError("The operation could not be completed. Please try again later.");
        }
    }

    private void loadSites() {
        if (selectedCompany == null) {
            siteGrid.addClassName("hris-hidden");
            siteEmpty.addClassName("hris-hidden");
            siteNoCompany.removeClassName("hris-hidden");
            sitePaging.addClassName("hris-hidden");
            clearSiteSelection();
            return;
        }

        siteNoCompany.addClassName("hris-hidden");
        try {
            ClientSitePage page = queries.sites(selectedCompany.publicId(), siteOffset, PAGE_SIZE);
            siteHasMore = page.hasMore();
            List<ClientSiteReference> rows = page.rows();

            UUID targetSiteId = selectedSiteId;
            programmaticSelectionChange = true;
            try {
                siteGrid.setItems(rows);
            } finally {
                programmaticSelectionChange = false;
            }

            boolean empty = rows.isEmpty();
            if (empty && siteOffset == 0) {
                siteEmpty.removeClassName("hris-hidden");
                siteGrid.addClassName("hris-hidden");
                sitePaging.addClassName("hris-hidden");
            } else {
                siteEmpty.addClassName("hris-hidden");
                siteGrid.removeClassName("hris-hidden");
                sitePaging.removeClassName("hris-hidden");
            }

            int pageNum = (siteOffset / PAGE_SIZE) + 1;
            sitePageInfo.setText("Page " + pageNum);
            sitePrevBtn.setEnabled(siteOffset > 0);
            siteNextBtn.setEnabled(siteHasMore);

            if (targetSiteId != null) {
                Optional<ClientSiteReference> matching = rows.stream()
                        .filter(s -> s.publicId().equals(targetSiteId))
                        .findFirst();
                if (matching.isPresent()) {
                    selectedSite = matching.get();
                    selectedSiteId = selectedSite.publicId();
                    programmaticSelectionChange = true;
                    try {
                        siteGrid.asSingleSelect().setValue(selectedSite);
                    } finally {
                        programmaticSelectionChange = false;
                    }
                    updateSiteActionButtons();
                } else {
                    clearSiteSelection();
                }
            } else {
                clearSiteSelection();
            }
        } catch (ClientManagementException ex) {
            handleSiteException(ex);
        } catch (Exception ex) {
            showSiteError("The operation could not be completed. Please try again later.");
        }
    }

    private void onCompanySelected(ClientCompanyReference company) {
        if (company != null) {
            selectedCompanyId = company.publicId();
            selectedCompany = company;
            siteOffset = 0;
            selectedSiteId = null;
            selectedSite = null;
            updateCompanyActionButtons();
            loadSites();
        } else {
            clearCompanySelection();
        }
    }

    private void onSiteSelected(ClientSiteReference site) {
        if (site != null) {
            selectedSiteId = site.publicId();
            selectedSite = site;
            updateSiteActionButtons();
        } else {
            clearSiteSelection();
        }
    }

    private void clearCompanySelection() {
        selectedCompanyId = null;
        selectedCompany = null;
        programmaticSelectionChange = true;
        try {
            companyGrid.asSingleSelect().clear();
        } finally {
            programmaticSelectionChange = false;
        }
        updateCompanyActionButtons();
        loadSites();
    }

    private void clearSiteSelection() {
        selectedSiteId = null;
        selectedSite = null;
        programmaticSelectionChange = true;
        try {
            siteGrid.asSingleSelect().clear();
        } finally {
            programmaticSelectionChange = false;
        }
        updateSiteActionButtons();
    }

    private void updateCompanyActionButtons() {
        boolean hasSelection = selectedCompany != null;
        renameCompanyBtn.setEnabled(hasSelection);
        toggleCompanyBtn.setEnabled(hasSelection);
        if (hasSelection) {
            if (selectedCompany.active()) {
                toggleCompanyBtn.setText("Deactivate");
                toggleCompanyBtn.setIcon(new Icon(VaadinIcon.BAN));
                toggleCompanyBtn.removeThemeVariants(ButtonVariant.LUMO_SUCCESS);
                toggleCompanyBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
            } else {
                toggleCompanyBtn.setText("Activate");
                toggleCompanyBtn.setIcon(new Icon(VaadinIcon.CHECK));
                toggleCompanyBtn.removeThemeVariants(ButtonVariant.LUMO_ERROR);
                toggleCompanyBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
            }
            createSiteBtn.setEnabled(selectedCompany.active());
            if (selectedCompany.active()) {
                createSiteHint.addClassName("hris-hidden");
                sitesContext.setText("Sites for " + selectedCompany.displayName());
            } else {
                createSiteHint.removeClassName("hris-hidden");
                sitesContext.setText("Sites for " + selectedCompany.displayName() + " (Company inactive)");
            }
        } else {
            toggleCompanyBtn.setText("Deactivate");
            toggleCompanyBtn.setIcon(new Icon(VaadinIcon.BAN));
            toggleCompanyBtn.removeThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_ERROR);
            createSiteBtn.setEnabled(false);
            createSiteHint.addClassName("hris-hidden");
            sitesContext.setText("Select a company to view its sites");
        }
    }

    private void updateSiteActionButtons() {
        boolean hasSelection = selectedSite != null;
        renameSiteBtn.setEnabled(hasSelection);
        if (!hasSelection) {
            toggleSiteBtn.setEnabled(false);
            toggleSiteBtn.setText("Deactivate");
            toggleSiteBtn.setIcon(new Icon(VaadinIcon.BAN));
            toggleSiteBtn.removeThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_ERROR);
            siteActionHint.addClassName("hris-hidden");
            return;
        }

        if (selectedSite.active()) {
            toggleSiteBtn.setEnabled(true);
            toggleSiteBtn.setText("Deactivate");
            toggleSiteBtn.setIcon(new Icon(VaadinIcon.BAN));
            toggleSiteBtn.removeThemeVariants(ButtonVariant.LUMO_SUCCESS);
            toggleSiteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
            siteActionHint.addClassName("hris-hidden");
        } else {
            toggleSiteBtn.setText("Activate");
            toggleSiteBtn.setIcon(new Icon(VaadinIcon.CHECK));
            toggleSiteBtn.removeThemeVariants(ButtonVariant.LUMO_ERROR);
            toggleSiteBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
            if (selectedCompany != null && selectedCompany.active()) {
                toggleSiteBtn.setEnabled(true);
                siteActionHint.addClassName("hris-hidden");
            } else {
                toggleSiteBtn.setEnabled(false);
                siteActionHint.setText("Cannot activate site because its company is inactive.");
                siteActionHint.removeClassName("hris-hidden");
            }
        }
    }

    private void prevCompanyPage() {
        if (companyOffset > 0) {
            companyOffset = Math.max(0, companyOffset - PAGE_SIZE);
            loadCompanies();
        }
    }

    private void nextCompanyPage() {
        if (companyHasMore) {
            companyOffset += PAGE_SIZE;
            loadCompanies();
        }
    }

    private void prevSitePage() {
        if (siteOffset > 0) {
            siteOffset = Math.max(0, siteOffset - PAGE_SIZE);
            loadSites();
        }
    }

    private void nextSitePage() {
        if (siteHasMore) {
            siteOffset += PAGE_SIZE;
            loadSites();
        }
    }

    private void openCreateCompanyDialog() {
        Dialog dialog = new Dialog();
        dialog.setId("hris-create-company-dialog");
        dialog.setHeaderTitle("New Company");
        dialog.setCloseOnOutsideClick(false);
        dialog.setCloseOnEsc(true);

        TextField nameField = new TextField("Company name");
        nameField.setId("hris-company-name-input");
        nameField.setMaxLength(200);
        nameField.setRequired(true);
        nameField.setWidthFull();
        nameField.setAutofocus(true);

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        cancelBtn.setId("hris-cancel-company-btn");

        Button createBtn = new Button("Create");
        createBtn.setId("hris-save-company-btn");
        createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createBtn.addClickListener(e -> {
            String raw = nameField.getValue();
            String name = raw != null ? raw.strip() : "";
            if (name.isBlank()) {
                nameField.setErrorMessage("Company name is required.");
                nameField.setInvalid(true);
                return;
            }
            if (name.length() > 200) {
                nameField.setErrorMessage("Company name must be 200 characters or fewer.");
                nameField.setInvalid(true);
                return;
            }
            if (hasControlCharacters(name)) {
                nameField.setErrorMessage("Company name contains invalid control characters.");
                nameField.setInvalid(true);
                return;
            }

            createBtn.setEnabled(false);
            cancelBtn.setEnabled(false);
            try {
                ClientCompanyReference created = service.createCompany(name);
                dialog.close();
                showCompanySuccess("Company \"" + created.displayName() + "\" created.");
                loadCompanies();
            } catch (ClientManagementException ex) {
                createBtn.setEnabled(true);
                cancelBtn.setEnabled(true);
                if (ex.reason() == ClientManagementException.Reason.INVALID_NAME) {
                    nameField.setErrorMessage(userMessageFor(ex));
                    nameField.setInvalid(true);
                } else {
                    dialog.close();
                    handleCompanyException(ex);
                }
            } catch (Exception ex) {
                dialog.close();
                showCompanyError("The operation could not be completed. Please try again later.");
                loadCompanies();
            }
        });

        dialog.add(nameField);
        dialog.getFooter().add(cancelBtn, createBtn);
        dialog.addOpenedChangeListener(e -> {
            if (!e.isOpened()) {
                remove(dialog);
            }
        });
        add(dialog);
        dialog.open();
    }

    private void openRenameCompanyDialog() {
        if (selectedCompany == null) return;
        Dialog dialog = new Dialog();
        dialog.setId("hris-rename-company-dialog");
        dialog.setHeaderTitle("Rename Company");
        dialog.setCloseOnOutsideClick(false);
        dialog.setCloseOnEsc(true);

        TextField nameField = new TextField("Company name");
        nameField.setId("hris-rename-company-input");
        nameField.setValue(selectedCompany.displayName());
        nameField.setMaxLength(200);
        nameField.setRequired(true);
        nameField.setWidthFull();
        nameField.setAutofocus(true);

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        cancelBtn.setId("hris-cancel-rename-company-btn");

        Button renameBtn = new Button("Rename");
        renameBtn.setId("hris-save-rename-company-btn");
        renameBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        renameBtn.addClickListener(e -> {
            String raw = nameField.getValue();
            String name = raw != null ? raw.strip() : "";
            if (name.isBlank()) {
                nameField.setErrorMessage("Company name is required.");
                nameField.setInvalid(true);
                return;
            }
            if (name.length() > 200) {
                nameField.setErrorMessage("Company name must be 200 characters or fewer.");
                nameField.setInvalid(true);
                return;
            }
            if (hasControlCharacters(name)) {
                nameField.setErrorMessage("Company name contains invalid control characters.");
                nameField.setInvalid(true);
                return;
            }
            if (name.equals(selectedCompany.displayName())) {
                dialog.close();
                showCompanyInfo("No changes were made.");
                return;
            }

            renameBtn.setEnabled(false);
            cancelBtn.setEnabled(false);
            try {
                ClientCompanyReference renamed = service.renameCompany(
                        selectedCompany.publicId(), name, selectedCompany.version());
                dialog.close();
                showCompanySuccess("Company renamed to \"" + renamed.displayName() + "\".");
                loadCompanies();
                loadSites();
            } catch (ClientManagementException ex) {
                renameBtn.setEnabled(true);
                cancelBtn.setEnabled(true);
                if (ex.reason() == ClientManagementException.Reason.INVALID_NAME) {
                    nameField.setErrorMessage(userMessageFor(ex));
                    nameField.setInvalid(true);
                } else {
                    dialog.close();
                    handleCompanyException(ex);
                }
            } catch (Exception ex) {
                dialog.close();
                showCompanyError("The operation could not be completed. Please try again later.");
                loadCompanies();
            }
        });

        dialog.add(nameField);
        dialog.getFooter().add(cancelBtn, renameBtn);
        dialog.addOpenedChangeListener(e -> {
            if (!e.isOpened()) {
                remove(dialog);
            }
        });
        add(dialog);
        dialog.open();
    }

    private void toggleCompanyLifecycle() {
        if (selectedCompany == null) return;
        boolean willActivate = !selectedCompany.active();
        String title = willActivate ? "Activate Company" : "Deactivate Company";
        String message = willActivate
                ? "Are you sure you want to activate company \"" + selectedCompany.displayName() + "\"?"
                : "Are you sure you want to deactivate company \"" + selectedCompany.displayName()
                  + "\"? Deactivating this company will make all its sites effectively inactive, but will preserve each site's stored active status.";
        String confirmText = willActivate ? "Activate" : "Deactivate";
        boolean isDestructive = !willActivate;

        openConfirmDialog(title, message, confirmText, isDestructive, () -> {
            try {
                if (willActivate) {
                    service.activateCompany(selectedCompany.publicId(), selectedCompany.version());
                    showCompanySuccess("Company \"" + selectedCompany.displayName() + "\" activated.");
                } else {
                    service.deactivateCompany(selectedCompany.publicId(), selectedCompany.version());
                    showCompanySuccess("Company \"" + selectedCompany.displayName() + "\" deactivated.");
                }
                loadCompanies();
                loadSites();
            } catch (ClientManagementException ex) {
                handleCompanyException(ex);
            } catch (Exception ex) {
                showCompanyError("The operation could not be completed. Please try again later.");
                loadCompanies();
                loadSites();
            }
        });
    }

    private void openCreateSiteDialog() {
        if (selectedCompany == null || !selectedCompany.active()) {
            showSiteError("The company must be active to perform this site action. Current status has been refreshed.");
            loadCompanies();
            loadSites();
            return;
        }
        Dialog dialog = new Dialog();
        dialog.setId("hris-create-site-dialog");
        dialog.setHeaderTitle("New Site");
        dialog.setCloseOnOutsideClick(false);
        dialog.setCloseOnEsc(true);

        Paragraph parentInfo = new Paragraph("Company: " + selectedCompany.displayName());
        parentInfo.addClassNames("hris-dialog-subtitle");

        TextField nameField = new TextField("Site name");
        nameField.setId("hris-site-name-input");
        nameField.setMaxLength(200);
        nameField.setRequired(true);
        nameField.setWidthFull();
        nameField.setAutofocus(true);

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        cancelBtn.setId("hris-cancel-site-btn");

        Button createBtn = new Button("Create");
        createBtn.setId("hris-save-site-btn");
        createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createBtn.addClickListener(e -> {
            String raw = nameField.getValue();
            String name = raw != null ? raw.strip() : "";
            if (name.isBlank()) {
                nameField.setErrorMessage("Site name is required.");
                nameField.setInvalid(true);
                return;
            }
            if (name.length() > 200) {
                nameField.setErrorMessage("Site name must be 200 characters or fewer.");
                nameField.setInvalid(true);
                return;
            }
            if (hasControlCharacters(name)) {
                nameField.setErrorMessage("Site name contains invalid control characters.");
                nameField.setInvalid(true);
                return;
            }

            createBtn.setEnabled(false);
            cancelBtn.setEnabled(false);
            try {
                ClientSiteReference created = service.createSite(selectedCompany.publicId(), name);
                dialog.close();
                showSiteSuccess("Site \"" + created.displayName() + "\" created.");
                loadSites();
            } catch (ClientManagementException ex) {
                createBtn.setEnabled(true);
                cancelBtn.setEnabled(true);
                if (ex.reason() == ClientManagementException.Reason.INVALID_NAME) {
                    nameField.setErrorMessage(userMessageFor(ex));
                    nameField.setInvalid(true);
                } else {
                    dialog.close();
                    handleSiteException(ex);
                }
            } catch (Exception ex) {
                dialog.close();
                showSiteError("The operation could not be completed. Please try again later.");
                loadSites();
            }
        });

        dialog.add(parentInfo, nameField);
        dialog.getFooter().add(cancelBtn, createBtn);
        dialog.addOpenedChangeListener(e -> {
            if (!e.isOpened()) {
                remove(dialog);
            }
        });
        add(dialog);
        dialog.open();
    }

    private void openRenameSiteDialog() {
        if (selectedSite == null) return;
        Dialog dialog = new Dialog();
        dialog.setId("hris-rename-site-dialog");
        dialog.setHeaderTitle("Rename Site");
        dialog.setCloseOnOutsideClick(false);
        dialog.setCloseOnEsc(true);

        TextField nameField = new TextField("Site name");
        nameField.setId("hris-rename-site-input");
        nameField.setValue(selectedSite.displayName());
        nameField.setMaxLength(200);
        nameField.setRequired(true);
        nameField.setWidthFull();
        nameField.setAutofocus(true);

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        cancelBtn.setId("hris-cancel-rename-site-btn");

        Button renameBtn = new Button("Rename");
        renameBtn.setId("hris-save-rename-site-btn");
        renameBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        renameBtn.addClickListener(e -> {
            String raw = nameField.getValue();
            String name = raw != null ? raw.strip() : "";
            if (name.isBlank()) {
                nameField.setErrorMessage("Site name is required.");
                nameField.setInvalid(true);
                return;
            }
            if (name.length() > 200) {
                nameField.setErrorMessage("Site name must be 200 characters or fewer.");
                nameField.setInvalid(true);
                return;
            }
            if (hasControlCharacters(name)) {
                nameField.setErrorMessage("Site name contains invalid control characters.");
                nameField.setInvalid(true);
                return;
            }
            if (name.equals(selectedSite.displayName())) {
                dialog.close();
                showSiteInfo("No changes were made.");
                return;
            }

            renameBtn.setEnabled(false);
            cancelBtn.setEnabled(false);
            try {
                ClientSiteReference renamed = service.renameSite(
                        selectedSite.publicId(), name, selectedSite.version());
                dialog.close();
                showSiteSuccess("Site renamed to \"" + renamed.displayName() + "\".");
                loadSites();
            } catch (ClientManagementException ex) {
                renameBtn.setEnabled(true);
                cancelBtn.setEnabled(true);
                if (ex.reason() == ClientManagementException.Reason.INVALID_NAME) {
                    nameField.setErrorMessage(userMessageFor(ex));
                    nameField.setInvalid(true);
                } else {
                    dialog.close();
                    handleSiteException(ex);
                }
            } catch (Exception ex) {
                dialog.close();
                showSiteError("The operation could not be completed. Please try again later.");
                loadSites();
            }
        });

        dialog.add(nameField);
        dialog.getFooter().add(cancelBtn, renameBtn);
        dialog.addOpenedChangeListener(e -> {
            if (!e.isOpened()) {
                remove(dialog);
            }
        });
        add(dialog);
        dialog.open();
    }

    private void toggleSiteLifecycle() {
        if (selectedSite == null || selectedCompany == null) return;
        boolean willActivate = !selectedSite.active();
        if (willActivate && !selectedCompany.active()) {
            showSiteError("The company must be active to perform this site action. Current status has been refreshed.");
            loadCompanies();
            loadSites();
            return;
        }

        String title = willActivate ? "Activate Site" : "Deactivate Site";
        String message = willActivate
                ? "Are you sure you want to activate site \"" + selectedSite.displayName() + "\"?"
                : "Are you sure you want to deactivate site \"" + selectedSite.displayName() + "\"?";
        String confirmText = willActivate ? "Activate" : "Deactivate";
        boolean isDestructive = !willActivate;

        openConfirmDialog(title, message, confirmText, isDestructive, () -> {
            try {
                if (willActivate) {
                    service.activateSite(selectedSite.publicId(), selectedSite.version());
                    showSiteSuccess("Site \"" + selectedSite.displayName() + "\" activated.");
                } else {
                    service.deactivateSite(selectedSite.publicId(), selectedSite.version());
                    showSiteSuccess("Site \"" + selectedSite.displayName() + "\" deactivated.");
                }
                loadSites();
            } catch (ClientManagementException ex) {
                handleSiteException(ex);
            } catch (Exception ex) {
                showSiteError("The operation could not be completed. Please try again later.");
                loadSites();
            }
        });
    }

    private void openConfirmDialog(String title, String message, String confirmText, boolean isDestructive, Runnable onConfirm) {
        Dialog dialog = new Dialog();
        dialog.setId("hris-lifecycle-dialog");
        dialog.setHeaderTitle(title);
        dialog.setCloseOnOutsideClick(false);
        dialog.setCloseOnEsc(true);

        Paragraph msg = new Paragraph(message);
        msg.setId("hris-lifecycle-message");

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        cancelBtn.setId("hris-cancel-lifecycle-btn");

        Button confirmBtn = new Button(confirmText);
        confirmBtn.setId("hris-confirm-lifecycle-btn");
        if (isDestructive) {
            confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        } else {
            confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        }

        confirmBtn.addClickListener(e -> {
            confirmBtn.setEnabled(false);
            cancelBtn.setEnabled(false);
            dialog.close();
            onConfirm.run();
        });

        dialog.add(msg);
        dialog.getFooter().add(cancelBtn, confirmBtn);
        dialog.addOpenedChangeListener(e -> {
            if (!e.isOpened()) {
                remove(dialog);
            }
        });
        add(dialog);
        dialog.open();
    }

    private void handleCompanyException(ClientManagementException ex) {
        showCompanyError(userMessageFor(ex));
        switch (ex.reason()) {
            case NOT_FOUND, INVALID_TARGET -> {
                clearCompanySelection();
                loadCompanies();
            }
            case STALE_VERSION, CONFLICT, INACTIVE_COMPANY -> {
                loadCompanies();
                loadSites();
            }
            case INVALID_PAGE -> {
                companyOffset = 0;
                loadCompanies();
            }
            default -> loadCompanies();
        }
    }

    private void handleSiteException(ClientManagementException ex) {
        showSiteError(userMessageFor(ex));
        switch (ex.reason()) {
            case NOT_FOUND, INVALID_TARGET -> {
                clearSiteSelection();
                loadSites();
            }
            case STALE_VERSION, CONFLICT -> {
                loadSites();
            }
            case INACTIVE_COMPANY -> {
                loadCompanies();
                loadSites();
            }
            case INVALID_PAGE -> {
                siteOffset = 0;
                loadSites();
            }
            default -> loadSites();
        }
    }

    private void showCompanySuccess(String message) {
        setFeedback(companyFeedback, message, "hris-feedback-success");
    }

    private void showCompanyError(String message) {
        setFeedback(companyFeedback, message, "hris-feedback-error");
    }

    private void showCompanyInfo(String message) {
        setFeedback(companyFeedback, message, "hris-feedback-info");
    }

    private void showSiteSuccess(String message) {
        setFeedback(siteFeedback, message, "hris-feedback-success");
    }

    private void showSiteError(String message) {
        setFeedback(siteFeedback, message, "hris-feedback-error");
    }

    private void showSiteInfo(String message) {
        setFeedback(siteFeedback, message, "hris-feedback-info");
    }

    private void setFeedback(Div feedbackDiv, String message, String themeClass) {
        feedbackDiv.removeAll();
        feedbackDiv.removeClassName("hris-hidden");
        feedbackDiv.removeClassName("hris-feedback-success");
        feedbackDiv.removeClassName("hris-feedback-error");
        feedbackDiv.removeClassName("hris-feedback-info");
        feedbackDiv.addClassName(themeClass);

        Span text = new Span(message);
        Button close = new Button(new Icon(VaadinIcon.CLOSE), e -> {
            feedbackDiv.removeAll();
            feedbackDiv.addClassName("hris-hidden");
        });
        close.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        close.getElement().setAttribute("aria-label", "Dismiss notification");

        feedbackDiv.add(text, close);
    }

    static boolean hasControlCharacters(String s) {
        return s != null && s.codePoints().anyMatch(c -> Character.isISOControl(c) || (c >= 0xD800 && c <= 0xDFFF));
    }

    static String userMessageFor(ClientManagementException ex) {
        return switch (ex.reason()) {
            case INVALID_NAME -> "Name is invalid. It must be 1 to 200 characters long and contain no control characters.";
            case INVALID_TARGET, NOT_FOUND -> "The selected record is no longer available. The view has been refreshed.";
            case INVALID_PAGE -> "Requested page is no longer valid. Reset to the first page.";
            case NO_CHANGE -> "No changes were made.";
            case INACTIVE_COMPANY -> "The company must be active to perform this site action. Current status has been refreshed.";
            case STALE_VERSION -> "The record was modified by another operation. Please review the updated information and try again.";
            case CONFLICT -> "A concurrent change conflict occurred. Please retry your request.";
            case PERSISTENCE_FAILED -> "The operation could not be completed. Please try again later.";
        };
    }
}
