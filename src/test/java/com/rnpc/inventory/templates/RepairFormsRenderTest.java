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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * The admin edit and add repair forms (repairs batch A2), rendered the way the app renders them:
 * the real RepairRecordController behind a real ThymeleafViewResolver in a standalone MockMvc, with
 * mocked services. The forms bind with th:field, which needs Spring's RequestContext and a
 * BindingResult, so a bare template engine is not enough. No Spring context, no database.
 *
 * The Cancelled cost hint has its own test (RepairFormCancelledHintTest). The browser half of the
 * forms - that script, and the shared remove-photo script - needs a real browser.
 *
 * "null" is looked for only outside script and style blocks, where it is ordinary JavaScript.
 */
class RepairFormsRenderTest {

    private static final Pattern SCRIPT_OR_STYLE =
            Pattern.compile("<script\\b.*?</script>|<style\\b.*?</style>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern WORD_NULL = Pattern.compile("\\bnull\\b");

    private final RepairRecordService repairService = mock(RepairRecordService.class);
    private final ClientService clientService = mock(ClientService.class);
    private final MockMvc mvc;

    private final Authentication admin = new UsernamePasswordAuthenticationToken(
            "admin@example.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

    RepairFormsRenderTest() {
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

        Client nand = client(1L, "Nand Test");
        Client robin = client(2L, "robin");
        when(clientService.getAllClients()).thenReturn(List.of(nand, robin));
    }

    // ---- Fixtures ---------------------------------------------------------------------------------

    private static Client client(long id, String name) {
        Client c = new Client();
        c.setClientId(id);
        c.setFullName(name);
        return c;
    }

    private RepairRecord repair(RepairStatus status) throws Exception {
        RepairRecord r = new RepairRecord();
        r.setRepairId(5L);
        r.setJobOrderNumber("JO-00005");
        r.setClient(client(1L, "Nand Test"));
        r.setDeviceType("Laptop");
        r.setBrand("Acer");
        r.setModelName("Aspire 5");
        r.setIssueDescription("no display");
        r.setRepairDate(new SimpleDateFormat("yyyy-MM-dd").parse("2026-10-02"));
        r.setCreatedAt(new SimpleDateFormat("yyyy-MM-dd").parse("2026-10-03"));
        r.setStatus(status);
        return r;
    }

    /** A repair as an appointment creates it: no brand, model, technician, fix or receiver. */
    private RepairRecord sparse() throws Exception {
        RepairRecord r = repair(RepairStatus.PENDING);
        r.setBrand(null);
        r.setModelName(null);
        r.setTechnician(null);
        r.setFix(null);
        r.setRecommendation(null);
        r.setRemarks(null);
        r.setSerialNumber(null);
        r.setReceivedByEmployeeId(null);
        return r;
    }

    private String render(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.principal(admin)).andReturn().getResponse().getContentAsString();
    }

    private String editForm(RepairRecord repair) throws Exception {
        when(repairService.getRepairRecordById(5L)).thenReturn(repair);
        return render(get("/repair/edit/5"));
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

    // ---- Edit form: header, shell, sections -------------------------------------------------------

    @Test
    void theEditFormHasTheDesignsHeaderSubtitleAndStatusPill() throws Exception {
        String html = editForm(repair(RepairStatus.PENDING));
        String text = readable(html);

        assertTrue(html.contains("href=\"/repair\""), html);
        assertTrue(text.contains("Back to repairs"), text);
        assertTrue(html.contains(">Edit repair</h1>"), html);
        assertTrue(text.contains("Job order JO-00005 for Nand Test. Add findings, parts and cost as the work moves along."), text);
        assertTrue(html.contains("badge-pending"), "status pill missing: " + html);
        assertTrue(text.contains("Pending"), text);
    }

    @Test
    void theEditFormSitsInTheShellWithRepairsActive() throws Exception {
        String html = editForm(repair(RepairStatus.PENDING));

        assertTrue(html.contains("class=\"sidebar\""), "layout-app sidebar missing");
        assertTrue(Pattern.compile("<a href=\"/repair\" class=\"nav-item[^\"]*\\bactive\\b").matcher(html).find(),
                "Repairs is not the active sidebar item");
    }

    @Test
    void theEditFormHasTheFourSectionsAndTheOptionalMarkers() throws Exception {
        String text = readable(editForm(repair(RepairStatus.PENDING)));

        assertTrue(text.contains("Client and device"), text);
        assertTrue(text.contains("Issue and findings"), text);
        assertTrue(text.contains("Status and cost"), text);
        assertTrue(text.contains("Photo for technician reference (optional, the client can see it)"), text);

        assertTrue(text.contains("Brand (optional)"), text);
        assertTrue(text.contains("Serial number (optional)"), text);
        assertTrue(text.contains("Technician (optional)"), text);
        assertTrue(text.contains("Fix (required once Completed or Released)"), text);
        assertTrue(text.contains("Recommendation (optional)"), text);
        assertTrue(text.contains("Remarks (optional, shop only)"), text);
        assertTrue(text.contains("Warranty ends (optional)"), text);
        // required fields carry no marker
        assertFalse(text.contains("Model name (optional)"), text);
        assertFalse(text.contains("Issue description (optional)"), text);
    }

    @Test
    void theClientSelectListsEveryClientWithTheRepairsOwnSelected() throws Exception {
        String html = editForm(repair(RepairStatus.PENDING));

        assertTrue(Pattern.compile("<option[^>]*value=\"1\"[^>]*selected").matcher(html).find(), html);
        assertFalse(Pattern.compile("<option[^>]*value=\"2\"[^>]*selected").matcher(html).find(), html);
        assertTrue(html.contains(">robin</option>"), html);
        assertTrue(readable(html).contains("Changing the client moves this repair to their record."));
    }

    @Test
    void theJobOrderNumberIsReadOnlyAndNotPosted() throws Exception {
        String html = editForm(repair(RepairStatus.PENDING));
        Matcher m = Pattern.compile("<input[^>]*id=\"jobOrderNumber\"[^>]*>").matcher(html);

        assertTrue(m.find(), html);
        assertTrue(m.group().contains("value=\"JO-00005\""), m.group());
        assertTrue(m.group().contains("readonly"), m.group());
        assertFalse(m.group().contains("name="), "a read-only job order must not be a posted field: " + m.group());
    }

    // ---- Edit form: the Record card -----------------------------------------------------------------

    @Test
    void theRecordCardShowsReceivedReceivedByAndBothButtons() throws Exception {
        RepairRecord r = repair(RepairStatus.PENDING);
        r.setReceivedByEmployeeId("Test Admin-12345");

        String html = editForm(r);
        String text = readable(html);

        assertTrue(text.contains("Record"), text);
        assertTrue(text.contains("Received Oct 3, 2026"), text);
        assertTrue(text.contains("Received by Test Admin-12345"), text);
        assertTrue(html.contains("href=\"/ticket/5/pdf\""), html);
        assertTrue(text.contains("View client copy"), text);

        // Delete keeps the list's confirm and DELETE flow exactly: its own form, _method=delete, confirm().
        Matcher form = Pattern.compile("<form[^>]*action=\"/repair/delete/5\"[^>]*>.*?</form>", Pattern.DOTALL).matcher(html);
        assertTrue(form.find(), "delete form missing: " + html);
        assertTrue(form.group().contains("method=\"post\""), form.group());
        assertTrue(form.group().contains("name=\"_method\" value=\"delete\""), form.group());
        assertTrue(form.group().contains("onclick=\"return confirm('Are you sure?')\""), form.group());
        assertTrue(form.group().contains("Delete repair"), form.group());
    }

    @Test
    void viewClientCopyOpensInANewTabSoTheFormKeepsItsUnsavedEntries() throws Exception {
        String html = editForm(repair(RepairStatus.PENDING));
        Matcher link = Pattern.compile("<a[^>]*href=\"/ticket/5/pdf\"[^>]*>").matcher(html);

        assertTrue(link.find(), "client copy link missing: " + html);
        assertTrue(link.group().contains("target=\"_blank\""), link.group());
        assertTrue(link.group().contains("rel=\"noopener noreferrer\""), link.group());
        assertTrue(readable(html).contains("View client copy"), readable(html));
        // only that one link opens a new tab; Cancel and Back stay in this one
        assertTrue(Pattern.compile("target=\"_blank\"").matcher(html).results().count() == 1, html);
    }

    @Test
    void aRepairWithNoReceiverHasNoReceivedByRowAtAll() throws Exception {
        for (String receiver : new String[]{null, "", "   "}) {
            RepairRecord r = repair(RepairStatus.PENDING);
            r.setReceivedByEmployeeId(receiver);

            String html = editForm(r);

            assertFalse(readable(html).contains("Received by"), "receiver=[" + receiver + "]: " + readable(html));
            // the other row is still there, and no row is left empty
            assertTrue(readable(html).contains("Received Oct 3, 2026"), readable(html));
            assertFalse(Pattern.compile("<dt>Received by</dt>").matcher(html).find(), html);
        }
    }

    @Test
    void aRepairWithNoCreatedDateHasNoReceivedRowEither() throws Exception {
        RepairRecord r = repair(RepairStatus.PENDING);
        r.setCreatedAt(null);

        String html = editForm(r);

        assertFalse(html.contains("<dt>Received</dt>"), html);
        assertFalse(html.contains("<dt>Received by</dt>"), html);
        assertTrue(readable(html).contains("View client copy"), readable(html));
        assertNoNullWord(html);
    }

    // ---- Edit form: the photo -----------------------------------------------------------------------

    @Test
    void withAPhotoTheFormShowsItWithTheSharedRemoveButtonAndScript() throws Exception {
        RepairRecord r = repair(RepairStatus.PENDING);
        r.setImageFileName("fixed.png");

        String html = editForm(r);

        assertTrue(html.contains("/images/fixed.png"), html);
        assertTrue(Pattern.compile("class=\"pf-remove remove-photo-btn\"[^>]*data-remove-url=\"/repair/removePhoto/5\"")
                .matcher(html).find(), html);
        assertFalse(readable(html).contains("No photo yet"), readable(html));
        // the shared remove-photo script: 303-aware (res.ok) and sends X-Requested-With ...
        assertTrue(html.contains("'X-Requested-With': 'XMLHttpRequest'"), "shared remove-photo script missing");
        assertTrue(html.contains("res.ok"), "shared remove-photo script missing");
        // ... and the old inline one (confirm + reload, no header, no res.ok) is gone
        assertFalse(html.contains("window.location.reload"), "old inline remove-photo script is still there");
        assertFalse(html.contains("Remove the current photo?'"), "old inline remove-photo script is still there");
    }

    @Test
    void withoutAPhotoTheFormSaysNoPhotoYetAndOffersNoRemoveButton() throws Exception {
        String html = editForm(repair(RepairStatus.PENDING));

        assertTrue(readable(html).contains("No photo yet"), readable(html));
        assertFalse(html.contains("remove-photo-btn\""), html);
        assertTrue(html.contains("type=\"file\""), html);
        // the 5 MB note is not printed until a repair photo validator exists
        assertFalse(readable(html).contains("up to 5 MB"), readable(html));
    }

    // ---- Edit form: nothing prints null -------------------------------------------------------------

    @Test
    void aRepairWithNullBrandModelTechnicianAndFixNeverPrintsNull() throws Exception {
        String html = editForm(sparse());

        assertNoNullWord(html);
        assertTrue(readable(html).contains("Job order JO-00005 for Nand Test."), readable(html));
    }

    @Test
    void aRepairWithNoClientNameStillReadsCleanly() throws Exception {
        RepairRecord r = sparse();
        r.getClient().setFullName(null);

        String html = editForm(r);

        assertNoNullWord(html);
        assertTrue(readable(html).contains("Job order JO-00005 for the client."), readable(html));
    }

    // ---- Edit form: a rejected save keeps everything, errors sit under the field ----------------------

    @Test
    void aRejectedEditKeepsTheEntriesAndShowsTheExistingMessagesUnderTheirFields() throws Exception {
        when(repairService.getRepairRecordById(5L)).thenReturn(repair(RepairStatus.PENDING));

        String html = render(put("/repair/update/5")
                .param("clientId", "2").param("deviceType", "Desktop").param("modelName", "Keep this model")
                .param("issueDescription", "").param("repairDate", "2026-10-02")
                .param("cost", "-5").param("status", "COMPLETED").param("remarks", "Keep this remark"));
        String text = readable(html);

        // messages unchanged, including the finished-repair group rules for Fix and Cost
        assertTrue(text.contains("The issue description is required!"), text);
        assertTrue(text.contains("The fix is required once the repair is Completed or Released."), text);
        assertTrue(text.contains("A cost greater than zero is required once the repair is Completed or Released."), text);
        // every other entry is kept
        assertTrue(html.contains("value=\"Keep this model\""), html);
        assertTrue(html.contains("Keep this remark"), html);
        assertTrue(Pattern.compile("<option[^>]*value=\"2\"[^>]*selected").matcher(html).find(), html);
        // the header and Record card still describe the saved repair, not the posted values
        assertTrue(text.contains("Job order JO-00005 for Nand Test."), text);
        assertNoNullWord(html);
    }

    // ---- Add form ------------------------------------------------------------------------------------

    @Test
    void theAddFormHasNoJobOrderNoRecordCardAndNoRemoveButton() throws Exception {
        String html = render(get("/repair/create"));
        String text = readable(html);

        assertTrue(html.contains(">Add repair</h1>"), html);
        assertTrue(text.contains("Back to repairs"), text);
        assertTrue(html.contains("href=\"/repair\""), html);
        assertTrue(text.contains("Client and device"), text);
        assertTrue(text.contains("Fix (required once Completed or Released)"), text);
        assertTrue(text.contains("Brand (optional)"), text);

        assertFalse(html.contains("id=\"jobOrderNumber\""), html);
        assertFalse(text.contains("Job order number"), text);
        assertFalse(text.contains("View client copy"), text);
        assertFalse(html.contains("/repair/delete/"), html);
        assertFalse(html.contains("remove-photo-btn\""), html);
        assertFalse(html.contains("Changing the client moves"), html);
        assertNoNullWord(html);
    }

    @Test
    void theAddFormPreselectsTheClientFromTheQueryString() throws Exception {
        String html = render(get("/repair/create").param("clientId", "2"));

        assertTrue(Pattern.compile("<option[^>]*value=\"2\"[^>]*selected").matcher(html).find(), html);
        assertFalse(Pattern.compile("<option[^>]*value=\"1\"[^>]*selected").matcher(html).find(), html);
    }
}
