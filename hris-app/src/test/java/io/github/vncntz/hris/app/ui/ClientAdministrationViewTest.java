package io.github.vncntz.hris.app.ui;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import io.github.vncntz.hris.clientmanagement.ClientAdministrationQueries;
import io.github.vncntz.hris.clientmanagement.ClientCompanyPage;
import io.github.vncntz.hris.clientmanagement.ClientCompanyReference;
import io.github.vncntz.hris.clientmanagement.ClientManagementException;
import io.github.vncntz.hris.clientmanagement.ClientManagementService;
import io.github.vncntz.hris.clientmanagement.ClientSitePage;
import io.github.vncntz.hris.clientmanagement.ClientSiteReference;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;
import org.junit.jupiter.api.AfterEach;

class ClientAdministrationViewTest {

    private ClientAdministrationQueries queries;
    private ClientManagementService service;
    private VaadinSession session;
    private UI ui;

    private final UUID company1Id = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID company2Id = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private final UUID site1Id = UUID.fromString("00000000-0000-0000-0000-000000000011");
    private final UUID site2Id = UUID.fromString("00000000-0000-0000-0000-000000000012");

    @BeforeEach
    void setUp() {
        queries = mock(ClientAdministrationQueries.class);
        service = mock(ClientManagementService.class);
        session = mock(VaadinSession.class);
        when(session.hasLock()).thenReturn(true);
        VaadinSession.setCurrent(session);
        ui = new UI();
        ui.getInternals().setSession(session);
        UI.setCurrent(ui);
    }

    @AfterEach
    void tearDown() {
        UI.setCurrent(null);
        VaadinSession.setCurrent(null);
    }

    private ClientAdministrationView createView() {
        return new ClientAdministrationView(queries, service);
    }

    @Test
    void routeAndMenuMetadataAreConfiguredWithExactAuthority() {
        Route route = ClientAdministrationView.class.getAnnotation(Route.class);
        assertNotNull(route);
        assertEquals("clients", route.value());
        assertEquals(MainLayout.class, route.layout());

        PageTitle pageTitle = ClientAdministrationView.class.getAnnotation(PageTitle.class);
        assertNotNull(pageTitle);
        assertEquals("Clients", pageTitle.value());

        Menu menu = ClientAdministrationView.class.getAnnotation(Menu.class);
        assertNotNull(menu);
        assertEquals("Clients", menu.title());
        assertEquals("vaadin:building", menu.icon());
        assertEquals(10.0, menu.order());

        RolesAllowed rolesAllowed = ClientAdministrationView.class.getAnnotation(RolesAllowed.class);
        assertNotNull(rolesAllowed);
        assertEquals(1, rolesAllowed.value().length);
        assertEquals(ClientManagementService.ADMIN_AUTHORITY, rolesAllowed.value()[0]);
        assertEquals("client:admin", rolesAllowed.value()[0]);

        assertNull(ClientAdministrationView.class.getAnnotation(PermitAll.class));
        assertNull(ClientAdministrationView.class.getAnnotation(AnonymousAllowed.class));

        StyleSheet styleSheet = ClientAdministrationView.class.getAnnotation(StyleSheet.class);
        assertNotNull(styleSheet);
        assertEquals("hris/clients.css", styleSheet.value());
    }

    @Test
    void visualHierarchyContainsNoSecondH1Heading() {
        when(queries.companies(0, ClientAdministrationView.PAGE_SIZE))
                .thenReturn(new ClientCompanyPage(List.of(), false));

        ClientAdministrationView view = createView();
        long h1Count = descendants(view).filter(H1.class::isInstance).count();
        assertEquals(0, h1Count, "ClientAdministrationView must not define an H1 heading (MainLayout owns H1)");
    }

    @Test
    void initialLoadRendersEmptyCompanyStateWhenNoCompaniesExist() {
        when(queries.companies(0, ClientAdministrationView.PAGE_SIZE))
                .thenReturn(new ClientCompanyPage(List.of(), false));

        ClientAdministrationView view = createView();

        Div companyEmpty = find(view, Div.class, "hris-company-empty");
        assertFalse(companyEmpty.getClassNames().contains("hris-hidden"));

        Grid<?> companyGrid = find(view, Grid.class, "hris-company-grid");
        assertTrue(companyGrid.getClassNames().contains("hris-hidden"));

        Div siteNoCompany = find(view, Div.class, "hris-site-no-company");
        assertFalse(siteNoCompany.getClassNames().contains("hris-hidden"));

        Button createSiteBtn = find(view, Button.class, "hris-create-site-btn");
        assertFalse(createSiteBtn.isEnabled());

        Button renameCompanyBtn = find(view, Button.class, "hris-rename-company-btn");
        assertFalse(renameCompanyBtn.isEnabled());

        Button toggleCompanyBtn = find(view, Button.class, "hris-toggle-company-btn");
        assertFalse(toggleCompanyBtn.isEnabled());
    }

