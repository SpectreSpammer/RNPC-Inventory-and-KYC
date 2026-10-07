package com.rnpc.inventory.controller;

import com.rnpc.inventory.dto.RepairRecordDto;
import com.rnpc.inventory.entity.Client;
import com.rnpc.inventory.entity.RepairRecord;
import com.rnpc.inventory.service.ClientService;
import com.rnpc.inventory.service.NotificationService;
import com.rnpc.inventory.service.RepairRecordService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BindingResult;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.text.SimpleDateFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Brand is optional and the fix is required only for COMPLETED or RELEASED (repairs batch 1a), so a
 * fresh walk-in ticket - no fix, maybe no brand - can be opened and saved from /repair/edit/{id}
 * while it is still PENDING or IN_PROGRESS. Real RepairRecordController and validation, mocked
 * services; views are not rendered, so this needs no Spring context, templates or database.
 */
class RepairRecordControllerTest {

    private final RepairRecordService service = mock(RepairRecordService.class);
    private final ClientService clientService = mock(ClientService.class);
    private final MockMvc mvc;

    private final Authentication admin = new UsernamePasswordAuthenticationToken(
            "admin@example.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

    RepairRecordControllerTest() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        InternalResourceViewResolver resolver = new InternalResourceViewResolver("/views/", ".html");
        mvc = MockMvcBuilders.standaloneSetup(new RepairRecordController(
                        service, clientService, mock(NotificationService.class), validator))
                .setValidator(validator).setViewResolvers(resolver).build();
    }

    /** What createFromTicket leaves behind: no brand, no fix, PENDING, cost 0. */
    private RepairRecord freshTicket() throws Exception {
        Client client = new Client();
        client.setClientId(1L);
        client.setFullName("Walk-in Customer");
        RepairRecord repair = new RepairRecord();
        repair.setRepairId(5L);
        repair.setJobOrderNumber("JO-00005");
        repair.setClient(client);
        repair.setDeviceType("Laptop");
        repair.setModelName("Aspire 5");
        repair.setIssueDescription("no display");
        repair.setRepairDate(new SimpleDateFormat("yyyy-MM-dd").parse("2026-10-02"));
        repair.setStatus(RepairRecord.RepairStatus.PENDING);
        return repair;
    }

    /** The form exactly as an admin would submit it for a fresh ticket: no brand, no fix, cost 0. */
    private MockHttpServletRequestBuilder editForm(String status) {
        return editFormWithCost(status, "0");
    }

    /** As editForm, with the cost posted as given; null leaves the cost field out of the request. */
    private MockHttpServletRequestBuilder editFormWithCost(String status, String cost) {
        MockHttpServletRequestBuilder request = put("/repair/update/5").principal(admin)
                .param("clientId", "1")
                .param("deviceType", "Laptop")
                .param("modelName", "Aspire 5")
                .param("issueDescription", "no display")
                .param("repairDate", "2026-10-02")
                .param("status", status);
        return cost == null ? request : request.param("cost", cost);
    }

    private MockHttpServletRequestBuilder createFormWithCost(String status, String cost) {
        MockHttpServletRequestBuilder request = post("/repair/create").principal(admin)
                .param("clientId", "1")
                .param("deviceType", "Laptop")
                .param("modelName", "Aspire 5")
                .param("issueDescription", "no display")
                .param("repairDate", "2026-10-02")
                .param("status", status);
        return cost == null ? request : request.param("cost", cost);
    }

    private static final String COST_MESSAGE =
            "A cost greater than zero is required once the repair is Completed or Released.";

    private static BindingResult bindingResultOf(MvcResult result) {
        return (BindingResult) result.getModelAndView().getModel()
                .get("org.springframework.validation.BindingResult.repairRecordDto");
    }

    /** Re-renders the edit form with exactly one error on cost - the required-cost message. */
    private void assertCostRejected(String status, String cost) throws Exception {
        when(service.getRepairRecordById(5L)).thenReturn(freshTicket());

        MvcResult result = mvc.perform(editFormWithCost(status, cost).param("fix", "Replaced the display cable"))
                .andExpect(status().isOk())
                .andExpect(view().name("repairs/repairEdit"))
                .andReturn();

        BindingResult errors = bindingResultOf(result);
        assertEquals(1, errors.getFieldErrorCount("cost"), status + " cost=" + cost + ": " + errors.getFieldErrors("cost"));
        assertEquals(COST_MESSAGE, errors.getFieldError("cost").getDefaultMessage());
        assertEquals(1, errors.getErrorCount(), "only the cost should be flagged: " + errors.getAllErrors());
        verify(service, never()).updateRepairRecord(any(), any());
    }

