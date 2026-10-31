package com.rnpc.inventory.controller;

import com.rnpc.inventory.dto.RepairRecordDto;
import com.rnpc.inventory.entity.Client;
import com.rnpc.inventory.entity.RepairRecord;
import com.rnpc.inventory.service.ClientService;
import com.rnpc.inventory.service.NotificationService;
import com.rnpc.inventory.service.RepairRecordService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import static org.junit.jupiter.api.Assertions.assertNull;
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
    void aNonNumericCostIsLeftAsSpringsOwnBindingError() throws Exception {
        // Deliberately unchanged: only a blank cost binds as 0.0 (see RepairRecordController.initBinder);
        // "abc" stays a generic typeMismatch in every status. Documented here so a change is a conscious one.
        when(service.getRepairRecordById(5L)).thenReturn(freshTicket());

        mvc.perform(editFormWithCost("PENDING", "abc"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrorCode("repairRecordDto", "cost", "typeMismatch"));
        mvc.perform(editFormWithCost("COMPLETED", "abc").param("fix", "Replaced the display cable"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrorCode("repairRecordDto", "cost", "typeMismatch"));
        verify(service, never()).updateRepairRecord(any(), any());
    }

    @Test
    void aBlankCostBindsAsZeroAndSavesWhilePendingInProgressOrCancelledOnEdit() throws Exception {
        when(service.getRepairRecordById(5L)).thenReturn(freshTicket());

        for (String open : new String[] {"PENDING", "IN_PROGRESS", "CANCELLED"}) {
            for (String blank : new String[] {"", "   "}) {
                mvc.perform(editFormWithCost(open, blank))
                        .andExpect(status().is3xxRedirection())
                        .andExpect(redirectedUrl("/repair?clientId=1"));
            }
        }
        ArgumentCaptor<RepairRecordDto> saved = ArgumentCaptor.forClass(RepairRecordDto.class);
        verify(service, times(6)).updateRepairRecord(eq(5L), saved.capture());
        for (RepairRecordDto dto : saved.getAllValues()) {
            assertEquals(0.0, dto.getCost(), dto.getStatus());
        }
    }

    @Test
    void aBlankCostBindsAsZeroAndSavesWhilePendingInProgressOrCancelledOnCreate() throws Exception {
        for (String open : new String[] {"PENDING", "IN_PROGRESS", "CANCELLED"}) {
            for (String blank : new String[] {"", "   "}) {
                mvc.perform(createFormWithCost(open, blank))
                        .andExpect(status().is3xxRedirection())
                        .andExpect(redirectedUrl("/repair?clientId=1"));
            }
        }
        ArgumentCaptor<RepairRecordDto> saved = ArgumentCaptor.forClass(RepairRecordDto.class);
        verify(service, times(6)).saveRepairRecord(saved.capture());
        for (RepairRecordDto dto : saved.getAllValues()) {
            assertEquals(0.0, dto.getCost(), dto.getStatus());
        }
    }

    @Test
    void aBlankCostOnACreatedCompletedOrReleasedRepairIsRejectedWithOneCostErrorAndEverythingElseKept() throws Exception {
        for (String finished : new String[] {"COMPLETED", "RELEASED"}) {
            for (String blank : new String[] {"", "   "}) {
                MvcResult result = mvc.perform(createFormWithCost(finished, blank)
                                .param("fix", "Replaced the display cable").param("remarks", "Keep this remark"))
                        .andExpect(status().isOk())
                        .andExpect(view().name("repairs/repairCreate"))
                        .andReturn();

                BindingResult errors = bindingResultOf(result);
                assertEquals(1, errors.getFieldErrorCount("cost"), finished + " [" + blank + "]");
                assertEquals(COST_MESSAGE, errors.getFieldError("cost").getDefaultMessage());
                assertEquals(1, errors.getErrorCount(), "only the cost should be flagged: " + errors.getAllErrors());

                RepairRecordDto kept = (RepairRecordDto) result.getModelAndView().getModel().get("repairRecordDto");
                assertEquals("Aspire 5", kept.getModelName());
                assertEquals("no display", kept.getIssueDescription());
                assertEquals("Replaced the display cable", kept.getFix());
                assertEquals("Keep this remark", kept.getRemarks());
                assertEquals(finished, kept.getStatus());
            }
        }
        verify(service, never()).saveRepairRecord(any());
    }

    @Test
    void aBlankCostOnAnEditedCompletedOrReleasedRepairKeepsEveryOtherEntry() throws Exception {
        when(service.getRepairRecordById(5L)).thenReturn(freshTicket());

        for (String finished : new String[] {"COMPLETED", "RELEASED"}) {
            for (String blank : new String[] {"", "   "}) {
                MvcResult result = mvc.perform(editFormWithCost(finished, blank)
                                .param("fix", "Replaced the display cable").param("remarks", "Keep this remark"))
                        .andExpect(status().isOk())
                        .andReturn();

                RepairRecordDto kept = (RepairRecordDto) result.getModelAndView().getModel().get("repairRecordDto");
                assertEquals("Aspire 5", kept.getModelName());
                assertEquals("no display", kept.getIssueDescription());
                assertEquals("Replaced the display cable", kept.getFix());
                assertEquals("Keep this remark", kept.getRemarks());
                assertEquals(finished, kept.getStatus());
            }
        }
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

    // ---- The model name is required only once the repair is Completed or Released --------------------

    private static final String MODEL_MESSAGE = "The model name is required once the repair is Completed or Released.";

    // Built from scratch: .param() appends, so overriding modelName on editForm() would post two values.
    private MockHttpServletRequestBuilder editFormWithModel(String status, String modelName, String cost) {
        return put("/repair/update/5").principal(admin)
                .param("clientId", "1")
                .param("deviceType", "Laptop")
                .param("modelName", modelName)
                .param("issueDescription", "no display")
                .param("cost", cost)
                .param("repairDate", "2026-10-02")
                .param("status", status);
    }

    private MockHttpServletRequestBuilder createFormWithModel(String status, String modelName, String cost) {
        return post("/repair/create").principal(admin)
                .param("clientId", "1")
                .param("deviceType", "Laptop")
                .param("modelName", modelName)
                .param("issueDescription", "no display")
                .param("cost", cost)
                .param("repairDate", "2026-10-02")
                .param("status", status);
    }

    /** Re-renders the given form with exactly one error, on modelName, carrying the model-name message. */
    private void assertModelRejected(String view, MockHttpServletRequestBuilder request) throws Exception {
        MvcResult result = mvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(view().name(view))
                .andExpect(model().attributeHasFieldErrorCode("repairRecordDto", "modelName", "NotBlank"))
                .andReturn();

        BindingResult errors = bindingResultOf(result);
        assertEquals(MODEL_MESSAGE, errors.getFieldError("modelName").getDefaultMessage());
        assertEquals(1, errors.getErrorCount(), "only the model name should be flagged: " + errors.getAllErrors());
    }

    @Test
    void aBlankModelSavesWhilePendingInProgressOrCancelledOnEdit() throws Exception {
        when(service.getRepairRecordById(5L)).thenReturn(freshTicket());

        for (String open : new String[] {"PENDING", "IN_PROGRESS", "CANCELLED"}) {
            mvc.perform(editFormWithModel(open, "", "0"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/repair?clientId=1"));
        }
        verify(service, times(3)).updateRepairRecord(eq(5L), any());
    }

    @Test
    void aBlankModelSavesWhilePendingInProgressOrCancelledOnCreate() throws Exception {
        for (String open : new String[] {"PENDING", "IN_PROGRESS", "CANCELLED"}) {
            mvc.perform(createFormWithModel(open, "", "0"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/repair?clientId=1"));
        }
        verify(service, times(3)).saveRepairRecord(any());
    }

    @Test
    void aBlankModelIsRejectedOnceCompletedOrReleasedOnEdit() throws Exception {
        when(service.getRepairRecordById(5L)).thenReturn(freshTicket());

        for (String finished : new String[] {"COMPLETED", "RELEASED"}) {
            assertModelRejected("repairs/repairEdit",
                    editFormWithModel(finished, "", "1500").param("fix", "Replaced the display cable"));
            assertModelRejected("repairs/repairEdit",
                    editFormWithModel(finished, "   ", "1500").param("fix", "Replaced the display cable"));
        }
        verify(service, never()).updateRepairRecord(any(), any());
    }

    @Test
    void aBlankModelIsRejectedOnceCompletedOrReleasedOnCreate() throws Exception {
        for (String finished : new String[] {"COMPLETED", "RELEASED"}) {
            assertModelRejected("repairs/repairCreate",
                    createFormWithModel(finished, "", "1500").param("fix", "Replaced the display cable"));
            assertModelRejected("repairs/repairCreate",
                    createFormWithModel(finished, "   ", "1500").param("fix", "Replaced the display cable"));
        }
        verify(service, never()).saveRepairRecord(any());
    }

    @Test
    void aRejectedModelNameKeepsEveryOtherEntryAndSitsBesideTheFixAndCostErrors() throws Exception {
        when(service.getRepairRecordById(5L)).thenReturn(freshTicket());

        for (String view : new String[] {"repairs/repairEdit", "repairs/repairCreate"}) {
            MockHttpServletRequestBuilder request = view.endsWith("Edit")
                    ? editFormWithModel("COMPLETED", "", "0") : createFormWithModel("COMPLETED", "", "0");

            MvcResult result = mvc.perform(request.param("remarks", "Keep this remark"))
                    .andExpect(status().isOk())
                    .andExpect(view().name(view))
                    .andExpect(model().attributeHasFieldErrorCode("repairRecordDto", "modelName", "NotBlank"))
                    .andExpect(model().attributeHasFieldErrorCode("repairRecordDto", "fix", "NotEmpty"))
                    .andExpect(model().attributeHasFieldErrorCode("repairRecordDto", "cost", "Positive"))
                    .andReturn();

            RepairRecordDto kept = (RepairRecordDto) result.getModelAndView().getModel().get("repairRecordDto");
            assertEquals("Laptop", kept.getDeviceType(), view);
            assertEquals("no display", kept.getIssueDescription(), view);
            assertEquals("Keep this remark", kept.getRemarks(), view);
            assertEquals("COMPLETED", kept.getStatus(), view);
        }
    }

    @Test
    void completedWithAModelAFixAndAPositiveCostIsSaved() throws Exception {
        when(service.getRepairRecordById(5L)).thenReturn(freshTicket());

        mvc.perform(editFormWithModel("COMPLETED", "Aspire 5", "1500").param("fix", "Replaced the display cable"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(createFormWithModel("COMPLETED", "Aspire 5", "1500").param("fix", "Replaced the display cable"))
                .andExpect(status().is3xxRedirection());
        verify(service).updateRepairRecord(eq(5L), any());
        verify(service).saveRepairRecord(any());
    }

    @Test
    void anAppointmentSourcedRepairWithNoModelOpensAndSavesAsInProgressFromTheEditForm() throws Exception {
        // What createFromAppointment leaves behind: no brand and no model until the device is received.
        RepairRecord fromAppointment = freshTicket();
        fromAppointment.setModelName(null);
        fromAppointment.setAppointment(new com.rnpc.inventory.entity.Appointment());
        when(service.getRepairRecordById(5L)).thenReturn(fromAppointment);

        MvcResult opened = mvc.perform(get("/repair/edit/5").principal(admin))
                .andExpect(status().isOk())
                .andExpect(view().name("repairs/repairEdit"))
                .andReturn();
        RepairRecordDto form = (RepairRecordDto) opened.getModelAndView().getModel().get("repairRecordDto");
        assertNull(form.getModelName());

        // The browser posts the empty model field as "".
        mvc.perform(editFormWithModel("IN_PROGRESS", "", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/repair?clientId=1"));
        verify(service).updateRepairRecord(eq(5L), any());
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

    // ---- Customer pages (repairs batch U1) --------------------------------------------------------

    private Authentication customerNamed(String email) {
        return new UsernamePasswordAuthenticationToken(email, "n/a", List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
    }

    private RepairRecord ownedBy(String email, long id) throws Exception {
        RepairRecord repair = freshTicket();
        repair.setRepairId(id);
        com.rnpc.inventory.entity.User user = new com.rnpc.inventory.entity.User();
        user.setUsername(email);
        repair.getClient().setUser(user);
        repair.setRemarks("INTERNAL remark");
        return repair;
    }

    @Test
    void aSignedInCustomerGetsTheirOwnTemplateBuiltFromRepairViews() throws Exception {
        when(service.getRepairRecordsForUser("customer@example.com")).thenReturn(List.of(ownedBy("customer@example.com", 5L)));

        MvcResult result = mvc.perform(get("/repair").principal(customerNamed("customer@example.com")))
                .andExpect(status().isOk())
                .andExpect(view().name("repairs/customerRepairIndex"))
                .andReturn();

        @SuppressWarnings("unchecked")
        List<com.rnpc.inventory.dto.RepairView> views =
                (List<com.rnpc.inventory.dto.RepairView>) result.getModelAndView().getModel().get("repairViews");
        assertEquals(1, views.size());
        assertEquals("JO-00005", views.get(0).getJobOrderNumber());
    }

    @Test
    void theCustomerListIsBuiltOnlyFromTheirOwnVisibleRepairs() throws Exception {
        when(service.getRepairRecordsForUser("customer@example.com")).thenReturn(List.of());

        mvc.perform(get("/repair").param("clientId", "99").principal(customerNamed("customer@example.com")))
                .andExpect(view().name("repairs/customerRepairIndex"))
                .andExpect(model().attribute("repairViews", List.of()));

        verify(service).getRepairRecordsForUser("customer@example.com");
        verify(service, never()).getAllRepairRecords();
        verify(service, never()).getRepairRecordsByClient(any());
    }

    @Test
    void anAdminAndASignedOutVisitorKeepTheExistingTemplate() throws Exception {
        mvc.perform(get("/repair").principal(admin))
                .andExpect(view().name("repairs/repairIndex"));
        mvc.perform(get("/repair"))
                .andExpect(view().name("repairs/repairIndex"));
    }

    @Test
    void theOwnerOfARepairGetsTheCustomerFragment() throws Exception {
        RepairRecord repair = ownedBy("customer@example.com", 5L);
        when(service.getRepairRecordById(5L)).thenReturn(repair);

        MvcResult result = mvc.perform(get("/repair/5/modal").principal(customerNamed("customer@example.com")))
                .andExpect(status().isOk())
                .andExpect(view().name("repairs/repairDetail :: customerRepairCard"))
                .andReturn();

        com.rnpc.inventory.dto.RepairView view =
                (com.rnpc.inventory.dto.RepairView) result.getModelAndView().getModel().get("view");
        assertEquals("JO-00005", view.getJobOrderNumber());
    }

    @Test
    void anotherCustomersRepairStillRedirectsAwayFromTheModal() throws Exception {
        when(service.getRepairRecordById(5L)).thenReturn(ownedBy("someone.else@example.com", 5L));

        mvc.perform(get("/repair/5/modal").principal(customerNamed("customer@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/repair"));
    }

    @Test
    void aRepairHiddenBehindAnUnconfirmedAppointmentStillRedirectsEvenForItsOwner() throws Exception {
        RepairRecord repair = ownedBy("customer@example.com", 5L);
        com.rnpc.inventory.entity.Appointment pending = new com.rnpc.inventory.entity.Appointment();
        pending.setStatus(com.rnpc.inventory.entity.Appointment.Status.PENDING);
        repair.setAppointment(pending);
        when(service.getRepairRecordById(5L)).thenReturn(repair);

        mvc.perform(get("/repair/5/modal").principal(customerNamed("customer@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/repair"));
    }

    @Test
    void anAdminStillGetsTheAdminFragmentForTheModal() throws Exception {
        when(service.getRepairRecordById(5L)).thenReturn(ownedBy("customer@example.com", 5L));

        mvc.perform(get("/repair/5/modal").principal(admin))
                .andExpect(view().name("repairs/repairDetail :: repairCard"))
                .andExpect(model().attributeDoesNotExist("view"));
    }
}