    @Test
    void initialLoadRendersCompaniesAndAllowsSelectionOfActiveCompany() {
        ClientCompanyReference c1 = new ClientCompanyReference(company1Id, "Acme Corp", true, 1L);
        when(queries.companies(0, ClientAdministrationView.PAGE_SIZE))
                .thenReturn(new ClientCompanyPage(List.of(c1), false));
        when(queries.sites(company1Id, 0, ClientAdministrationView.PAGE_SIZE))
                .thenReturn(new ClientSitePage(List.of(), false));

        ClientAdministrationView view = createView();

        @SuppressWarnings("unchecked")
        Grid<ClientCompanyReference> companyGrid = find(view, Grid.class, "hris-company-grid");
        assertFalse(companyGrid.getClassNames().contains("hris-hidden"));

        // Select the active company
        companyGrid.asSingleSelect().setValue(c1);

        verify(queries).sites(company1Id, 0, ClientAdministrationView.PAGE_SIZE);

        Button renameCompanyBtn = find(view, Button.class, "hris-rename-company-btn");
        assertTrue(renameCompanyBtn.isEnabled());

        Button toggleCompanyBtn = find(view, Button.class, "hris-toggle-company-btn");
        assertTrue(toggleCompanyBtn.isEnabled());
        assertEquals("Deactivate", toggleCompanyBtn.getText());

        Button createSiteBtn = find(view, Button.class, "hris-create-site-btn");
        assertTrue(createSiteBtn.isEnabled());

        Span createSiteHint = find(view, Span.class, "hris-create-site-hint");
        assertTrue(createSiteHint.getClassNames().contains("hris-hidden"));

        Span sitesContext = find(view, Span.class, "hris-sites-context");
        assertEquals("Sites for Acme Corp", sitesContext.getText());
    }

    @Test
    void selectingInactiveCompanyDisablesCreateSiteWithExplanation() {
        ClientCompanyReference inactiveCompany = new ClientCompanyReference(company2Id, "Inactive Corp", false, 1L);
        when(queries.companies(0, ClientAdministrationView.PAGE_SIZE))
                .thenReturn(new ClientCompanyPage(List.of(inactiveCompany), false));
        when(queries.sites(company2Id, 0, ClientAdministrationView.PAGE_SIZE))
                .thenReturn(new ClientSitePage(List.of(), false));

        ClientAdministrationView view = createView();

        @SuppressWarnings("unchecked")
        Grid<ClientCompanyReference> companyGrid = find(view, Grid.class, "hris-company-grid");
        companyGrid.asSingleSelect().setValue(inactiveCompany);

        Button toggleCompanyBtn = find(view, Button.class, "hris-toggle-company-btn");
        assertTrue(toggleCompanyBtn.isEnabled());
        assertEquals("Activate", toggleCompanyBtn.getText());

        Button createSiteBtn = find(view, Button.class, "hris-create-site-btn");
        assertFalse(createSiteBtn.isEnabled());

        Span createSiteHint = find(view, Span.class, "hris-create-site-hint");
        assertFalse(createSiteHint.getClassNames().contains("hris-hidden"));
        assertEquals("Cannot create sites for an inactive company", createSiteHint.getText());

        Span sitesContext = find(view, Span.class, "hris-sites-context");
        assertTrue(sitesContext.getText().contains("Inactive Corp"));
        assertTrue(sitesContext.getText().contains("Company inactive"));

        Button renameCompanyBtn = find(view, Button.class, "hris-rename-company-btn");
        assertTrue(renameCompanyBtn.isEnabled(), "Rename must remain enabled for inactive company");
    }

