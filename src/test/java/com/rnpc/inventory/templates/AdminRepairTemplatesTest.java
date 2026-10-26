package com.rnpc.inventory.templates;

import com.rnpc.inventory.dto.AdminRepairView;
import com.rnpc.inventory.entity.Client;
import com.rnpc.inventory.entity.RepairRecord;
import com.rnpc.inventory.entity.RepairRecord.RepairStatus;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Renders the admin repair templates (repairs batch A1) - the details fragment and the list in both
 * modes - with the real Thymeleaf engine and the real template files, the same way
 * CustomerRepairTemplatesTest does for the customer's: a WebContext over spring-test's mock servlet
 * objects, so no Spring context, no controller, no database.
 *
 * "null" and "()" are looked for only in the markup left once every script and style block is
 * removed: inside a script they are ordinary JavaScript, not a rendering bug.
 */
class AdminRepairTemplatesTest {

    private static final Pattern SCRIPT_OR_STYLE =
            Pattern.compile("<script\\b.*?</script>|<style\\b.*?</style>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern WORD_NULL = Pattern.compile("\\bnull\\b");
    private static final Pattern STAT_CARD =
            Pattern.compile("data-status=\"(\\w*)\"[^>]*>.*?<p class=\"stat-label\"[^>]*>([^<]*)</p>\\s*<p class=\"stat-value\">(\\d+)</p>",
                    Pattern.DOTALL);

    // ---- Rendering --------------------------------------------------------------------------------

    private static String render(String template, String fragment, Map<String, Object> variables) {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);

        MockServletContext servletContext = new MockServletContext();
        WebContext context = new WebContext(
                JakartaServletWebApplication.buildApplication(servletContext).buildExchange(
                        new MockHttpServletRequest(servletContext), new MockHttpServletResponse()),
                Locale.ENGLISH);
        context.setVariables(variables);
        return fragment == null ? engine.process(template, context) : engine.process(template, Set.of(fragment), context);
    }

