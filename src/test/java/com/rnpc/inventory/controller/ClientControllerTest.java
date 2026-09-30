package com.rnpc.inventory.controller;

import com.rnpc.inventory.entity.Client;
import com.rnpc.inventory.service.ClientService;
import com.rnpc.inventory.service.NotificationService;
import com.rnpc.inventory.service.RepairRecordService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * The client forms refuse a number that belongs to a different client, naming that client. Real
 * ClientController and validation, mocked services; views are not rendered (only their names are
 * checked), so this needs no Spring context, templates or database.
 */
class ClientControllerTest {

    private final ClientService service = mock(ClientService.class);
    private final MockMvc mvc;

    ClientControllerTest() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        InternalResourceViewResolver resolver = new InternalResourceViewResolver("/views/", ".html");
        mvc = MockMvcBuilders.standaloneSetup(new ClientController(service, mock(RepairRecordService.class), mock(NotificationService.class)))
                .setValidator(validator).setViewResolvers(resolver).build();
    }

    private static Client nand() {
        Client c = new Client();
        c.setClientId(12L);
        c.setFullName("Nand Test");
        c.setContactNumber("0917 234 5678");
        return c;
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder form(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request, String number) {
        return request.param("fullName", "Someone Else").param("contactNumber", number)
                .param("email", "x@example.com").param("address", "Somewhere");
    }

    @Test
    void creatingAClientWithAnExistingNumberInAnotherFormatIsRejectedNamingTheClient() throws Exception {
        when(service.findOtherWithContactNumber("+63 917-234-5678", null)).thenReturn(Optional.of(nand()));

        mvc.perform(form(post("/client/create"), "+63 917-234-5678"))
                .andExpect(status().isOk())
                .andExpect(view().name("clients/clientCreate"))
                .andExpect(model().attributeHasFieldErrors("clientDto", "contactNumber"))
                .andExpect(model().attributeHasFieldErrorCode("clientDto", "contactNumber", "duplicate"));
        verify(service, never()).saveClient(any());
    }

    @Test
    void theRejectionMessageNamesTheExistingClientAndTheirId() throws Exception {
        when(service.findOtherWithContactNumber("09172345678", null)).thenReturn(Optional.of(nand()));

        String message = mvc.perform(form(post("/client/create"), "09172345678")).andReturn()
                .getModelAndView().getModel().get("org.springframework.validation.BindingResult.clientDto")
                .toString();
        org.junit.jupiter.api.Assertions.assertTrue(message.contains("Nand Test"), message);
        org.junit.jupiter.api.Assertions.assertTrue(message.contains("client #12"), message);
    }

    @Test
    void creatingAClientWithAFreeNumberIsSavedAndRedirects() throws Exception {
        when(service.findOtherWithContactNumber(any(), eq(null))).thenReturn(Optional.empty());

        mvc.perform(form(post("/client/create"), "0999 000 0000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/client"));
        verify(service).saveClient(any());
    }

    @Test
    void editingAClientToAnotherClientsNumberIsRejected() throws Exception {
        when(service.findOtherWithContactNumber("09172345678", 2L)).thenReturn(Optional.of(nand()));
        when(service.getClientById(2L)).thenReturn(new Client());

        mvc.perform(form(put("/client/update/2"), "09172345678"))
                .andExpect(status().isOk())
                .andExpect(view().name("clients/clientEdit"))
                .andExpect(model().attributeHasFieldErrorCode("clientDto", "contactNumber", "duplicate"));
        verify(service, never()).updateClient(any(), any());
    }

    @Test
    void editingAClientAndKeepingTheirOwnNumberIsAllowed() throws Exception {
        // The service excludes the client being edited, so it reports no clash.
        when(service.findOtherWithContactNumber("0917 234 5678", 12L)).thenReturn(Optional.empty());

        mvc.perform(form(put("/client/update/12"), "0917 234 5678"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/client"));
        verify(service).updateClient(eq(12L), any());
    }

    @Test
    void aNumberThatFailsValidationDoesNotAlsoGetADuplicateError() throws Exception {
        mvc.perform(form(post("/client/create"), "abc"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrorCode("clientDto", "contactNumber", "Pattern"));
        verify(service, never()).findOtherWithContactNumber(any(), any());
    }

    // ---- Email and address: optional (batch 4) --------------------------------------------------

    @Test
    void creatingAClientWithNoEmailOrAddressIsSaved() throws Exception {
        when(service.findOtherWithContactNumber(any(), eq(null))).thenReturn(Optional.empty());

        mvc.perform(post("/client/create")
                        .param("fullName", "Someone Else")
                        .param("contactNumber", "0999 000 0000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/client"));
        verify(service).saveClient(any());
    }

    @Test
    void aMalformedEmailIsStillRejectedEvenThoughEmailIsOptional() throws Exception {
        mvc.perform(post("/client/create")
                        .param("fullName", "Someone Else")
                        .param("contactNumber", "0999 000 0000")
                        .param("email", "not-an-email"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrorCode("clientDto", "email", "Email"));
        verify(service, never()).saveClient(any());
    }

    // ---- Photo validation surfaced as a field error, not a 500 ------------------------------------

    @Test
    void aRejectedPhotoOnCreateReRendersTheFormWithoutRedirecting() throws Exception {
        when(service.findOtherWithContactNumber(any(), eq(null))).thenReturn(Optional.empty());
        when(service.saveClient(any())).thenThrow(new IllegalArgumentException("Choose a JPG or PNG no larger than 5 MB."));

        mvc.perform(form(post("/client/create"), "0999 000 0000"))
                .andExpect(status().isOk())
                .andExpect(view().name("clients/clientCreate"))
                .andExpect(model().attributeHasFieldErrorCode("clientDto", "imageFile", "invalid"));
    }

    @Test
    void aRejectedPhotoOnUpdateReRendersTheEditFormWithoutRedirecting() throws Exception {
        when(service.findOtherWithContactNumber("0917 234 5678", 12L)).thenReturn(Optional.empty());
        when(service.getClientById(12L)).thenReturn(nand());
        when(service.updateClient(eq(12L), any())).thenThrow(new IllegalArgumentException("Only JPG and PNG images are supported."));

        mvc.perform(form(put("/client/update/12"), "0917 234 5678"))
                .andExpect(status().isOk())
                .andExpect(view().name("clients/clientEdit"))
                .andExpect(model().attributeHasFieldErrorCode("clientDto", "imageFile", "invalid"));
    }

    // ---- Remove photo: must be a 303, not a bare 302 ----------------------------------------------
    //
    // removePhoto is called by fetch (fragments/parts-form :: remove-photo-script), not a browser
    // form submission, so the actual wire-level HTTP method is a real DELETE. Browsers preserve the
    // original method when following a 301/302 for anything other than POST, so a bare
    // "redirect:/client/edit/12" (a 302) would have the browser re-send the follow-up as DELETE
    // against a GET-only route, 405ing even though the removal itself succeeded. A 303 See Other is
    // always followed as GET, regardless of the original method - see util.Redirects.

    @Test
    void removePhotoRedirectsWithSeeOther() throws Exception {
        mvc.perform(delete("/client/removePhoto/12"))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", "/client/edit/12"));
        verify(service).removePhoto(12L);
    }
}