    @Test
    void storedVsEffectiveSiteActivityPresentationAndControls() {
        ClientCompanyReference inactiveCompany = new ClientCompanyReference(company2Id, "Parent Corp", false, 1L);
        ClientSiteReference site1 = new ClientSiteReference(site1Id, company2Id, "Headquarters", true, false, 1L);
        ClientSiteReference site2 = new ClientSiteReference(site2Id, company2Id, "Branch Office", false, false, 1L);

        when(queries.companies(0, ClientAdministrationView.PAGE_SIZE))
                .thenReturn(new ClientCompanyPage(List.of(inactiveCompany), false));
        when(queries.sites(company2Id, 0, ClientAdministrationView.PAGE_SIZE))
                .thenReturn(new ClientSitePage(List.of(site1, site2), false));

        ClientAdministrationView view = createView();

        @SuppressWarnings("unchecked")
        Grid<ClientCompanyReference> companyGrid = find(view, Grid.class, "hris-company-grid");
        companyGrid.asSingleSelect().setValue(inactiveCompany);

        @SuppressWarnings("unchecked")
        Grid<ClientSiteReference> siteGrid = find(view, Grid.class, "hris-site-grid");
        assertFalse(siteGrid.getClassNames().contains("hris-hidden"));

        // Select stored-active site under inactive company
        siteGrid.asSingleSelect().setValue(site1);

        Button renameSiteBtn = find(view, Button.class, "hris-rename-site-btn");
        assertTrue(renameSiteBtn.isEnabled(), "Rename site must be enabled under inactive company");

        Button toggleSiteBtn = find(view, Button.class, "hris-toggle-site-btn");
        assertTrue(toggleSiteBtn.isEnabled(), "Deactivate site must remain enabled even if company is inactive");
        assertEquals("Deactivate", toggleSiteBtn.getText());

        Span siteActionHint = find(view, Span.class, "hris-site-action-hint");
        assertTrue(siteActionHint.getClassNames().contains("hris-hidden"));

        // Now select stored-inactive site under inactive company
        siteGrid.asSingleSelect().setValue(site2);
        assertTrue(renameSiteBtn.isEnabled());
        assertEquals("Activate", toggleSiteBtn.getText());
        assertFalse(toggleSiteBtn.isEnabled(), "Activate site must be disabled when company is inactive");
        assertFalse(siteActionHint.getClassNames().contains("hris-hidden"));
        assertEquals("Cannot activate site because its company is inactive.", siteActionHint.getText());
    }

    @Test
    void duplicateDisplayNamesAreHandledAsDistinctIdentities() {
        ClientCompanyReference c1 = new ClientCompanyReference(company1Id, "Duplicate Name", true, 1L);
        ClientCompanyReference c2 = new ClientCompanyReference(company2Id, "Duplicate Name", true, 2L);

        when(queries.companies(0, ClientAdministrationView.PAGE_SIZE))
                .thenReturn(new ClientCompanyPage(List.of(c1, c2), false));
        when(queries.sites(company2Id, 0, ClientAdministrationView.PAGE_SIZE))
                .thenReturn(new ClientSitePage(List.of(), false));

        ClientAdministrationView view = createView();

        @SuppressWarnings("unchecked")
        Grid<ClientCompanyReference> companyGrid = find(view, Grid.class, "hris-company-grid");

        // Select the second duplicate-named company
        companyGrid.asSingleSelect().setValue(c2);

        verify(queries).sites(company2Id, 0, ClientAdministrationView.PAGE_SIZE);
        verify(queries, never()).sites(eq(company1Id), anyInt(), anyInt());
    }

    @Test
    void boundedCompanyPagingNavigationWithoutFabricatedTotals() {
        ClientCompanyReference c1 = new ClientCompanyReference(company1Id, "Acme", true, 1L);
        when(queries.companies(0, ClientAdministrationView.PAGE_SIZE))
                .thenReturn(new ClientCompanyPage(List.of(c1), true)); // hasMore = true
        when(queries.companies(ClientAdministrationView.PAGE_SIZE, ClientAdministrationView.PAGE_SIZE))
                .thenReturn(new ClientCompanyPage(List.of(c1), false)); // page 2 hasMore = false

        ClientAdministrationView view = createView();

        Button prevBtn = find(view, Button.class, "hris-company-prev-btn");
        Button nextBtn = find(view, Button.class, "hris-company-next-btn");
        Span pageInfo = find(view, Span.class, "hris-company-page-info");

        assertEquals("Page 1", pageInfo.getText());
        assertFalse(prevBtn.isEnabled());
        assertTrue(nextBtn.isEnabled());

        // Navigate to Page 2
        nextBtn.click();

        verify(queries).companies(ClientAdministrationView.PAGE_SIZE, ClientAdministrationView.PAGE_SIZE);
        assertEquals("Page 2", pageInfo.getText());
        assertTrue(prevBtn.isEnabled());
        assertFalse(nextBtn.isEnabled());

        // Navigate back to Page 1
        prevBtn.click();

        verify(queries, times(2)).companies(0, ClientAdministrationView.PAGE_SIZE);
        assertEquals("Page 1", pageInfo.getText());
        assertFalse(prevBtn.isEnabled());
        assertTrue(nextBtn.isEnabled());
    }

