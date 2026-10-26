package com.rnpc.inventory.controller;

import com.rnpc.inventory.dto.AdminRepairView;
import com.rnpc.inventory.entity.Client;
import com.rnpc.inventory.entity.RepairRecord;
import com.rnpc.inventory.entity.User;
import com.rnpc.inventory.service.ClientService;
import com.rnpc.inventory.service.NotificationService;
import com.rnpc.inventory.service.RepairRecordService;
import com.rnpc.inventory.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.text.SimpleDateFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Which template and model each audience gets (repairs batch A1): an admin gets the admin list on
 * /repair and the same list in ticket mode on /ticket/view, a customer still gets the customer list
 * on /repair and is sent from /ticket/view to it. Real controllers, mocked services; views are not
 * rendered (CustomerRepairTemplatesTest / AdminRepairTemplatesTest render the templates), so no
 * Spring context, templates or database.
 */
class AdminRepairPagesControllerTest {

    private final RepairRecordService repairService = mock(RepairRecordService.class);
    private final ClientService clientService = mock(ClientService.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final MockMvc mvc;

    private final Authentication admin = new UsernamePasswordAuthenticationToken(
            "admin@example.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    private final Authentication customer = new UsernamePasswordAuthenticationToken(
            "customer@example.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));

    AdminRepairPagesControllerTest() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        InternalResourceViewResolver resolver = new InternalResourceViewResolver("/views/", ".html");
        mvc = MockMvcBuilders.standaloneSetup(
                        new RepairRecordController(repairService, clientService, notificationService, validator),
                        new TicketController(clientService, repairService, notificationService, mock(UserService.class)))
                .setValidator(validator).setViewResolvers(resolver).build();
    }

    private RepairRecord repair(long id, String jobOrder, String clientName) throws Exception {
        Client client = new Client();
        client.setClientId(9L);
        client.setFullName(clientName);
        client.setContactNumber("0999 123 4567");
        RepairRecord r = new RepairRecord();
        r.setRepairId(id);
        r.setJobOrderNumber(jobOrder);
        r.setClient(client);
        r.setDeviceType("Laptop");
        r.setIssueDescription("no display");
        r.setRepairDate(new SimpleDateFormat("yyyy-MM-dd").parse("2026-10-03"));
        r.setStatus(RepairRecord.RepairStatus.PENDING);
        r.setRemarks("INTERNAL-NOTE-XYZ");
        return r;
    }

    @SuppressWarnings("unchecked")
    private static List<AdminRepairView> adminViews(MvcResult result) {
        return (List<AdminRepairView>) result.getModelAndView().getModel().get("repairViews");
    }

    // ---- /repair ----------------------------------------------------------------------------------

    @Test
    void anAdminOnRepairGetsTheAdminTemplateInRepairMode() throws Exception {
        when(repairService.getAllRepairRecords()).thenReturn(List.of(repair(1L, "JO-00001", "Nand Test")));

        MvcResult result = mvc.perform(get("/repair").principal(admin))
                .andExpect(status().isOk())
                .andExpect(view().name("repairs/repairIndex"))
                .andExpect(model().attribute("ticketMode", false))
                .andExpect(model().attribute("isAdmin", true))
                .andReturn();

        assertEquals(1, adminViews(result).size());
        assertEquals("Nand Test", adminViews(result).get(0).getClientName());
    }

    @Test
    void anAdminOnRepairWithAClientFilterGetsThatClientsRepairsAndTheClient() throws Exception {
        Client client = new Client();
        client.setClientId(9L);
        client.setFullName("Nand Test");
        when(clientService.getClientById(9L)).thenReturn(client);
        when(repairService.getRepairRecordsByClient(9L)).thenReturn(List.of(repair(1L, "JO-00001", "Nand Test")));

        mvc.perform(get("/repair").param("clientId", "9").principal(admin))
                .andExpect(view().name("repairs/repairIndex"))
                .andExpect(model().attribute("client", client))
                .andExpect(model().attribute("clientId", 9L));
        verify(repairService, never()).getAllRepairRecords();
    }

    @Test
    void aCustomerOnRepairStillGetsTheCustomerTemplate() throws Exception {
        when(repairService.getRepairRecordsForUser("customer@example.com")).thenReturn(List.of());

        mvc.perform(get("/repair").principal(customer))
                .andExpect(view().name("repairs/customerRepairIndex"))
                .andExpect(model().attributeExists("repairViews"));
    }

    // ---- /ticket/view -----------------------------------------------------------------------------

    @Test
    void anAdminOnTicketViewGetsTheSameTemplateInTicketMode() throws Exception {
        when(repairService.getAllRepairRecords()).thenReturn(List.of(
                repair(1L, "JO-00001", "Nand Test"), repair(2L, "JO-00002", "robin")));

        MvcResult result = mvc.perform(get("/ticket/view").principal(admin))
                .andExpect(status().isOk())
                .andExpect(view().name("repairs/repairIndex"))
                .andExpect(model().attribute("ticketMode", true))
                .andExpect(model().attribute("isAdmin", true))
                // the shell the page needs for its topbar, which /ticket/view never set before
                .andExpect(model().attribute("currentUsername", "admin@example.com"))
                .andExpect(model().attribute("currentRole", "Admin"))
                .andExpect(model().attributeExists("unreadNotifications", "recentNotifications"))
                .andReturn();

        assertEquals(2, adminViews(result).size());
    }

    @Test
    void ticketViewWithAClientFilterScopesToThatClient() throws Exception {
        Client client = new Client();
        client.setClientId(9L);
        client.setFullName("Nand Test");
        when(clientService.getClientById(9L)).thenReturn(client);
        when(repairService.getRepairRecordsByClient(9L)).thenReturn(List.of(repair(1L, "JO-00001", "Nand Test")));

        mvc.perform(get("/ticket/view").param("clientId", "9").principal(admin))
                .andExpect(model().attribute("ticketMode", true))
                .andExpect(model().attribute("client", client));
        verify(repairService, never()).getAllRepairRecords();
    }

    @Test
    void aCustomerOnTicketViewIsSentToTheirOwnRepairHistoryAndNothingIsLoaded() throws Exception {
        mvc.perform(get("/ticket/view").principal(customer))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/repair"));

        verify(repairService, never()).getAllRepairRecords();
        verify(repairService, never()).getRepairRecordsByClient(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void aSignedOutVisitorOnTicketViewIsSentToRepairToo() throws Exception {
        mvc.perform(get("/ticket/view"))
                .andExpect(redirectedUrl("/repair"));
        verify(repairService, never()).getAllRepairRecords();
    }

    // ---- The details modal ------------------------------------------------------------------------

    @Test
    void anAdminOpeningTheModalGetsTheAdminFragmentAndTheAdminViewOnly() throws Exception {
        RepairRecord r = repair(5L, "JO-00005", "Nand Test");
        when(repairService.getRepairRecordById(5L)).thenReturn(r);

        MvcResult result = mvc.perform(get("/repair/5/modal").principal(admin))
                .andExpect(view().name("repairs/repairDetail :: repairCard"))
                .andExpect(model().attributeDoesNotExist("view"))
                .andReturn();

        AdminRepairView v = (AdminRepairView) result.getModelAndView().getModel().get("adminView");
        assertEquals("INTERNAL-NOTE-XYZ", v.getRemarks().getText());
        assertEquals("0999 123 4567", v.getContact().getText());
    }

    @Test
    void aCustomerOpeningTheirModalNeverGetsTheAdminView() throws Exception {
        RepairRecord r = repair(5L, "JO-00005", "Nand Test");
        User owner = new User();
        owner.setUsername("customer@example.com");
        r.getClient().setUser(owner);
        when(repairService.getRepairRecordById(5L)).thenReturn(r);

        MvcResult result = mvc.perform(get("/repair/5/modal").principal(customer))
                .andExpect(view().name("repairs/repairDetail :: customerRepairCard"))
                .andExpect(model().attributeDoesNotExist("adminView"))
                .andReturn();

        assertTrue(result.getModelAndView().getModel().containsKey("view"));
    }
}
