package com.rnpc.inventory.templates;

import com.rnpc.inventory.controller.TicketController;
import com.rnpc.inventory.entity.Client;
import com.rnpc.inventory.entity.RepairRecord;
import com.rnpc.inventory.entity.RepairRecord.RepairStatus;
import com.rnpc.inventory.service.ClientService;
import com.rnpc.inventory.service.NotificationService;
import com.rnpc.inventory.service.RepairRecordService;
import com.rnpc.inventory.service.UserService;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * The walk-in ticket pages (batches N1 and N2). N1: every "Back to tickets" and Cancel link on the
 * ticket-created page and the create form has to end on /ticket/view, the real list - there is no
 * GET /ticket handler, so a link to bare /ticket would 404. N2: the slip never carries the shop-only
 * Remarks; the page keeps its "Ticket created" wording for a PENDING repair and reads "Client copy"
 * for any other status; blank Received / Received by rows are left out. Rendered through a
 * real ThymeleafViewResolver behind the real TicketController in a standalone MockMvc, with mocked
 * services: no Spring context, no database.
 */
class TicketPagesRenderTest {

    private final RepairRecordService repairService = mock(RepairRecordService.class);
    private final MockMvc mvc;

    private final Authentication admin = new UsernamePasswordAuthenticationToken(
            "admin@example.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

    TicketPagesRenderTest() {
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

        mvc = MockMvcBuilders.standaloneSetup(new TicketController(
                        mock(ClientService.class), repairService, mock(NotificationService.class), mock(UserService.class)))
                .setValidator(validator).setViewResolvers(views).build();
    }

    private RepairRecord repair() throws Exception {
        Client client = new Client();
        client.setClientId(1L);
        client.setFullName("Nand Test");
        client.setContactNumber("0999 123 4567");
        RepairRecord r = new RepairRecord();
        r.setRepairId(5L);
        r.setJobOrderNumber("JO-00005");
        r.setClient(client);
        r.setDeviceType("Laptop");
        r.setModelName("Aspire 5");
        r.setIssueDescription("no display");
        r.setRepairDate(new SimpleDateFormat("yyyy-MM-dd").parse("2026-10-02"));
        r.setStatus(RepairStatus.PENDING);
        return r;
    }

    private String render(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.principal(admin)).andReturn().getResponse().getContentAsString();
    }

    /** The opening tag of the first link whose text starts with the given label. */
    private static String linkLabelled(String html, String label) {
        Matcher m = Pattern.compile("<a\\b([^>]*)>\\s*(?:<[^>]+>\\s*)*" + Pattern.quote(label), Pattern.DOTALL).matcher(html);
        assertTrue(m.find(), "no link labelled \"" + label + "\" in: " + html);
        return m.group(1);
    }

    @Test
    void backToTicketsOnTheTicketCreatedPageGoesToTheTicketList() throws Exception {
        when(repairService.getRepairRecordById(5L)).thenReturn(repair());

        String attributes = linkLabelled(render(get("/ticket/5")), "Back to tickets");

        assertTrue(attributes.contains("href=\"/ticket/view\""), attributes);
    }

    @Test
    void everyListLinkOnTheTicketCreatedPageEndsOnTheTicketListNeverBareTicket() throws Exception {
        when(repairService.getRepairRecordById(5L)).thenReturn(repair());

        String html = render(get("/ticket/5"));

        // "Go to repair history" in the What next card is the other link to the list
        assertTrue(linkLabelled(html, "Go to repair history").contains("href=\"/ticket/view\""), html);
        // no link anywhere points at bare /ticket (it has no handler and would 404)
        assertEquals(0, Pattern.compile("href=\"/ticket/?\"").matcher(html).results().count(), html);
    }

    @Test
    void theCreateFormsBackLinkAndCancelButtonBothGoToTheTicketList() throws Exception {
        String html = render(get("/ticket/create"));

        // the shared header's back link (its text is the parts form's fixed "Back to parts")
        Matcher back = Pattern.compile("<a class=\"pf-back\"[^>]*href=\"([^\"]*)\"").matcher(html);
        assertTrue(back.find(), html);
        assertEquals("/ticket/view", back.group(1));

        Matcher cancel = Pattern.compile("<a class=\"pf-cancel\"[^>]*href=\"([^\"]*)\"[^>]*>Cancel").matcher(html);
        assertTrue(cancel.find(), html);
        assertEquals("/ticket/view", cancel.group(1));
        assertEquals(0, Pattern.compile("href=\"/ticket/?\"").matcher(html).results().count(), html);
    }

    // ---- N2: Remarks, wording by status, blank-safe rows, create form header ---------------------------

    /** Markup without its HTML comments (the templates' own comments quote the old wording). */
    private static String withoutComments(String markup) {
        return markup.replaceAll("(?s)<!--.*?-->", "");
    }