    @Test
    void aFreshTicketOpensInTheEditForm() throws Exception {
        when(service.getRepairRecordById(5L)).thenReturn(freshTicket());

        mvc.perform(get("/repair/edit/5").principal(admin))
                .andExpect(status().isOk())
                .andExpect(view().name("repairs/repairEdit"));
    }

    @Test
    void aFreshTicketSavesAsPendingWithNoFixOrBrand() throws Exception {
        when(service.getRepairRecordById(5L)).thenReturn(freshTicket());

        mvc.perform(editForm("PENDING"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/repair?clientId=1"));
        verify(service).updateRepairRecord(eq(5L), any());
    }

    @Test
    void aFreshTicketSavesAsInProgressWithNoFixOrBrand() throws Exception {
        when(service.getRepairRecordById(5L)).thenReturn(freshTicket());

        mvc.perform(editForm("IN_PROGRESS"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/repair?clientId=1"));
        verify(service).updateRepairRecord(eq(5L), any());
    }

    @Test
    void completedWithoutAFixIsRejected() throws Exception {
        when(service.getRepairRecordById(5L)).thenReturn(freshTicket());

        mvc.perform(editForm("COMPLETED").param("fix", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("repairs/repairEdit"))
                .andExpect(model().attributeHasFieldErrorCode("repairRecordDto", "fix", "NotEmpty"));
        verify(service, never()).updateRepairRecord(any(), any());
    }

    @Test
    void releasedWithoutAFixIsRejected() throws Exception {
        when(service.getRepairRecordById(5L)).thenReturn(freshTicket());

        mvc.perform(editForm("RELEASED"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrorCode("repairRecordDto", "fix", "NotEmpty"));
        verify(service, never()).updateRepairRecord(any(), any());
    }

    @Test
    void completedWithAFixAndACostIsSaved() throws Exception {
        when(service.getRepairRecordById(5L)).thenReturn(freshTicket());

        mvc.perform(editFormWithCost("COMPLETED", "1500").param("fix", "Replaced the display cable"))
                .andExpect(status().is3xxRedirection());
        verify(service).updateRepairRecord(eq(5L), any());
    }

    // ---- Cost must be greater than zero once the repair is Completed or Released -------------------

    @Test
    void completedWithAZeroNegativeOrMissingCostIsRejectedWithTheRequiredCostMessage() throws Exception {
        assertCostRejected("COMPLETED", "0");
        assertCostRejected("COMPLETED", "-5");
        assertCostRejected("COMPLETED", null);
    }

    @Test
    void releasedWithAZeroNegativeOrMissingCostIsRejectedWithTheRequiredCostMessage() throws Exception {
        assertCostRejected("RELEASED", "0");
        assertCostRejected("RELEASED", "-5");
        assertCostRejected("RELEASED", null);
    }

    @Test
    void completedWithAClearedCostIsRejectedWithTheRequiredCostMessageNotAGenericBindingError() throws Exception {
        assertCostRejected("COMPLETED", "");
        assertCostRejected("COMPLETED", "   ");
    }

    @Test
    void releasedWithAClearedCostIsRejectedWithTheRequiredCostMessageNotAGenericBindingError() throws Exception {
        assertCostRejected("RELEASED", "");
        assertCostRejected("RELEASED", "   ");
    }

    @Test
    void releasedWithAPositiveCostIsSaved() throws Exception {
        when(service.getRepairRecordById(5L)).thenReturn(freshTicket());

        mvc.perform(editFormWithCost("RELEASED", "0.01").param("fix", "Replaced the display cable"))
                .andExpect(status().is3xxRedirection());
        verify(service).updateRepairRecord(eq(5L), any());
    }

    @Test
    void pendingInProgressAndCancelledStillSaveWithACostOfZero() throws Exception {
        when(service.getRepairRecordById(5L)).thenReturn(freshTicket());

        for (String open : new String[] {"PENDING", "IN_PROGRESS", "CANCELLED"}) {
            mvc.perform(editFormWithCost(open, "0")).andExpect(status().is3xxRedirection());
        }
        verify(service, times(3)).updateRepairRecord(eq(5L), any());
    }

    @Test
    void aClearedCostOnAnUnfinishedRepairIsLeftAsSpringsOwnBindingError() throws Exception {
        // Deliberately unchanged: only COMPLETED and RELEASED get the required-cost message (see
        // RepairRecordController.initBinder). Documented here so a change is a conscious one.
        when(service.getRepairRecordById(5L)).thenReturn(freshTicket());

        mvc.perform(editFormWithCost("PENDING", ""))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrorCode("repairRecordDto", "cost", "typeMismatch"));
        verify(service, never()).updateRepairRecord(any(), any());
    }

    @Test
    void aNegativeCostIsStillRefusedWhileTheRepairIsNotFinished() throws Exception {
        when(service.getRepairRecordById(5L)).thenReturn(freshTicket());

        MvcResult result = mvc.perform(editFormWithCost("IN_PROGRESS", "-1"))
                .andExpect(status().isOk()).andReturn();

        assertEquals("The cost cannot be negative", bindingResultOf(result).getFieldError("cost").getDefaultMessage());
        verify(service, never()).updateRepairRecord(any(), any());
    }

    @Test
    void aMissingFixAndAMissingCostAreBothFlaggedAndEverythingElseIsKept() throws Exception {
        when(service.getRepairRecordById(5L)).thenReturn(freshTicket());

        MvcResult result = mvc.perform(editFormWithCost("COMPLETED", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("repairs/repairEdit"))
                .andExpect(model().attributeHasFieldErrorCode("repairRecordDto", "fix", "NotEmpty"))
                .andExpect(model().attributeHasFieldErrorCode("repairRecordDto", "cost", "Positive"))
                .andReturn();

        RepairRecordDto kept = (RepairRecordDto) result.getModelAndView().getModel().get("repairRecordDto");
        assertEquals("Aspire 5", kept.getModelName());
        assertEquals("no display", kept.getIssueDescription());
        assertEquals("COMPLETED", kept.getStatus());
    }

    @Test
    void creatingACompletedRepairWithNoCostIsRejectedAndWithACostIsSaved() throws Exception {
        mvc.perform(createFormWithCost("COMPLETED", "0").param("fix", "Replaced the display cable"))
                .andExpect(status().isOk())
                .andExpect(view().name("repairs/repairCreate"))
                .andExpect(model().attributeHasFieldErrorCode("repairRecordDto", "cost", "Positive"));
        mvc.perform(createFormWithCost("RELEASED", "").param("fix", "Replaced the display cable"))
                .andExpect(status().isOk())
                .andExpect(view().name("repairs/repairCreate"))
                .andExpect(model().attributeHasFieldErrorCode("repairRecordDto", "cost", "Positive"));
        verify(service, never()).saveRepairRecord(any());

        mvc.perform(createFormWithCost("COMPLETED", "800").param("fix", "Replaced the display cable"))
                .andExpect(status().is3xxRedirection());
        verify(service).saveRepairRecord(any());
    }

    @Test
    void theModelNameIsStillRequired() throws Exception {
        when(service.getRepairRecordById(5L)).thenReturn(freshTicket());

        // Built from scratch: .param() appends, so overriding modelName on editForm() would post two values.
        mvc.perform(put("/repair/update/5").principal(admin)
                        .param("clientId", "1")
                        .param("deviceType", "Laptop")
                        .param("modelName", "")
                        .param("issueDescription", "no display")
                        .param("cost", "0")
                        .param("repairDate", "2026-10-02")
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrorCode("repairRecordDto", "modelName", "NotEmpty"));
        verify(service, never()).updateRepairRecord(any(), any());
    }

    @Test
    void creatingARepairWithNoFixOrBrandWhilePendingIsSaved() throws Exception {
        mvc.perform(post("/repair/create").principal(admin)
                        .param("clientId", "1")
                        .param("deviceType", "Laptop")
                        .param("modelName", "Aspire 5")
                        .param("issueDescription", "no display")
                        .param("cost", "0")
                        .param("repairDate", "2026-10-02")
                        .param("status", "PENDING"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/repair?clientId=1"));
        verify(service).saveRepairRecord(any());
    }

    @Test
    void theControllerStillChecksAdminItselfAsASecondLayer() throws Exception {
        Authentication customer = new UsernamePasswordAuthenticationToken(
                "customer@example.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        mvc.perform(editForm("PENDING").principal(customer))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/repair"));
        verify(service, never()).updateRepairRecord(any(), any());
    }
}
