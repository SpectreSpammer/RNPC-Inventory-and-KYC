package com.rnpc.inventory.templates;

import com.rnpc.inventory.controller.ClientController;
import com.rnpc.inventory.entity.Client;
import com.rnpc.inventory.entity.RepairRecord;
import com.rnpc.inventory.entity.RepairRecord.RepairStatus;
import com.rnpc.inventory.entity.User;
import com.rnpc.inventory.service.ClientService;
import com.rnpc.inventory.service.NotificationService;
import com.rnpc.inventory.service.RepairRecordService;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
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

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * The customer details page (customers batch C1) and the client forms' back link, rendered the way
 * the app renders them: the real ClientController behind a real ThymeleafViewResolver in a
 * standalone MockMvc, with mocked services. The forms bind with th:field, which needs Spring's
 * RequestContext, so a bare template engine is not enough. No Spring context, no database.
 *
 * "null" is looked for only outside script and style blocks, where it is ordinary JavaScript. The
 * Customers list builds its rows in the browser, so its icons are checked in the template source.
 */
class ClientPagesRenderTest {

    private static final Pattern SCRIPT_OR_STYLE =
            Pattern.compile("<script\\b.*?</script>|<style\\b.*?</style>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern WORD_NULL = Pattern.compile("\\bnull\\b");
    private static final String SECRET_REMARK = "SECRET-SHOP-REMARK-DO-NOT-SHOW";

    private final ClientService clientService = mock(ClientService.class);
    private final RepairRecordService repairService = mock(RepairRecordService.class);
    private final MockMvc mvc;

    private final Authentication admin = new UsernamePasswordAuthenticationToken(
            "admin@example.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

    ClientPagesRenderTest() {
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

        mvc = MockMvcBuilders.standaloneSetup(
                        new ClientController(clientService, repairService, mock(NotificationService.class)))
                .setValidator(validator).setViewResolvers(views).build();
    }

    // ---- Fixtures ---------------------------------------------------------------------------------

    /** A walk-in as the ticket form can leave one: a name and nothing else. */
    private static Client walkIn() {
        Client c = new Client();
        c.setClientId(12L);
        c.setFullName("Nand Test");
        return c;
    }

    private static Client linked() throws Exception {
        Client c = new Client();
        c.setClientId(12L);
        c.setFullName("Nand Test");
        c.setContactNumber("09172345678");
        c.setEmail("nand.contact@example.com");
        c.setAddress("12 Rizal St, Quezon City");
        c.setCreatedAt(new SimpleDateFormat("yyyy-MM-dd").parse("2026-09-18"));
        c.setImageFileName("1793000000000_id.png");
        User u = new User();
        u.setUsername("nand-user");
        u.setEmail("nand.account@example.com");
        c.setUser(u);
        return c;
    }

    private RepairRecord repair(long id, String jobOrder, RepairStatus status, double cost) throws Exception {
        RepairRecord r = new RepairRecord();
        r.setRepairId(id);
        r.setJobOrderNumber(jobOrder);
        r.setClient(linked());
        r.setDeviceType("Laptop");
        r.setBrand("Acer");
        r.setModelName("Aspire 5");
        r.setIssueDescription("no display");
        r.setRepairDate(new SimpleDateFormat("yyyy-MM-dd").parse("2026-10-02"));
        r.setStatus(status);
        r.setCost(cost);
        r.setRemarks(SECRET_REMARK + "-" + id);
        return r;
    }

    private String render(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.principal(admin)).andReturn().getResponse().getContentAsString();
    }

    private static String visibleMarkup(String html) {
        return SCRIPT_OR_STYLE.matcher(html).replaceAll("");
    }

    private static void assertNoNullWord(String html) {
        String visible = visibleMarkup(html);
        Matcher m = WORD_NULL.matcher(visible);
        assertFalse(m.find(), () -> "the word null appears outside scripts and styles, near: "
                + visible.substring(Math.max(0, m.start() - 80), Math.min(visible.length(), m.end() + 80)));
    }

    /** The visible text of some markup: tags dropped, runs of whitespace collapsed. */
    private static String readable(String markup) {
        return visibleMarkup(markup).replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ");
    }

    /** The page's pf-back link, not the whole page: a template comment may still quote the old wording. */
    private static void assertBackLinkToCustomers(String html) {
        Matcher m = Pattern.compile("<a class=\"pf-back\" href=\"([^\"]*)\">(.*?)</a>", Pattern.DOTALL).matcher(html);
        assertTrue(m.find(), "no pf-back link found: " + html);
        assertTrue(m.group(1).equals("/client"), "the back link should go to /client but goes to " + m.group(1));
        String label = m.group(2).replaceAll("<[^>]*>", " ").replaceAll("\s+", " ").trim();
        assertTrue(label.equals("Back to customers"), "the back link reads: " + label);
    }

    private static int count(String text, String needle) {
        int n = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + needle.length())) {
            n++;
        }
        return n;
    }

    // ---- Details page: a walk-in with nothing filled in ---------------------------------------------

    @Test
    void aWalkInWithBlankFieldsShowsNotSetNoPhotoYetAndWalkInClientNeverNull() throws Exception {
        when(clientService.getClientById(12L)).thenReturn(walkIn());

        String html = render(get("/client/view/12"));
        String text = readable(html);

        assertTrue(text.contains("Customer details") || text.contains("Nand Test"), text);
        assertTrue(text.contains("Nand Test"), text);
        // contact number, email and address are all blank
        assertTrue(count(text, "Not set") >= 3, "Not set should stand in for all three blank fields: " + text);
        assertTrue(text.contains("No photo yet"), text);
        assertTrue(text.contains("Walk-in client"), text);
        assertTrue(text.contains("No job orders yet"), text);
        assertFalse(html.contains("<img class=\"cd-photo\""), "no image element without a photo");
        assertNoNullWord(html);
    }

    @Test
    void theHeaderHasBackEditAddRepairAndViewRepairsLinks() throws Exception {
        when(clientService.getClientById(12L)).thenReturn(walkIn());

        String html = render(get("/client/view/12"));

        assertTrue(readable(html).contains("Back to customers"), html);
        assertTrue(html.contains("href=\"/client\""), html);
        assertTrue(html.contains("href=\"/client/edit/12\""), html);
        assertTrue(html.contains("href=\"/repair/create?clientId=12\""), html);
        assertTrue(html.contains("href=\"/repair?clientId=12\""), html);
    }

    // ---- Details page: a client with an account, a photo and repairs --------------------------------

    @Test
    void aLinkedClientShowsItsDetailsPhotoAccountAndJobOrdersButNoRemarks() throws Exception {
        when(clientService.getClientById(12L)).thenReturn(linked());
        when(repairService.getRepairRecordsByClient(12L)).thenReturn(List.of(
                repair(5, "JO-00005", RepairStatus.COMPLETED, 2500),
                repair(4, "JO-00004", RepairStatus.CANCELLED, 1500),
                repair(3, "JO-00003", RepairStatus.PENDING, 0)));

        String html = render(get("/client/view/12"));
        String text = readable(html);

        // details
        assertTrue(text.contains("0917 234 5678"), text);
        assertTrue(text.contains("nand.contact@example.com"), text);
        assertTrue(text.contains("12 Rizal St, Quezon City"), text);
        assertTrue(text.contains("Added Sep 18, 2026"), text);
        assertFalse(text.contains("Walk-in client"), text);
        assertFalse(text.contains("No photo yet"), text);
        // the photo is served from the same /images/ URL as the edit form
        assertTrue(html.contains("src=\"/images/1793000000000_id.png\""), html);
        // the linked account's email, not the username
        assertTrue(text.contains("nand.account@example.com"), text);
        assertFalse(text.contains("nand-user"), text);
        // job orders: number, device, status label, date and cost text from AdminRepairView
        assertTrue(text.contains("JO-00005"), text);
        assertTrue(text.contains("JO-00004"), text);
        assertTrue(text.contains("JO-00003"), text);
        assertTrue(text.contains("Laptop (Acer Aspire 5)"), text);
        assertTrue(text.contains("Completed"), text);
        assertTrue(text.contains("Cancelled"), text);
        assertTrue(text.contains("Pending"), text);
        assertTrue(text.contains("Oct 02, 2026"), text);
        assertTrue(text.contains("Php 2,500.00"), text);
        assertTrue(text.contains("Php 1,500.00 (diagnostic fee)") || (text.contains("Php 1,500.00") && text.contains("(diagnostic fee)")), text);
        assertTrue(text.contains("To be assessed"), text);
        assertNoNullWord(html);

        // remarks: non-empty on every repair, and printed nowhere - not even in markup or scripts
        assertFalse(html.contains(SECRET_REMARK), "shop remarks must never reach the page");
        assertFalse(text.contains("No remarks"), text);
        assertFalse(text.toLowerCase().contains("remarks"), text);
    }

    @Test
    void aLinkedAccountWithNoEmailShowsItsUsername() throws Exception {
        Client c = linked();
        c.getUser().setEmail("");
        when(clientService.getClientById(12L)).thenReturn(c);

        String text = readable(render(get("/client/view/12")));

        assertTrue(text.contains("nand-user"), text);
    }

    // ---- Back link on both client forms -------------------------------------------------------------

    @Test
    void theEditFormSaysBackToCustomersAndLinksToTheList() throws Exception {
        when(clientService.getClientById(12L)).thenReturn(walkIn());

        String html = render(get("/client/edit/12"));

        assertBackLinkToCustomers(html);
        assertTrue(readable(html).contains("Edit Nand Test"), html);
    }

    @Test
    void theAddFormSaysBackToCustomersAndLinksToTheList() throws Exception {
        String html = render(get("/client/create"));

        assertBackLinkToCustomers(html);
        assertTrue(readable(html).contains("Add client"), html);
    }

    // ---- Customers list: the icons are built in the browser, so check the template source ------------

    @Test
    void theCustomersListIconsAreNamedAndTheEyeOpensTheDetailsPage() throws Exception {
        String source = new String(new ClassPathResource("templates/clients/clientIndex.html")
                .getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        assertTrue(source.contains("view.href = '/client/view/' + c.id;"), source);
        assertTrue(source.contains("view.title = 'View customer';"), source);
        assertTrue(source.contains("'View customer ' + c.fullName"), source);
        assertTrue(source.contains("edit.title = 'Edit customer';"), source);
        assertTrue(source.contains("'Edit customer ' + c.fullName"), source);
        assertTrue(source.contains("del.title = 'Delete customer';"), source);
        assertTrue(source.contains("'Delete customer ' + c.fullName"), source);
        assertFalse(source.contains("View job orders"), source);
        // still three icons: view, edit, delete
        assertTrue(source.contains("acts.append(view, edit, del);"), source);
    }
}