    private static String readable(String markup) {
        return withoutComments(markup).replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ");
    }

    private RepairRecord repairWithStatus(RepairStatus status) throws Exception {
        RepairRecord r = repair();
        r.setStatus(status);
        r.setCreatedAt(new SimpleDateFormat("yyyy-MM-dd").parse("2026-10-03"));
        r.setReceivedByEmployeeId("Test Admin-12345");
        return r;
    }

    @Test
    void theSlipNeverPrintsTheShopOnlyRemarksOnScreenOnPaperOrInTheModal() throws Exception {
        RepairRecord r = repairWithStatus(RepairStatus.COMPLETED);
        r.setRemarks("SHOP-ONLY-REMARK-XYZ");
        r.setFix("Replaced the display cable");
        r.setRecommendation("Back up your files");
        r.setTechnician("Juan Dela Cruz");
        when(repairService.getRepairRecordById(5L)).thenReturn(r);

        String page = render(get("/ticket/5"));
        String modal = render(get("/ticket/5/modal"));

        for (String html : List.of(page, modal)) {
            assertFalse(html.contains("SHOP-ONLY-REMARK-XYZ"), html);
            assertFalse(readable(html).contains("Remarks"), readable(html));
            // everything else on the slip is still there
            assertTrue(readable(html).contains("Fix Applied"), readable(html));
            assertTrue(html.contains("Replaced the display cable"), html);
            assertTrue(readable(html).contains("Recommendation"), readable(html));
            assertTrue(html.contains("Back up your files"), html);
            assertTrue(html.contains("Juan Dela Cruz"), html);
            assertTrue(html.contains("no display"), html);
        }
    }

    // ---- P1: the slip follows the same cost and status rules as the PDF ---------------------------------

    /** The slip alone (the modal fragment), as readable text. */
    private String slipText(RepairRecord r) throws Exception {
        when(repairService.getRepairRecordById(5L)).thenReturn(r);
        return readable(render(get("/ticket/5/modal")));
    }

    @Test
    void theSlipShowsToBeAssessedWhileNoCostIsSetOnAnyOpenStatus() throws Exception {
        for (RepairStatus status : new RepairStatus[]{RepairStatus.PENDING, RepairStatus.IN_PROGRESS,
                RepairStatus.COMPLETED, RepairStatus.RELEASED}) {
            RepairRecord r = repairWithStatus(status);
            r.setCost(0);

            String text = slipText(r);

            assertTrue(text.contains("Estimated cost To be assessed"), status + ": " + text);
            assertFalse(text.contains("Php 0.00"), status + ": " + text);
        }
    }

    @Test
    void theSlipShowsAnAssessedCostAsMoneyWithNoDiagnosticFeeNote() throws Exception {
        RepairRecord r = repairWithStatus(RepairStatus.COMPLETED);
        r.setCost(2500);

        String text = slipText(r);

        assertTrue(text.contains("Estimated cost Php 2,500.00"), text);
        assertFalse(text.contains("diagnostic fee"), text);
    }

    @Test
    void theSlipShowsACancelledRepairsCostAsTheDiagnosticFee() throws Exception {
        RepairRecord r = repairWithStatus(RepairStatus.CANCELLED);
        r.setCost(1500);

        String text = slipText(r);

        assertTrue(text.contains("Estimated cost Php 1,500.00 (diagnostic fee)"), text);
        assertFalse(text.contains("To be assessed"), text);
    }

    @Test
    void theSlipShowsADashForACancelledRepairWithNoCost() throws Exception {
        RepairRecord r = repairWithStatus(RepairStatus.CANCELLED);
        r.setCost(0);

        String text = slipText(r);

        assertTrue(text.contains("Estimated cost —"), text);
        assertFalse(text.contains("To be assessed"), text);
        assertFalse(text.contains("Php 0.00"), text);
    }

    @Test
    void theSlipSpellsTheStatusInWordsNotAsTheEnum() throws Exception {
        String[][] expected = {{"PENDING", "Pending"}, {"IN_PROGRESS", "In Progress"}, {"COMPLETED", "Completed"},
                {"RELEASED", "Released"}, {"CANCELLED", "Cancelled"}};
        for (String[] pair : expected) {
            String text = slipText(repairWithStatus(RepairStatus.valueOf(pair[0])));

            assertTrue(text.contains("Status " + pair[1]), pair[0] + ": " + text);
            assertFalse(text.contains("IN PROGRESS") || text.contains("COMPLETED") || text.contains("CANCELLED")
                    || text.contains("RELEASED") || text.contains("PENDING"), pair[0] + ": " + text);
        }
    }

