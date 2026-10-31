package com.rnpc.inventory.controller;

import com.rnpc.inventory.dto.AdminRepairView;
import com.rnpc.inventory.entity.Client;
import com.rnpc.inventory.entity.RepairRecord;
import com.rnpc.inventory.entity.RepairRecord.RepairStatus;
import com.rnpc.inventory.entity.User;
import com.rnpc.inventory.service.ClientService;
import com.rnpc.inventory.service.NotificationService;
import com.rnpc.inventory.service.RepairRecordService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * GET /client/view/{id} (customers batch C1): what the controller hands the page. Real
 * ClientController, mocked services, views not rendered (the render test does that), so no Spring
 * context, templates or database.
 *
 * Who may reach the route is not decided here - the filter chain does that - and is covered by
 * AdminOnlyCustomerRoutesSecurityTest (anonymous -> /login, customer -> /).
 */
class ClientDetailsControllerTest {

    private final ClientService service = mock(ClientService.class);
    private final RepairRecordService repairs = mock(RepairRecordService.class);
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new ClientController(service, repairs, mock(NotificationService.class)))
            .setViewResolvers(new InternalResourceViewResolver("/views/", ".html")).build();

    private final Authentication admin = new UsernamePasswordAuthenticationToken(
            "admin@example.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

    private static Client client(User user, String image) {
        Client c = new Client();
        c.setClientId(12L);
        c.setFullName("Nand Test");
        c.setUser(user);
        c.setImageFileName(image);
        return c;
    }

    private static User account(String username, String email) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        return u;
    }

    @Test
    void anAdminGets200AndTheDetailsView() throws Exception {
        when(service.getClientById(12L)).thenReturn(client(null, null));

        mvc.perform(get("/client/view/12").principal(admin))
                .andExpect(status().isOk())
                .andExpect(view().name("clients/clientView"))
                .andExpect(model().attributeExists("view", "account", "walkIn", "repairs", "currentUsername"));
    }

    @Test
    void anUnknownIdIsA404AndLoadsNoRepairs() throws Exception {
        when(service.getClientById(99L)).thenThrow(new IllegalArgumentException("Invalid client Id: 99"));

        mvc.perform(get("/client/view/99").principal(admin)).andExpect(status().isNotFound());
        verify(repairs, never()).getRepairRecordsByClient(99L);
    }

    @Test
    void theEditRouteIsUnchangedAndStillThrowsOnAnUnknownId() {
        when(service.getClientById(99L)).thenThrow(new IllegalArgumentException("Invalid client Id: 99"));

        org.junit.jupiter.api.Assertions.assertThrows(Exception.class,
                () -> mvc.perform(get("/client/edit/99").principal(admin)));
    }

    @Test
    void aClientWithNoLinkedUserIsAWalkInWithNoPhoto() throws Exception {
        when(service.getClientById(12L)).thenReturn(client(null, null));

        var model = mvc.perform(get("/client/view/12").principal(admin)).andReturn().getModelAndView().getModel();
        assertEquals(true, model.get("walkIn"));
        assertEquals("Walk-in client", model.get("account"));
        assertNull(model.get("photoFile"));
    }

    @Test
    void aBlankPhotoNameIsTreatedAsNoPhoto() throws Exception {
        when(service.getClientById(12L)).thenReturn(client(null, "  "));

        var model = mvc.perform(get("/client/view/12").principal(admin)).andReturn().getModelAndView().getModel();
        assertNull(model.get("photoFile"));
    }

    @Test
    void aLinkedAccountShowsItsEmailAndTheStoredPhotoName() throws Exception {
        when(service.getClientById(12L)).thenReturn(client(account("nand", "nand@example.com"), "1_id.png"));

        var model = mvc.perform(get("/client/view/12").principal(admin)).andReturn().getModelAndView().getModel();
        assertEquals(false, model.get("walkIn"));
        assertEquals("nand@example.com", model.get("account"));
        assertEquals("1_id.png", model.get("photoFile"));
    }

    @Test
    void aLinkedAccountWithABlankEmailFallsBackToTheUsername() throws Exception {
        when(service.getClientById(12L)).thenReturn(client(account("nand-user", "  "), null));

        var model = mvc.perform(get("/client/view/12").principal(admin)).andReturn().getModelAndView().getModel();
        assertEquals("nand-user", model.get("account"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void theJobOrdersAreTheClientsRepairsAsAdminRepairViews() throws Exception {
        when(service.getClientById(12L)).thenReturn(client(null, null));
        RepairRecord r = new RepairRecord();
        r.setRepairId(5L);
        r.setJobOrderNumber("JO-00005");
        r.setStatus(RepairStatus.COMPLETED);
        r.setCost(2500);
        when(repairs.getRepairRecordsByClient(12L)).thenReturn(List.of(r));

        var model = mvc.perform(get("/client/view/12").principal(admin)).andReturn().getModelAndView().getModel();
        List<AdminRepairView> rows = (List<AdminRepairView>) model.get("repairs");
        assertEquals(1, rows.size());
        assertEquals("JO-00005", rows.get(0).getJobOrderNumber());
        assertEquals("Completed", rows.get(0).getStatusLabel());
        assertTrue(rows.get(0).isCostAssessed());
    }
}
