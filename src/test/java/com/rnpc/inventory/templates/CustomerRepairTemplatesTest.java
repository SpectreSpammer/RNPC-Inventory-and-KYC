package com.rnpc.inventory.templates;

import com.rnpc.inventory.dto.RepairView;
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
 * Renders the customer repair templates (repairs batch U1) with the real Thymeleaf engine and the
 * real template files - a WebContext over spring-test's mock servlet objects, so no Spring context,
 * no controller, no database. It is the check that what a customer actually sees is free of remarks,
 * client details and the word "null".
 *
 * "null" and "()" are looked for only in the markup that is left once every script and style block
 * is removed: inside a script they are ordinary JavaScript, not a rendering bug.
 */
class CustomerRepairTemplatesTest {

    private static final Pattern SCRIPT_OR_STYLE =
            Pattern.compile("<script\\b.*?</script>|<style\\b.*?</style>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern WORD_NULL = Pattern.compile("\\bnull\\b");
    private static final Pattern STAT_CARD =
            Pattern.compile("data-status=\"(\\w*)\"[^>]*>.*?<p class=\"stat-value\">(\\d+)</p>", Pattern.DOTALL);

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
        variables.put("view", RepairView.from(repair));
        return render("repairs/repairDetail", "customerRepairCard", variables);
    }

