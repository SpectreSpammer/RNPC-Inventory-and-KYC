package com.rnpc.inventory.controller;

import com.rnpc.inventory.entity.Client;
import com.rnpc.inventory.entity.RepairRecord;
import com.rnpc.inventory.service.ClientService;
import com.rnpc.inventory.service.NotificationService;
import com.rnpc.inventory.service.RepairRecordService;
import com.rnpc.inventory.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Email, address and brand are optional (batch 5); a contact number in any format must reach an
 * existing client through the shared PhoneNumbers matcher (batch 2) instead of creating a duplicate.
 * Real TicketController and validation, mocked services; views are not rendered (only their names
 * are checked), so this needs no Spring context, templates or database.
 */
class TicketControllerTest {

    private final ClientService clientService = mock(ClientService.class);
    private final RepairRecordService repairRecordService = mock(RepairRecordService.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final UserService userService = mock(UserService.class);
    private final MockMvc mvc;

    TicketControllerTest() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        InternalResourceViewResolver resolver = new InternalResourceViewResolver("/views/", ".html");
        // No Authentication is resolved in a standalone MockMvc setup (null by default), so
        // TicketController's receivedByEmployeeId lookup short-circuits before touching
        // userService here - these tests don't need to stub it.
        mvc = MockMvcBuilders.standaloneSetup(new TicketController(clientService, repairRecordService, notificationService, userService))
                .setValidator(validator).setViewResolvers(resolver).build();
    }

    private static Client client(long id, String fullName) {
        Client c = new Client();
        c.setClientId(id);
        c.setFullName(fullName);
        return c;
    }

    private static RepairRecord repair(long id, String jobOrderNumber) {
        RepairRecord r = new RepairRecord();
        r.setRepairId(id);
        r.setJobOrderNumber(jobOrderNumber);
        return r;
    }

    private MockHttpServletRequestBuilder validForm() {
        return validForm("Walk-in Customer", "0999 000 0000");
    }

    private MockHttpServletRequestBuilder validForm(String fullName, String contactNumber) {
        return post("/ticket/create")
                .param("fullName", fullName)
                .param("contactNumber", contactNumber)
                .param("deviceType", "Laptop")
                .param("modelName", "Aspire 5")
                .param("issueDescription", "Won't turn on");
    }

    // ---- Email, address and brand: optional (batch 5) ---------------------------------------------

    @Test
    void creatingATicketWithNoEmailAddressOrBrandIsSaved() throws Exception {
        when(clientService.findByContactNumber(any())).thenReturn(Optional.empty());
        Client newClient = client(5L, "Walk-in Customer");
        when(clientService.saveClient(any())).thenReturn(newClient);
        when(repairRecordService.createFromTicket(eq(newClient), any(), any())).thenReturn(repair(1L, "SUP-00001"));

        mvc.perform(validForm())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ticket/1"));
        verify(clientService).saveClient(any());
    }

    @Test
    void aMalformedEmailIsStillRejectedEvenThoughEmailIsOptional() throws Exception {
        mvc.perform(validForm().param("email", "not-an-email"))
                .andExpect(status().isOk())
                .andExpect(view().name("tickets/ticketCreate"))
                .andExpect(model().attributeHasFieldErrorCode("ticketDto", "email", "Email"));
        verify(clientService, never()).saveClient(any());
    }

    @Test
    void anAddressOverTheLimitIsStillRejectedEvenThoughAddressIsOptional() throws Exception {
        mvc.perform(validForm().param("address", "x".repeat(501)))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrorCode("ticketDto", "address", "Size"));
        verify(clientService, never()).saveClient(any());
    }

    // ---- Cross-format client match (batch 2's shared matcher) -------------------------------------

    @Test
    void aContactNumberInADifferentFormatReachesTheSameExistingClient() throws Exception {
        // findByContactNumber already normalises through PhoneNumbers.matchKey - this exercises
        // that a ticket submitted with an unformatted number still reuses the existing client
        // rather than creating a duplicate.
        Client existing = client(9L, "Nand Test");
        when(clientService.findByContactNumber("09172345678")).thenReturn(Optional.of(existing));
        when(repairRecordService.createFromTicket(eq(existing), any(), any())).thenReturn(repair(2L, "SUP-00002"));

        mvc.perform(validForm("Typed Differently", "09172345678"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ticket/2"));
        verify(clientService, never()).saveClient(any());
        verify(repairRecordService).createFromTicket(eq(existing), any(), any());
    }

    // ---- A matched client keeps its own data; only blanks are filled in (batch 5) ------------------

    @Test
    void aMatchedClientIsOfferedTheTypedDetailsToFillInAnyBlanks() throws Exception {
        // The actual "keep what's there, fill only blanks" rule is ClientService's own
        // (see ClientServiceTest.fillMissingDetailsNeverOverwritesAnExistingValue) - this just
        // confirms the controller hands the typed values to it rather than to saveClient.
        Client existing = client(9L, "Nand Test");
        when(clientService.findByContactNumber("0999 000 0000")).thenReturn(Optional.of(existing));
        when(repairRecordService.createFromTicket(eq(existing), any(), any())).thenReturn(repair(3L, "SUP-00003"));

        mvc.perform(validForm().param("email", "typed@example.com").param("address", "Typed Address"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ticket/3"));
        verify(clientService, never()).saveClient(any());
        verify(clientService).fillMissingDetails(existing, "Walk-in Customer", "typed@example.com", "Typed Address");
    }
}