    @Test
    void boundedSitePagingNavigation() {
        ClientCompanyReference c1 = new ClientCompanyReference(company1Id, "Acme", true, 1L);
        ClientSiteReference s1 = new ClientSiteReference(site1Id, company1Id, "Site 1", true, true, 1L);

        when(queries.companies(0, ClientAdministrationView.PAGE_SIZE))
                .thenReturn(new ClientCompanyPage(List.of(c1), false));
        when(queries.sites(company1Id, 0, ClientAdministrationView.PAGE_SIZE))
                .thenReturn(new ClientSitePage(List.of(s1), true));
        when(queries.sites(company1Id, ClientAdministrationView.PAGE_SIZE, ClientAdministrationView.PAGE_SIZE))
                .thenReturn(new ClientSitePage(List.of(s1), false));

        ClientAdministrationView view = createView();

        @SuppressWarnings("unchecked")
        Grid<ClientCompanyReference> companyGrid = find(view, Grid.class, "hris-company-grid");
        companyGrid.asSingleSelect().setValue(c1);

        Button prevBtn = find(view, Button.class, "hris-site-prev-btn");
        Button nextBtn = find(view, Button.class, "hris-site-next-btn");
        Span pageInfo = find(view, Span.class, "hris-site-page-info");

        assertEquals("Page 1", pageInfo.getText());
        assertFalse(prevBtn.isEnabled());
        assertTrue(nextBtn.isEnabled());

        nextBtn.click();

        verify(queries).sites(company1Id, ClientAdministrationView.PAGE_SIZE, ClientAdministrationView.PAGE_SIZE);
        assertEquals("Page 2", pageInfo.getText());
        assertTrue(prevBtn.isEnabled());
        assertFalse(nextBtn.isEnabled());

        prevBtn.click();

        verify(queries, times(2)).sites(company1Id, 0, ClientAdministrationView.PAGE_SIZE);
        assertEquals("Page 1", pageInfo.getText());
    }

    @Test
    void createCompanyDialogSubmitsValidNameAndRefreshes() {
        when(queries.companies(0, ClientAdministrationView.PAGE_SIZE))
                .thenReturn(new ClientCompanyPage(List.of(), false));
        ClientCompanyReference created = new ClientCompanyReference(company1Id, "New Corp", true, 1L);
        when(service.createCompany("New Corp")).thenReturn(created);

        ClientAdministrationView view = createView();

        Button newCompanyBtn = find(view, Button.class, "hris-create-company-btn");
        newCompanyBtn.click();

        Dialog dialog = find(view, Dialog.class, "hris-create-company-dialog");
        assertTrue(dialog.isOpened());

        TextField input = find(dialog, TextField.class, "hris-company-name-input");
        Button saveBtn = find(dialog, Button.class, "hris-save-company-btn");

        // Blank name rejection
        input.setValue("   ");
        saveBtn.click();
        assertTrue(input.isInvalid());
        verify(service, never()).createCompany(anyString());

        // Valid name submission
        input.setValue("New Corp");
        saveBtn.click();

        verify(service).createCompany("New Corp");
        assertFalse(dialog.isOpened());

        Div feedback = find(view, Div.class, "hris-company-feedback");
        assertFalse(feedback.getClassNames().contains("hris-hidden"));
        assertTrue(feedback.getElement().getTextRecursively().contains("New Corp"));
    }

    @Test
    void renameCompanyDialogSubmitsValidNameAndRefreshes() {
        ClientCompanyReference c1 = new ClientCompanyReference(company1Id, "Old Name", true, 1L);
        when(queries.companies(0, ClientAdministrationView.PAGE_SIZE))
                .thenReturn(new ClientCompanyPage(List.of(c1), false));
        ClientCompanyReference renamed = new ClientCompanyReference(company1Id, "Renamed Corp", true, 2L);
        when(service.renameCompany(company1Id, "Renamed Corp", 1L)).thenReturn(renamed);

        ClientAdministrationView view = createView();

        @SuppressWarnings("unchecked")
        Grid<ClientCompanyReference> companyGrid = find(view, Grid.class, "hris-company-grid");
        companyGrid.asSingleSelect().setValue(c1);

        Button renameBtn = find(view, Button.class, "hris-rename-company-btn");
        renameBtn.click();

        Dialog dialog = find(view, Dialog.class, "hris-rename-company-dialog");
        assertTrue(dialog.isOpened());

        TextField input = find(dialog, TextField.class, "hris-rename-company-input");
        assertEquals("Old Name", input.getValue());

        Button saveBtn = find(dialog, Button.class, "hris-save-rename-company-btn");

        // Submitting with identical name shows NO_CHANGE without calling service
        saveBtn.click();
        assertFalse(dialog.isOpened());
        verify(service, never()).renameCompany(any(), any(), anyLong());

        // Re-open and submit new name
        renameBtn.click();
        Dialog dialog2 = find(view, Dialog.class, "hris-rename-company-dialog");
        TextField input2 = find(dialog2, TextField.class, "hris-rename-company-input");
        Button saveBtn2 = find(dialog2, Button.class, "hris-save-rename-company-btn");

        input2.setValue("Renamed Corp");
        saveBtn2.click();

        verify(service).renameCompany(company1Id, "Renamed Corp", 1L);
        assertFalse(dialog2.isOpened());

        Div feedback = find(view, Div.class, "hris-company-feedback");
        assertFalse(feedback.getClassNames().contains("hris-hidden"));
        assertTrue(feedback.getElement().getTextRecursively().contains("Renamed Corp"));
    }