    private static String renderCard(RepairRecord repair) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("repair", repair);
        variables.put("adminView", AdminRepairView.from(repair));
        return render("repairs/repairDetail", "repairCard", variables);
    }

    private static String renderList(List<RepairRecord> repairs, boolean ticketMode, Client client) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("repairViews", repairs.stream().map(AdminRepairView::from).toList());
        variables.put("ticketMode", ticketMode);
        variables.put("isAdmin", true);
        variables.put("navIsAdmin", true);
        variables.put("client", client);
        variables.put("currentUsername", "admin@example.com");
        variables.put("currentRole", "Admin");
        variables.put("unreadNotifications", 0);
        variables.put("recentNotifications", List.of());
        return render("repairs/repairIndex", null, variables);
    }

    /** What is left to read once scripts and styles are gone. */
    private static String visibleMarkup(String html) {
        return SCRIPT_OR_STYLE.matcher(html).replaceAll("");
    }

    private static void assertNoNullWord(String html) {
        String visible = visibleMarkup(html);
        Matcher m = WORD_NULL.matcher(visible);
        assertFalse(m.find(), () -> "the word null appears outside scripts and styles, near: "
                + visible.substring(Math.max(0, m.start() - 80), Math.min(visible.length(), m.end() + 80)));
    }

    // ---- Fixtures ---------------------------------------------------------------------------------

    private static Client client() {
        Client c = new Client();
        c.setClientId(9L);
        c.setFullName("Nand Test");
        c.setContactNumber("0999 123 4567");
        return c;
    }

    private static RepairRecord repair(long id, String jobOrder, RepairStatus status) throws Exception {
        RepairRecord r = new RepairRecord();
        r.setRepairId(id);
        r.setJobOrderNumber(jobOrder);
        r.setClient(client());
        r.setDeviceType("Cellphone");
        r.setBrand("Asus");
        r.setModelName("dsadas");
        r.setIssueDescription("bsod");
        r.setRepairDate(new SimpleDateFormat("yyyy-MM-dd").parse("2026-10-03"));
        r.setStatus(status);
        return r;
    }

    private static RepairRecord appointmentSourced() throws Exception {
        RepairRecord r = repair(2L, "JO-00002", RepairStatus.PENDING);
        r.setBrand(null);
        r.setModelName(null);
        return r;
    }

    private static Map<String, String> statCards(String html) {
        Map<String, String> counts = new HashMap<>();
        Matcher m = STAT_CARD.matcher(html);
        while (m.find()) {
            counts.put(m.group(1), m.group(2).trim() + "=" + m.group(3));
        }
        return counts;
    }

    // ---- The details fragment ---------------------------------------------------------------------

    @Test
    void theAdminFragmentShowsRemarksClientAndContactAndNeverNull() throws Exception {
        RepairRecord r = repair(14L, "JO-00014", RepairStatus.COMPLETED);
        r.setRemarks("INTERNAL-NOTE-XYZ");
        r.setTechnician("Juan Dela Cruz");
        r.setFix("Replaced the display cable");
        r.setRecommendation("Back up your files");
        r.setCost(2000.0);
        r.setImageFileName("fixed.png");

        String html = renderCard(r);

        assertTrue(html.contains("JO-00014"), html);
        assertTrue(html.contains("The work is done. Ready to hand back to the client."), html);
        assertTrue(html.contains("INTERNAL-NOTE-XYZ"), html);
        assertTrue(html.contains("Shop remarks"), html);
        assertTrue(html.contains("(not shown to the client)"), html);
        assertTrue(html.contains("Nand Test"), html);
        assertTrue(html.contains("0999 123 4567"), html);
        assertTrue(html.contains("Juan Dela Cruz"), html);
        assertTrue(html.contains("Php 2,000.00"), html);
        assertTrue(html.contains("/images/fixed.png"), html);
        assertTrue(html.contains("Photo from the technician"), html);
        assertTrue(html.contains("href=\"/repair/edit/14\""), html);
        assertTrue(html.contains("Edit repair"), html);
        assertTrue(html.contains("Close"), html);
        assertEquals(3, countOccurrences(html, "<li class=\"is-done\">"), html);
        assertNoNullWord(html);
    }

    @Test
    void anAppointmentSourcedRepairRendersCleanlyInTheAdminFragment() throws Exception {
        String html = renderCard(appointmentSourced());
        String visible = visibleMarkup(html);

        assertNoNullWord(html);
        assertFalse(visible.contains("()"), "empty parentheses in: " + visible);
        assertTrue(visible.contains("Cellphone"), visible);

        // The fallbacks, each in place of a value that was never entered.
        assertTrue(html.contains("To be assessed"), html);
        assertTrue(html.contains("Not assigned yet"), html);
        assertTrue(html.contains("No warranty set"), html);
        assertTrue(html.contains("Not recorded yet"), html);
        assertTrue(html.contains("None"), html);
        assertTrue(html.contains("No remarks"), html);
        assertTrue(html.contains("Received and waiting for a technician."), html);
        assertEquals(1, countOccurrences(html, "<li class=\"is-done\">"), html);

        // No photo block, and the client line is still there.
        assertFalse(html.contains("class=\"ra-photo\""), html);
        assertFalse(html.contains("<img"), html);
        assertTrue(html.contains("Nand Test"), html);
    }

    @Test
    void aMissingContactNumberReadsNotProvidedNotNull() throws Exception {
        RepairRecord r = repair(14L, "JO-00014", RepairStatus.PENDING);
        r.getClient().setContactNumber(null);

        String html = renderCard(r);

        assertTrue(html.contains("Not provided"), html);
        assertNoNullWord(html);
    }

    // ---- The list, repair mode --------------------------------------------------------------------

    @Test
    void theListRendersTheSixCardsWithTheRightCounts() throws Exception {
        String html = renderList(List.of(
                repair(1L, "JO-00001", RepairStatus.PENDING),
                repair(2L, "JO-00002", RepairStatus.PENDING),
                repair(3L, "JO-00003", RepairStatus.IN_PROGRESS),
                repair(4L, "JO-00004", RepairStatus.RELEASED)), false, null);

        assertEquals(Map.of(
                "", "Total Repairs=4",
                "PENDING", "Pending=2",
                "IN_PROGRESS", "In Progress=1",
                "COMPLETED", "Completed=0",
                "RELEASED", "Released=1",
                "CANCELLED", "Cancelled=0"), statCards(html), html);
        assertNoNullWord(html);
    }

    @Test
    void repairModeUsesTheRepairWordingAndActionsInOrder() throws Exception {
        RepairRecord r = repair(1L, "JO-00001", RepairStatus.PENDING);
        String html = renderList(List.of(r), false, null);
        String visible = visibleMarkup(html);

        assertTrue(visible.contains(">Repair History</h1>"), visible);
        assertTrue(visible.contains("Track your device repairs and their current status."), visible);
        assertTrue(visible.contains("Add Repair"), visible);
        assertTrue(visible.contains("href=\"/repair/create\""), visible);
        assertTrue(visible.contains("Total Repairs"), visible);
        assertTrue(visible.contains(">Ref #</th>"), visible);
        assertTrue(visible.contains(">Date of repair</th>"), visible);
        assertTrue(visible.contains("1 repair(s) total"), visible);
        assertTrue(visible.contains("No repairs match these filters."), visible);
        // (the sidebar's own "Tickets" group is expected to be there; the page's wording is not)
        assertFalse(visible.contains("Total Tickets") || visible.contains("ticket(s)")
                || visible.contains(">Ticket #</th>") || visible.contains("No tickets"),
                "ticket wording leaked into repair mode: " + visible);

        // The row: client link, then Edit, Delete (the existing confirm and DELETE form) and View, in that order.
        assertTrue(visible.contains("href=\"/repair?clientId=9\""), visible);
        int edit = visible.indexOf("href=\"/repair/edit/1\"");
        int delete = visible.indexOf("action=\"/repair/delete/1\"");
        int viewButton = visible.indexOf("data-repair-id=\"1\"");
        assertTrue(edit > 0 && edit < delete && delete < viewButton, visible);
        assertTrue(visible.contains("name=\"_method\" value=\"delete\""), visible);
        assertTrue(visible.contains("onclick=\"return confirm('Are you sure?')\""), visible);
        assertTrue(visible.contains("To be assessed"), visible);
        assertTrue(visible.contains("Oct 03, 2026"), visible);
    }

    @Test
    void repairModeHighlightsRepairsAndLeavesTicketsAlone() throws Exception {
        String html = renderList(List.of(repair(1L, "JO-00001", RepairStatus.PENDING)), false, null);

        assertTrue(Pattern.compile("<a href=\"/repair\" class=\"nav-item[^\"]*\\bactive\\b").matcher(html).find(), html);
        assertFalse(Pattern.compile("<a href=\"/ticket/view\"[^>]*\\bactive\\b").matcher(html).find(), html);
    }

    @Test
    void theClientDrillDownKeepsItsTitleAndBothButtonsAndDropsTheClientColumn() throws Exception {
        String html = renderList(List.of(repair(1L, "JO-00001", RepairStatus.PENDING)), false, client());
        String visible = visibleMarkup(html);

        assertTrue(visible.contains(">Nand Test - Repair History</h1>"), visible);
        assertTrue(visible.contains("href=\"/client\""), visible);
        assertTrue(visible.contains("Back to Clients"), visible);
        assertTrue(visible.contains("View All Repairs"), visible);
        assertTrue(visible.contains("href=\"/repair/create?clientId=9\""), visible);
        assertFalse(visible.contains(">Client</th>"), visible);
    }

    @Test
    void anEmptyListShowsTheNoRepairsStateAndNoTable() throws Exception {
        String html = renderList(List.of(), false, null);

        assertTrue(html.contains("No repairs on record yet"), html);
        assertFalse(html.contains("id=\"rpTable\""), html);
        assertNoNullWord(html);
    }

    // ---- The list, ticket mode --------------------------------------------------------------------

    @Test
    void ticketModeUsesTheTicketsWordingAndNewTicketButton() throws Exception {
        String html = renderList(List.of(
                repair(1L, "JO-00001", RepairStatus.PENDING),
                repair(2L, "JO-00002", RepairStatus.RELEASED)), true, null);
        String visible = visibleMarkup(html);

        assertTrue(visible.contains(">Tickets</h1>"), visible);
        assertTrue(visible.contains("Every job order logged at the shop, with its current status."), visible);
        assertTrue(visible.contains("New Ticket"), visible);
        assertTrue(visible.contains("href=\"/ticket/create\""), visible);
        assertFalse(visible.contains("Add Repair"), visible);
        assertTrue(visible.contains("Total Tickets"), visible);
        assertTrue(visible.contains(">Ticket #</th>"), visible);
        assertTrue(visible.contains(">Received</th>"), visible);
        assertTrue(visible.contains("Search by ticket number, client, device or issue"), visible);
        assertTrue(visible.contains("No tickets match these filters."), visible);
        assertTrue(visible.contains("2 ticket(s) total"), visible);
        assertFalse(visible.contains("repair(s)"), visible);
        assertEquals("Total Tickets=2", statCards(html).get(""), html);
        assertNoNullWord(html);
    }

    @Test
    void ticketModeHighlightsViewTicketsAndExpandsTheTicketsGroup() throws Exception {
        String html = renderList(List.of(repair(1L, "JO-00001", RepairStatus.PENDING)), true, null);

        assertTrue(Pattern.compile("<a href=\"/ticket/view\"[^>]*\\bactive\\b[^>]*>View Tickets").matcher(html).find(), html);
        assertTrue(Pattern.compile("class=\"collapse-panel[^\"]*\\bshow\\b[^\"]*\" data-panel=\"ticket\"").matcher(html).find(), html);
        // ...and Repairs is not the highlighted item.
        assertFalse(Pattern.compile("<a href=\"/repair\" class=\"nav-item[^\"]*\\bactive\\b").matcher(html).find(), html);
    }

    @Test
    void ticketModeDrillDownOffersViewAllTicketsNotViewAllRepairs() throws Exception {
        String visible = visibleMarkup(renderList(List.of(repair(1L, "JO-00001", RepairStatus.PENDING)), true, client()));

        assertTrue(visible.contains(">Nand Test - Tickets</h1>"), visible);
        assertTrue(visible.contains("View All Tickets"), visible);
        assertFalse(visible.contains("View All Repairs"), visible);
    }

    @Test
    void anEmptyTicketListSaysNoTicketsYet() throws Exception {
        assertTrue(renderList(List.of(), true, null).contains("No tickets yet"));
    }

    // The cancelled-repair cost rule: the cost on a CANCELLED repair is the diagnostic fee the shop kept.
    @Test
    void aCancelledRepairShowsItsFeeWithTheSuffixAndADashWhenThereWasNoCheckUp() throws Exception {
        RepairRecord fee = repair(1L, "JO-00001", RepairStatus.CANCELLED);
        fee.setCost(1500.0);
        RepairRecord noCheckUp = repair(2L, "JO-00002", RepairStatus.CANCELLED);
        noCheckUp.setCost(0.0);

        String list = visibleMarkup(renderList(List.of(fee, noCheckUp), false, null));
        String feeCard = renderCard(fee);
        String noCheckUpCard = renderCard(noCheckUp);

        assertTrue(readable(list).contains("Php 1,500.00 (diagnostic fee)"), list);
        assertTrue(list.contains(">—</span>"), list);
        assertFalse(list.contains("To be assessed"), list);

        assertTrue(readable(feeCard).contains("Php 1,500.00 (diagnostic fee)"), feeCard);
        assertTrue(noCheckUpCard.contains(">—</span>"), noCheckUpCard);
        assertFalse(feeCard.contains("To be assessed"), feeCard);
        assertFalse(noCheckUpCard.contains("To be assessed"), noCheckUpCard);
    }

    /** The visible text of some markup: tags dropped, runs of whitespace collapsed. */
    private static String readable(String markup) {
        return markup.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ");
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        for (int at = haystack.indexOf(needle); at >= 0; at = haystack.indexOf(needle, at + needle.length())) {
            count++;
        }
        return count;
    }
}