    private static String renderList(List<RepairRecord> repairs) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("repairViews", repairs.stream().map(RepairView::from).toList());
        variables.put("currentUsername", "customer@example.com");
        variables.put("currentRole", "Customer");
        variables.put("unreadNotifications", 0);
        variables.put("recentNotifications", List.of());
        variables.put("navIsAdmin", false);
        return render("repairs/customerRepairIndex", null, variables);
    }

    /** What is left to read once scripts and styles are gone. */
    private static String visibleMarkup(String html) {
        return SCRIPT_OR_STYLE.matcher(html).replaceAll("");
    }

    private static void assertNoNullWord(String html) {
        Matcher m = WORD_NULL.matcher(visibleMarkup(html));
        assertFalse(m.find(), () -> "the word null appears outside scripts and styles, near: "
                + visibleMarkup(html).substring(Math.max(0, m.start() - 80), Math.min(visibleMarkup(html).length(), m.end() + 80)));
    }

    // ---- Fixtures ---------------------------------------------------------------------------------

    private static RepairRecord repair(long id, String jobOrder, RepairStatus status) throws Exception {
        Client client = new Client();
        client.setClientId(1L);
        client.setFullName("Nand Test");
        client.setContactNumber("0999 123 4567");
        RepairRecord r = new RepairRecord();
        r.setRepairId(id);
        r.setJobOrderNumber(jobOrder);
        r.setClient(client);
        r.setDeviceType("Laptop");
        r.setBrand("Acer");
        r.setModelName("Aspire 5");
        r.setIssueDescription("No display");
        r.setRepairDate(new SimpleDateFormat("yyyy-MM-dd").parse("2026-09-18"));
        r.setStatus(status);
        return r;
    }

    /** What an appointment-sourced repair looks like before an admin has filled anything in. */
    private static RepairRecord appointmentSourced() throws Exception {
        RepairRecord r = repair(2L, "JO-00002", RepairStatus.PENDING);
        r.setBrand(null);
        r.setModelName(null);
        r.setTechnician(null);
        r.setFix(null);
        r.setRecommendation(null);
        r.setWarrantyEndDate(null);
        r.setImageFileName(null);
        r.setCost(0.0);
        return r;
    }

    // ---- 1. The details fragment for a fully filled-in repair -------------------------------------

    @Test
    void theDetailsFragmentNeverShowsRemarksOrTheClientsNameOrPhone() throws Exception {
        RepairRecord r = repair(1L, "JO-00001", RepairStatus.COMPLETED);
        r.setRemarks("INTERNAL-NOTE-XYZ");
        r.setTechnician("Juan Dela Cruz");
        r.setFix("Replaced the display cable");
        r.setRecommendation("Back up your files");
        r.setCost(2000.0);
        r.setImageFileName("fixed.png");

        String html = renderCard(r);

        // It really rendered the repair...
        assertTrue(html.contains("JO-00001"), html);
        assertTrue(html.contains("Juan Dela Cruz"), html);
        assertTrue(html.contains("Replaced the display cable"), html);
        assertTrue(html.contains("/images/fixed.png"), html);
        // ...and none of what must stay internal.
        assertFalse(html.contains("INTERNAL-NOTE-XYZ"), html);
        assertFalse(html.contains("Nand Test"), html);
        assertFalse(html.contains("0999 123 4567"), html);
        assertFalse(html.contains("09991234567"), html);
        assertNoNullWord(html);
    }

    // ---- 2. The details fragment for an appointment-sourced repair --------------------------------

    @Test
    void anAppointmentSourcedRepairRendersCleanlyWithNothingFilledIn() throws Exception {
        String html = renderCard(appointmentSourced());
        String visible = visibleMarkup(html);

        assertNoNullWord(html);
        assertFalse(visible.contains("()"), "empty parentheses in: " + visible);
        assertTrue(visible.contains("Laptop"), visible);

        // No photo block at all, and no image.
        assertFalse(html.contains("class=\"rd-photo\""), html);
        assertFalse(html.contains("<img"), html);
        assertFalse(html.contains("Photo from the technician"), html);

        assertTrue(html.contains("To be assessed"), html);
        // th:text escapes the apostrophes in the sentence as &#39;.
        assertTrue(html.replace("&#39;", "'").contains("We've received your device. It's waiting to be looked at."), html);
        assertEquals(1, countOccurrences(html, "<li class=\"is-done\">"), html);
        assertEquals(4, countOccurrences(html, "class=\"rd-step-bar\""), html);
    }

    // ---- 3. The list page -------------------------------------------------------------------------

    @Test
    void theListShowsBothJobOrdersAndTheRightStatCounts() throws Exception {
        RepairRecord pending = repair(1L, "JO-00001", RepairStatus.PENDING);
        RepairRecord released = repair(2L, "JO-00002", RepairStatus.RELEASED);
        released.setCost(1500.0);

        String html = renderList(List.of(pending, released));

        assertTrue(html.contains("JO-00001"), html);
        assertTrue(html.contains("JO-00002"), html);

        Map<String, String> counts = new HashMap<>();
        Matcher m = STAT_CARD.matcher(html);
        while (m.find()) {
            counts.put(m.group(1), m.group(2));
        }
        assertEquals(Map.of(
                "", "2",
                "PENDING", "1",
                "IN_PROGRESS", "0",
                "COMPLETED", "0",
                "RELEASED", "1",
                "CANCELLED", "0"), counts, html);

        assertNoNullWord(html);
    }

    @Test
    void theListWithNoRepairsShowsTheEmptyStateAndSixZeroCards() {
        String html = renderList(List.of());

        assertTrue(html.contains("No repairs on record yet"), html);
        assertFalse(html.contains("id=\"rpTable\""), html);

        Map<String, String> counts = new HashMap<>();
        Matcher m = STAT_CARD.matcher(html);
        while (m.find()) {
            counts.put(m.group(1), m.group(2));
        }
        assertEquals(Map.of(
                "", "0",
                "PENDING", "0",
                "IN_PROGRESS", "0",
                "COMPLETED", "0",
                "RELEASED", "0",
                "CANCELLED", "0"), counts, html);

        assertNoNullWord(html);
    }

    @Test
    void theListNeverCarriesRemarksOrClientDetailsEither() throws Exception {
        RepairRecord r = repair(1L, "JO-00001", RepairStatus.PENDING);
        r.setRemarks("INTERNAL-NOTE-XYZ");

        // Unlike the null check, this one covers scripts too: ALL_REPAIRS is in the page source.
        String html = renderList(List.of(r));

        assertFalse(html.contains("INTERNAL-NOTE-XYZ"), html);
        assertFalse(html.contains("Nand Test"), html);
        assertFalse(html.contains("0999 123 4567"), html);
    }

    // The cancelled-repair cost rule: the cost on a CANCELLED repair is the diagnostic fee the shop kept.
    @Test
    void aCancelledRepairShowsItsFeeWithTheSuffixAndADashWhenThereWasNoCheckUp() throws Exception {
        RepairRecord fee = repair(1L, "JO-00001", RepairStatus.CANCELLED);
        fee.setCost(1500.0);
        RepairRecord noCheckUp = repair(2L, "JO-00002", RepairStatus.CANCELLED);
        noCheckUp.setCost(0.0);

        String list = visibleMarkup(renderList(List.of(fee, noCheckUp)));
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