    @Test
    void companyDeactivationShowsWarningAboutSitesAndRefreshesBothSections() {
        ClientCompanyReference c1 = new ClientCompanyReference(company1Id, "Active Corp", true, 1L);
        when(queries.companies(0, ClientAdministrationView.PAGE_SIZE))
                .thenReturn(new ClientCompanyPage(List.of(c1), false));
        when(queries.sites(company1Id, 0, ClientAdministrationView.PAGE_SIZE))
                .thenReturn(new ClientSitePage(List.of(), false));
        ClientCompanyReference deactivated = new ClientCompanyReference(company1Id, "Active Corp", false, 2L);
        when(service.deactivateCompany(company1Id, 1L)).thenReturn(deactivated);

        ClientAdministrationView view = createView();

        @SuppressWarnings("unchecked")
        Grid<ClientCompanyReference> companyGrid = find(view, Grid.class, "hris-company-grid");
        companyGrid.asSingleSelect().setValue(c1);

        Button toggleBtn = find(view, Button.class, "hris-toggle-company-btn");
        assertEquals("Deactivate", toggleBtn.getText());
        toggleBtn.click();

        Dialog dialog = find(view, Dialog.class, "hris-lifecycle-dialog");
        assertTrue(dialog.isOpened());

        Paragraph msg = find(dialog, Paragraph.class, "hris-lifecycle-message");
        assertTrue(msg.getText().contains("effectively inactive"));
        assertTrue(msg.getText().contains("preserve each site's stored active status"));

        Button confirmBtn = find(dialog, Button.class, "hris-confirm-lifecycle-btn");
        confirmBtn.click();

        verify(service).deactivateCompany(company1Id, 1L);
        assertFalse(dialog.isOpened());

        // Must reload companies AND sites
        verify(queries, times(2)).companies(0, ClientAdministrationView.PAGE_SIZE);
        verify(queries, times(2)).sites(company1Id, 0, ClientAdministrationView.PAGE_SIZE);
    }

    @Test
    void createSiteDialogSubmitsValidNameAndRefreshes() {
        ClientCompanyReference c1 = new ClientCompanyReference(company1Id, "Active Corp", true, 1L);
        when(queries.companies(0, ClientAdministrationView.PAGE_SIZE))
                .thenReturn(new ClientCompanyPage(List.of(c1), false));
        when(queries.sites(company1Id, 0, ClientAdministrationView.PAGE_SIZE))
                .thenReturn(new ClientSitePage(List.of(), false));
        ClientSiteReference siteCreated = new ClientSiteReference(site1Id, company1Id, "New Site", true, true, 1L);
        when(service.createSite(company1Id, "New Site")).thenReturn(siteCreated);

        ClientAdministrationView view = createView();

        @SuppressWarnings("unchecked")
        Grid<ClientCompanyReference> companyGrid = find(view, Grid.class, "hris-company-grid");
        companyGrid.asSingleSelect().setValue(c1);

        Button createSiteBtn = find(view, Button.class, "hris-create-site-btn");
        createSiteBtn.click();

        Dialog dialog = find(view, Dialog.class, "hris-create-site-dialog");
        assertTrue(dialog.isOpened());

        TextField input = find(dialog, TextField.class, "hris-site-name-input");
        Button saveBtn = find(dialog, Button.class, "hris-save-site-btn");

        input.setValue("New Site");
        saveBtn.click();

        verify(service).createSite(company1Id, "New Site");
        assertFalse(dialog.isOpened());

        Div feedback = find(view, Div.class, "hris-site-feedback");
        assertFalse(feedback.getClassNames().contains("hris-hidden"));
        assertTrue(feedback.getElement().getTextRecursively().contains("New Site"));
    }

