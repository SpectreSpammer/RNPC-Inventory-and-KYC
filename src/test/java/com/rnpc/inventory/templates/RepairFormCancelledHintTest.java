package com.rnpc.inventory.templates;

import com.rnpc.inventory.controller.RepairRecordController;
import com.rnpc.inventory.entity.Client;
import com.rnpc.inventory.entity.RepairRecord;
import com.rnpc.inventory.entity.RepairRecord.RepairStatus;
import com.rnpc.inventory.service.ClientService;
import com.rnpc.inventory.service.NotificationService;
import com.rnpc.inventory.service.RepairRecordService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * The Cancelled cost hint on the repair edit and create forms. The forms bind with th:field, which
 * needs Spring's RequestContext and a BindingResult, so they are rendered the way the app renders
 * them - through a real ThymeleafViewResolver behind the real RepairRecordController in a standalone
 * MockMvc, with mocked services. No Spring context, no database, no running app.
 *
 * What is covered is the server-side half: the hint is rendered visible when the saved status (edit)
 * or the posted status (a form re-shown after a validation error) is CANCELLED, and rendered hidden
 * for any other status and for none. What is NOT covered is the browser half - the script that
 * toggles the hint as the Status select changes - which needs a real browser and is checked by hand.
 */
class RepairFormCancelledHintTest {

    private static final String HINT_TEXT =
            "If the client declined after the check-up, enter the diagnostic fee you charged. "
                    + "Leave it at 0 if there was no check-up.";
    private static final Pattern HINT_TAG = Pattern.compile("<div[^>]*id=\"cancelledCostHint\"[^>]*>");

    private final RepairRecordService repairService = mock(RepairRecordService.class);
    private final ClientService clientService = mock(ClientService.class);
    private final MockMvc mvc;

    private final Authentication admin = new UsernamePasswordAuthenticationToken(
            "admin@example.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

    RepairFormCancelledHintTest() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        ClassLoaderTemplateResolver templates = new ClassLoaderTemplateResolver();
        templates.setPrefix("templates/");
        templates.setSuffix(".html");
        templates.setTemplateMode(TemplateMode.HTML);
        templates.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(templates);
        ThymeleafViewResolver views = new ThymeleafViewResolver();
        views.setTemplateEngine(engine);
        views.setCharacterEncoding("UTF-8");

        mvc = MockMvcBuilders.standaloneSetup(new RepairRecordController(
                        repairService, clientService, mock(NotificationService.class), validator))
                .setValidator(validator).setViewResolvers(views).build();
    }

    private RepairRecord repair(RepairStatus status) throws Exception {
        Client client = new Client();
        client.setClientId(1L);
        client.setFullName("Nand Test");
        RepairRecord r = new RepairRecord();
        r.setRepairId(5L);
        r.setJobOrderNumber("JO-00005");
        r.setClient(client);
        r.setDeviceType("Laptop");
        r.setModelName("Aspire 5");
        r.setIssueDescription("no display");
        r.setRepairDate(new SimpleDateFormat("yyyy-MM-dd").parse("2026-10-02"));
        r.setStatus(status);
        return r;
    }

    private String render(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.principal(admin)).andReturn().getResponse().getContentAsString();
    }

    /** The hint's own tag, so "hidden" is read off that element and not off anything else on the page. */
    private static String hintTag(String html) {
        Matcher m = HINT_TAG.matcher(html);
        assertTrue(m.find(), "the form has no #cancelledCostHint element: " + html);
        return m.group();
    }

    private static void assertHintVisible(String html) {
        assertFalse(hintTag(html).contains("hidden"), "hint should be visible: " + hintTag(html));
        assertTrue(html.contains(HINT_TEXT), "hint text missing");
    }

    private static void assertHintHidden(String html) {
        assertTrue(hintTag(html).contains("hidden"), "hint should be hidden: " + hintTag(html));
        // still in the page, so the script can reveal it when the select changes
        assertTrue(html.contains(HINT_TEXT), "hint text missing");
    }

    /** An update the validator refuses (negative cost), so the form is re-shown with what was posted. */
    private MockHttpServletRequestBuilder badUpdate(String status) {
        return put("/repair/update/5")
                .param("clientId", "1").param("deviceType", "Laptop").param("modelName", "Aspire 5")
                .param("issueDescription", "no display").param("repairDate", "2026-10-02")
                .param("cost", "-5").param("status", status);
    }

    private MockHttpServletRequestBuilder badCreate(String status) {
        return post("/repair/create")
                .param("clientId", "1").param("deviceType", "Laptop").param("modelName", "Aspire 5")
                .param("issueDescription", "no display").param("repairDate", "2026-10-02")
                .param("cost", "-5").param("status", status);
    }

    // ---- Edit form: the saved status --------------------------------------------------------------

    @Test
    void theEditFormShowsTheHintWhenTheSavedStatusIsCancelled() throws Exception {
        when(repairService.getRepairRecordById(5L)).thenReturn(repair(RepairStatus.CANCELLED));

        assertHintVisible(render(get("/repair/edit/5")));
    }

    @Test
    void theEditFormHidesTheHintForEveryOtherSavedStatus() throws Exception {
        for (RepairStatus status : RepairStatus.values()) {
            if (status == RepairStatus.CANCELLED) {
                continue;
            }
            when(repairService.getRepairRecordById(5L)).thenReturn(repair(status));

            assertHintHidden(render(get("/repair/edit/5")));
        }
    }

    // ---- Edit form: the posted status, after a validation error ------------------------------------

    @Test
    void aRejectedEditKeepsTheHintVisibleWhenTheRepostedStatusIsCancelled() throws Exception {
        when(repairService.getRepairRecordById(5L)).thenReturn(repair(RepairStatus.PENDING));

        assertHintVisible(render(badUpdate("CANCELLED")));
    }

    @Test
    void aRejectedEditHidesTheHintWhenTheRepostedStatusIsNotCancelled() throws Exception {
        // saved as Cancelled, but the person changed it to Pending before posting a bad cost
        when(repairService.getRepairRecordById(5L)).thenReturn(repair(RepairStatus.CANCELLED));

        assertHintHidden(render(badUpdate("PENDING")));
    }

    // ---- Create form --------------------------------------------------------------------------------

    @Test
    void aFreshCreateFormHasNoStatusAndHidesTheHint() throws Exception {
        assertHintHidden(render(get("/repair/create")));
    }

    @Test
    void aRejectedCreateShowsTheHintForPostedCancelledAndHidesItForPending() throws Exception {
        assertHintVisible(render(badCreate("CANCELLED")));
        assertHintHidden(render(badCreate("PENDING")));
    }

    // ---- What the browser half depends on -----------------------------------------------------------

    @Test
    void theFormCarriesTheIdsAndScriptTheBrowserToggleDependsOn() throws Exception {
        when(repairService.getRepairRecordById(5L)).thenReturn(repair(RepairStatus.PENDING));

        for (String html : List.of(render(get("/repair/edit/5")), render(get("/repair/create")))) {
            assertTrue(html.contains("id=\"status\""), "Status select id missing");
            assertTrue(html.contains("getElementById('cancelledCostHint')"), "toggle script missing");
            assertTrue(html.contains("status.value !== 'CANCELLED'"), "toggle condition missing");
        }
    }
}