    @Test
    void thePageSlipAndTheModalSlipAgreeOnCostAndStatus() throws Exception {
        RepairRecord r = repairWithStatus(RepairStatus.CANCELLED);
        r.setCost(1500);
        when(repairService.getRepairRecordById(5L)).thenReturn(r);

        String page = readable(render(get("/ticket/5")));

        assertTrue(page.contains("Estimated cost Php 1,500.00 (diagnostic fee)"), page);
        assertTrue(page.contains("Status Cancelled"), page);
    }

    @Test
    void aPendingRepairKeepsTheTicketCreatedWordingExactly() throws Exception {
        when(repairService.getRepairRecordById(5L)).thenReturn(repairWithStatus(RepairStatus.PENDING));

        String html = withoutComments(render(get("/ticket/5")));
        String collapsed = html.replaceAll("\\s+", " ");

        assertTrue(html.contains("<title>Ticket created - RN PC</title>"), html);
        assertTrue(html.contains(">Ticket created</h1>"), html);
        assertTrue(collapsed.contains("<p>Saved and logged as <strong>Pending</strong>. Hand the client their copy "
                + "of the slip, then keep the device with the job order number attached.</p>"), collapsed);
        assertFalse(readable(html).contains("Client copy for"), readable(html));
        assertFalse(html.contains(">Client copy</h1>"), html);
    }

    @Test
    void aCompletedRepairGetsANeutralTitleHeadingAndBannerWithTheStatusInWords() throws Exception {
        when(repairService.getRepairRecordById(5L)).thenReturn(repairWithStatus(RepairStatus.COMPLETED));

        String html = withoutComments(render(get("/ticket/5")));
        String text = readable(html);

        assertTrue(html.contains("<title>Client copy - RN PC</title>"), html);
        assertTrue(html.contains(">Client copy</h1>"), html);
        assertTrue(text.contains("Client copy for JO-00005. Status: Completed."), text);
        assertFalse(html.contains("Ticket created"), html);
        assertFalse(text.contains("Saved and logged as"), text);
    }

    @Test
    void theNeutralBannerSpellsEveryOtherStatusInWords() throws Exception {
        String[][] expected = {
                {"IN_PROGRESS", "In Progress"}, {"RELEASED", "Released"}, {"CANCELLED", "Cancelled"}};
        for (String[] pair : expected) {
            when(repairService.getRepairRecordById(5L)).thenReturn(repairWithStatus(RepairStatus.valueOf(pair[0])));

            String text = readable(render(get("/ticket/5")));

            assertTrue(text.contains("Client copy for JO-00005. Status: " + pair[1] + "."), pair[0] + ": " + text);
            assertFalse(text.contains("Saved and logged as"), pair[0] + ": " + text);
        }
    }

    @Test
    void aBlankReceivedByLeavesItsRowOutAndAFilledOneShowsIt() throws Exception {
        for (String receiver : new String[]{null, "", "   "}) {
            RepairRecord r = repairWithStatus(RepairStatus.PENDING);
            r.setReceivedByEmployeeId(receiver);
            when(repairService.getRepairRecordById(5L)).thenReturn(r);

            String html = render(get("/ticket/5"));

            assertFalse(html.contains("<span class=\"k\">Received by</span>"), "receiver=[" + receiver + "]: " + html);
            // the row before it is untouched
            assertTrue(html.contains("<span class=\"k\">Received</span>"), html);
        }

        when(repairService.getRepairRecordById(5L)).thenReturn(repairWithStatus(RepairStatus.PENDING));
        String html = render(get("/ticket/5"));
        assertTrue(html.contains("<span class=\"k\">Received by</span>"), html);
        assertTrue(html.contains("Test Admin-12345"), html);
    }

    @Test
    void aNullCreatedDateLeavesTheReceivedRowOutInsteadOfPrintingAnEmptyOne() throws Exception {
        RepairRecord r = repairWithStatus(RepairStatus.PENDING);
        r.setCreatedAt(null);
        when(repairService.getRepairRecordById(5L)).thenReturn(r);

        String html = render(get("/ticket/5"));

        assertFalse(html.contains("<span class=\"k\">Received</span>"), html);
        assertTrue(html.contains("<span class=\"k\">Received by</span>"), html);
        assertFalse(Pattern.compile("\\bnull\\b").matcher(html.replaceAll("(?is)<script\\b.*?</script>|<style\\b.*?</style>", "")).find(), html);
    }

    @Test
    void theCreateFormsBackLinkSaysBackToTicketsNotBackToParts() throws Exception {
        String html = render(get("/ticket/create"));
        String text = readable(html);

        assertTrue(text.contains("Back to tickets"), text);
        assertFalse(text.contains("Back to parts"), text);
        assertTrue(linkLabelled(html, "Back to tickets").contains("href=\"/ticket/view\""), html);
        // the rest of the shared header look is kept
        assertTrue(html.contains("class=\"pf-code\">TKT</span>"), html);
        assertTrue(html.contains(">Create ticket</h1>"), html);
    }
}