    @Test
    void renameSiteDialogSubmitsValidNameAndRefreshes() {
        ClientCompanyReference c1 = new ClientCompanyReference(company1Id, "Active Corp", true, 1L);
        ClientSiteReference s1 = new ClientSiteReference(site1Id, company1Id, "Old Site", true, true, 1L);

        when(queries.companies(0, ClientAdministrationView.PAGE_SIZE))
                .thenReturn(new ClientCompanyPage(List.of(c1), false));
        when(queries.sites(company1Id, 0, ClientAdministrationView.PAGE_SIZE))
                .thenReturn(new ClientSitePage(List.of(s1), false));
        ClientSiteReference renamed = new ClientSiteReference(site1Id, company1Id, "Renamed Site", true, true, 2L);
        when(service.renameSite(site1Id, "Renamed Site", 1L)).thenReturn(renamed);

        ClientAdministrationView view = createView();

        @SuppressWarnings("unchecked")
        Grid<ClientCompanyReference> companyGrid = find(view, Grid.class, "hris-company-grid");
        companyGrid.asSingleSelect().setValue(c1);

        @SuppressWarnings("unchecked")
        Grid<ClientSiteReference> siteGrid = find(view, Grid.class, "hris-site-grid");
        siteGrid.asSingleSelect().setValue(s1);

        Button renameSiteBtn = find(view, Button.class, "hris-rename-site-btn");
        renameSiteBtn.click();

        Dialog dialog = find(view, Dialog.class, "hris-rename-site-dialog");
        assertTrue(dialog.isOpened());

        TextField input = find(dialog, TextField.class, "hris-rename-site-input");
        assertEquals("Old Site", input.getValue());

        Button saveBtn = find(dialog, Button.class, "hris-save-rename-site-btn");
        input.setValue("Renamed Site");
        saveBtn.click();

        verify(service).renameSite(site1Id, "Renamed Site", 1L);
        assertFalse(dialog.isOpened());

        Div feedback = find(view, Div.class, "hris-site-feedback");
        assertFalse(feedback.getClassNames().contains("hris-hidden"));
        assertTrue(feedback.getElement().getTextRecursively().contains("Renamed Site"));
    }

    @Test
    void siteLifecycleDeactivateAndActivateSubmissions() {
        ClientCompanyReference c1 = new ClientCompanyReference(company1Id, "Active Corp", true, 1L);
        ClientSiteReference activeSite = new ClientSiteReference(site1Id, company1Id, "Site 1", true, true, 1L);
        ClientSiteReference inactiveSite = new ClientSiteReference(site2Id, company1Id, "Site 2", false, true, 1L);

        when(queries.companies(0, ClientAdministrationView.PAGE_SIZE))
                .thenReturn(new ClientCompanyPage(List.of(c1), false));
        when(queries.sites(company1Id, 0, ClientAdministrationView.PAGE_SIZE))
                .thenReturn(new ClientSitePage(List.of(activeSite, inactiveSite), false));

        ClientAdministrationView view = createView();

        @SuppressWarnings("unchecked")
        Grid<ClientCompanyReference> companyGrid = find(view, Grid.class, "hris-company-grid");
        companyGrid.asSingleSelect().setValue(c1);

        @SuppressWarnings("unchecked")
        Grid<ClientSiteReference> siteGrid = find(view, Grid.class, "hris-site-grid");

        // Deactivate activeSite
        siteGrid.asSingleSelect().setValue(activeSite);
        Button toggleBtn = find(view, Button.class, "hris-toggle-site-btn");
        assertEquals("Deactivate", toggleBtn.getText());
        toggleBtn.click();

        Dialog dialog = find(view, Dialog.class, "hris-lifecycle-dialog");
        find(dialog, Button.class, "hris-confirm-lifecycle-btn").click();
        verify(service).deactivateSite(site1Id, 1L);

        // Activate inactiveSite
        siteGrid.asSingleSelect().setValue(inactiveSite);
        assertEquals("Activate", toggleBtn.getText());
        toggleBtn.click();

        Dialog dialog2 = find(view, Dialog.class, "hris-lifecycle-dialog");
        find(dialog2, Button.class, "hris-confirm-lifecycle-btn").click();
        verify(service).activateSite(site2Id, 1L);
    }

    @Test
    void staleVersionErrorShowsSafeMessageAndRefreshesView() {
        ClientCompanyReference c1 = new ClientCompanyReference(company1Id, "Acme", true, 1L);
        when(queries.companies(0, ClientAdministrationView.PAGE_SIZE))
                .thenReturn(new ClientCompanyPage(List.of(c1), false));
        when(service.renameCompany(company1Id, "New Name", 1L))
                .thenThrow(new ClientManagementException(ClientManagementException.Reason.STALE_VERSION));

        ClientAdministrationView view = createView();

        @SuppressWarnings("unchecked")
        Grid<ClientCompanyReference> companyGrid = find(view, Grid.class, "hris-company-grid");
        companyGrid.asSingleSelect().setValue(c1);

        Button renameBtn = find(view, Button.class, "hris-rename-company-btn");
        renameBtn.click();

        Dialog dialog = find(view, Dialog.class, "hris-rename-company-dialog");
        TextField input = find(dialog, TextField.class, "hris-rename-company-input");
        input.setValue("New Name");
        Button saveBtn = find(dialog, Button.class, "hris-save-rename-company-btn");
        saveBtn.click();

        assertFalse(dialog.isOpened());

        Div feedback = find(view, Div.class, "hris-company-feedback");
        assertFalse(feedback.getClassNames().contains("hris-hidden"));
        assertTrue(feedback.getClassNames().contains("hris-feedback-error"));
        assertTrue(feedback.getElement().getTextRecursively().contains("modified by another operation"));

        // Must refresh companies
        verify(queries, times(2)).companies(0, ClientAdministrationView.PAGE_SIZE);
    }

    @Test
    void fixedReasonUserMessagesCoverAllEnumsSafelyWithoutInfrastructureDetails() {
        for (ClientManagementException.Reason reason : ClientManagementException.Reason.values()) {
            ClientManagementException ex = new ClientManagementException(reason);
            String message = ClientAdministrationView.userMessageFor(ex);
            assertNotNull(message);
            assertFalse(message.isBlank());
            assertFalse(message.contains("SQL"));
            assertFalse(message.contains("Exception"));
            assertFalse(message.contains("stack"));
            assertFalse(message.contains("database"));
        }
    }

    @Test
    void invalidPageErrorRecoversToFirstPage() {
        when(queries.companies(ClientAdministrationView.PAGE_SIZE, ClientAdministrationView.PAGE_SIZE))
                .thenThrow(new ClientManagementException(ClientManagementException.Reason.INVALID_PAGE));
        when(queries.companies(0, ClientAdministrationView.PAGE_SIZE))
                .thenReturn(new ClientCompanyPage(List.of(new ClientCompanyReference(company1Id, "C", true, 1L)), true));

        ClientAdministrationView view = createView();

        Button nextBtn = find(view, Button.class, "hris-company-next-btn");
        assertTrue(nextBtn.isEnabled());
        nextBtn.click();

        // Throws INVALID_PAGE on offset PAGE_SIZE, recovers to offset 0
        Span pageInfo = find(view, Span.class, "hris-company-page-info");
        assertEquals("Page 1", pageInfo.getText());
        Div feedback = find(view, Div.class, "hris-company-feedback");
        assertTrue(feedback.getElement().getTextRecursively().contains("Reset to the first page"));
    }

    @Test
    void persistentCompanyQueryFailureDoesNotRecursivelyRetryAndRendersSafeFeedback() {
        when(queries.companies(0, ClientAdministrationView.PAGE_SIZE))
                .thenThrow(new ClientManagementException(ClientManagementException.Reason.PERSISTENCE_FAILED));

        ClientAdministrationView view = createView();

        // Must query exactly once; zero recursive retry
        verify(queries, times(1)).companies(0, ClientAdministrationView.PAGE_SIZE);

        Div feedback = find(view, Div.class, "hris-company-feedback");
        assertFalse(feedback.getClassNames().contains("hris-hidden"));
        assertTrue(feedback.getClassNames().contains("hris-feedback-error"));
        String text = feedback.getElement().getTextRecursively();
        assertTrue(text.contains("The operation could not be completed"));
        assertFalse(text.contains("SQL"));
        assertFalse(text.contains("Exception"));

        Button prevBtn = find(view, Button.class, "hris-company-prev-btn");
        Button nextBtn = find(view, Button.class, "hris-company-next-btn");
        assertFalse(prevBtn.isEnabled());
        assertFalse(nextBtn.isEnabled());
        Span pageInfo = find(view, Span.class, "hris-company-page-info");
        assertEquals("Page 1", pageInfo.getText());
    }

    @Test
    void persistentSiteQueryFailureDoesNotRecursivelyRetryAndRendersSafeFeedback() {
        ClientCompanyReference c1 = new ClientCompanyReference(company1Id, "Alpha Corp", true, 1L);
        when(queries.companies(0, ClientAdministrationView.PAGE_SIZE))
                .thenReturn(new ClientCompanyPage(List.of(c1), false));
        when(queries.sites(company1Id, 0, ClientAdministrationView.PAGE_SIZE))
                .thenThrow(new ClientManagementException(ClientManagementException.Reason.PERSISTENCE_FAILED));

        ClientAdministrationView view = createView();

        @SuppressWarnings("unchecked")
        Grid<ClientCompanyReference> companyGrid = find(view, Grid.class, "hris-company-grid");
        companyGrid.asSingleSelect().setValue(c1);

        // Must query sites exactly once; zero recursive retry
        verify(queries, times(1)).sites(company1Id, 0, ClientAdministrationView.PAGE_SIZE);

        Div feedback = find(view, Div.class, "hris-site-feedback");
        assertFalse(feedback.getClassNames().contains("hris-hidden"));
        assertTrue(feedback.getClassNames().contains("hris-feedback-error"));
        String text = feedback.getElement().getTextRecursively();
        assertTrue(text.contains("The operation could not be completed"));
        assertFalse(text.contains("SQL"));
        assertFalse(text.contains("Exception"));

        Button prevBtn = find(view, Button.class, "hris-site-prev-btn");
        Button nextBtn = find(view, Button.class, "hris-site-next-btn");
        assertFalse(prevBtn.isEnabled());
        assertFalse(nextBtn.isEnabled());
        Span pageInfo = find(view, Span.class, "hris-site-page-info");
        assertEquals("Page 1", pageInfo.getText());
    }

    @Test
    void invalidPageRecoveryStopsIfRecoveryQueryAlsoFails() {
        when(queries.companies(0, ClientAdministrationView.PAGE_SIZE))
                .thenReturn(new ClientCompanyPage(List.of(new ClientCompanyReference(company1Id, "C", true, 1L)), true));
        when(queries.companies(ClientAdministrationView.PAGE_SIZE, ClientAdministrationView.PAGE_SIZE))
                .thenThrow(new ClientManagementException(ClientManagementException.Reason.INVALID_PAGE));

        ClientAdministrationView view = createView();

        // Prepare page 0 query to fail when recovery attempts to reload it
        when(queries.companies(0, ClientAdministrationView.PAGE_SIZE))
                .thenThrow(new ClientManagementException(ClientManagementException.Reason.PERSISTENCE_FAILED));

        Button nextBtn = find(view, Button.class, "hris-company-next-btn");
        assertTrue(nextBtn.isEnabled());
        nextBtn.click();

        // Exactly 1 initial load + 1 page-next attempt + 1 recovery query = 3 queries max, no loop
        verify(queries, times(2)).companies(0, ClientAdministrationView.PAGE_SIZE);
        verify(queries, times(1)).companies(ClientAdministrationView.PAGE_SIZE, ClientAdministrationView.PAGE_SIZE);

        Div feedback = find(view, Div.class, "hris-company-feedback");
        assertFalse(feedback.getClassNames().contains("hris-hidden"));
        assertTrue(feedback.getClassNames().contains("hris-feedback-error"));

        Button prevBtn = find(view, Button.class, "hris-company-prev-btn");
        assertFalse(prevBtn.isEnabled());
        assertFalse(nextBtn.isEnabled());
    }

    @Test
    void mutationRefreshFailureTerminatesSafelyWithoutRecursiveRetry() {
        ClientCompanyReference c1 = new ClientCompanyReference(company1Id, "Acme", true, 1L);
        when(queries.companies(0, ClientAdministrationView.PAGE_SIZE))
                .thenReturn(new ClientCompanyPage(List.of(c1), false));
        when(service.renameCompany(company1Id, "New Name", 1L))
                .thenThrow(new ClientManagementException(ClientManagementException.Reason.STALE_VERSION));

        ClientAdministrationView view = createView();

        // Make subsequent refresh query fail
        when(queries.companies(0, ClientAdministrationView.PAGE_SIZE))
                .thenThrow(new ClientManagementException(ClientManagementException.Reason.PERSISTENCE_FAILED));

        @SuppressWarnings("unchecked")
        Grid<ClientCompanyReference> companyGrid = find(view, Grid.class, "hris-company-grid");
        companyGrid.asSingleSelect().setValue(c1);

        Button renameBtn = find(view, Button.class, "hris-rename-company-btn");
        renameBtn.click();

        Dialog dialog = find(view, Dialog.class, "hris-rename-company-dialog");
        TextField input = find(dialog, TextField.class, "hris-rename-company-input");
        input.setValue("New Name");
        Button saveBtn = find(dialog, Button.class, "hris-save-rename-company-btn");
        saveBtn.click();

        // Exactly 1 initial load + 1 mutation refresh attempt = 2 invocations total
        verify(queries, times(2)).companies(0, ClientAdministrationView.PAGE_SIZE);

        Div feedback = find(view, Div.class, "hris-company-feedback");
        assertFalse(feedback.getClassNames().contains("hris-hidden"));
        assertTrue(feedback.getClassNames().contains("hris-feedback-error"));
    }

    private static <T extends Component> T find(Component root, Class<T> type, String id) {
        return descendants(root).filter(type::isInstance).map(type::cast)
                .filter(c -> c.getId().equals(Optional.of(id))).findFirst().orElseThrow();
    }

    private static Stream<Component> descendants(Component root) {
        return Stream.concat(
                Stream.of(root),
                elementDescendants(root.getElement())
                        .flatMap(el -> el.getComponent().stream())
        );
    }

    private static Stream<com.vaadin.flow.dom.Element> elementDescendants(com.vaadin.flow.dom.Element element) {
        return element.getChildren().flatMap(child -> Stream.concat(Stream.of(child), elementDescendants(child)));
    }
}
