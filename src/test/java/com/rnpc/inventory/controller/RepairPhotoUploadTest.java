package com.rnpc.inventory.controller;

import com.rnpc.inventory.dto.RepairRecordDto;
import com.rnpc.inventory.entity.Client;
import com.rnpc.inventory.entity.RepairRecord;
import com.rnpc.inventory.repository.ClientRepository;
import com.rnpc.inventory.repository.RepairRecordRepository;
import com.rnpc.inventory.service.ClientService;
import com.rnpc.inventory.service.NotificationService;
import com.rnpc.inventory.service.RepairRecordService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BindingResult;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Repair photos are screened (batch V1) on both the create and the update path, by the real
 * RepairRecordService and the real controller together; only the repositories and the other
 * services are mocked, so there is no Spring context and no database. A refused photo comes back as
 * a field error on imageFile with every other entry kept, and nothing is written to public/images/.
 * The two tests that accept a real photo do write a file there (unmocked, as in production) and
 * delete it again straight away, pass or fail.
 */
class RepairPhotoUploadTest {

    private static final Path UPLOAD_DIR = Paths.get("public/images");

    private final RepairRecordRepository repo = mock(RepairRecordRepository.class);
    private final ClientRepository clientRepo = mock(ClientRepository.class);
    private final ClientService clientService = mock(ClientService.class);
    private final MockMvc mvc;

    private final Authentication admin = new UsernamePasswordAuthenticationToken(
            "admin@example.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

    RepairPhotoUploadTest() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        RepairRecordService service = new RepairRecordService(repo, clientRepo);
        mvc = MockMvcBuilders.standaloneSetup(new RepairRecordController(
                        service, clientService, mock(NotificationService.class), validator))
                .setValidator(validator)
                .setViewResolvers(new InternalResourceViewResolver("/views/", ".html")).build();

        Client client = new Client();
        client.setClientId(1L);
        client.setFullName("Nand Test");
        when(clientRepo.findById(1L)).thenReturn(Optional.of(client));
        when(repo.save(any(RepairRecord.class))).thenAnswer(inv -> {
            RepairRecord r = inv.getArgument(0);
            if (r.getRepairId() == null) {
                r.setRepairId(11L);
            }
            return r;
        });
    }

    // ---- Fixtures ---------------------------------------------------------------------------------

    private RepairRecord existing() throws Exception {
        Client client = new Client();
        client.setClientId(1L);
        client.setFullName("Nand Test");
        RepairRecord r = new RepairRecord();
        r.setRepairId(5L);
        r.setJobOrderNumber("JO-00005");
        r.setClient(client);
        r.setDeviceType("Laptop");
        r.setBrand("Acer");
        r.setModelName("Aspire 5");
        r.setIssueDescription("no display");
        r.setRepairDate(new SimpleDateFormat("yyyy-MM-dd").parse("2026-10-02"));
        r.setStatus(RepairRecord.RepairStatus.PENDING);
        r.setImageFileName("already-stored.png");
        when(repo.findById(5L)).thenReturn(Optional.of(r));
        return r;
    }

    private static byte[] image(String format) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB), format, out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static MockMultipartFile photo(String name, String contentType, byte[] bytes) {
        return new MockMultipartFile("imageFile", name, contentType, bytes);
    }

    private static MockMultipartFile junkNamedJpg() {
        return photo("holiday.jpg", "image/jpeg", "not an image".getBytes());
    }

    private static MockMultipartFile noFileChosen() {
        return photo("", "application/octet-stream", new byte[0]);
    }

    // .param() appends, so a test that needs a different value builds the request with it here
    // rather than overriding afterwards (that would post two values).
    private MockMultipartHttpServletRequestBuilder createForm(MockMultipartFile file) {
        return fields(multipart("/repair/create").file(file), "1");
    }

    private MockMultipartHttpServletRequestBuilder editForm(MockMultipartFile file) {
        return fields(multipart(HttpMethod.PUT, "/repair/update/5").file(file), "1");
    }

    private MockMultipartHttpServletRequestBuilder fields(MockMultipartHttpServletRequestBuilder request, String clientId) {
        request.principal(admin)
                .param("clientId", clientId)
                .param("deviceType", "Laptop")
                .param("modelName", "Aspire 5")
                .param("issueDescription", "no display")
                .param("repairDate", "2026-10-02")
                .param("status", "PENDING")
                .param("cost", "0")
                .param("remarks", "Keep this remark");
        return request;
    }

    private static Set<String> uploadedFiles() throws IOException {
        if (!Files.isDirectory(UPLOAD_DIR)) {
            return Set.of();
        }
        try (Stream<Path> files = Files.list(UPLOAD_DIR)) {
            return files.map(p -> p.getFileName().toString()).collect(Collectors.toSet());
        }
    }

    private static void deleteUpload(String fileName) {
        if (fileName != null) {
            new File(UPLOAD_DIR.toFile(), fileName).delete();
        }
    }

    private static BindingResult bindingResultOf(MvcResult result) {
        return (BindingResult) result.getModelAndView().getModel()
                .get("org.springframework.validation.BindingResult.repairRecordDto");
    }

    /** The form came back (not a redirect) with exactly one error, on imageFile, and every entry kept. */
    private void assertPhotoRefused(MvcResult result, String view, String message) {
        assertEquals(view, result.getModelAndView().getViewName());
        BindingResult errors = bindingResultOf(result);
        assertEquals(1, errors.getErrorCount(), errors.getAllErrors().toString());
        assertEquals(1, errors.getFieldErrorCount("imageFile"));
        assertEquals("invalid", errors.getFieldError("imageFile").getCode());
        assertEquals(message, errors.getFieldError("imageFile").getDefaultMessage());

        RepairRecordDto kept = (RepairRecordDto) result.getModelAndView().getModel().get("repairRecordDto");
        assertEquals(1L, kept.getClientId());
        assertEquals("Laptop", kept.getDeviceType());
        assertEquals("Aspire 5", kept.getModelName());
        assertEquals("no display", kept.getIssueDescription());
        assertEquals("PENDING", kept.getStatus());
        assertEquals("Keep this remark", kept.getRemarks());
    }

    // ---- A refused photo -----------------------------------------------------------------------------

    @Test
    void aFakeJpgIsRefusedOnCreateWithAFieldErrorAndNothingIsSavedOrWritten() throws Exception {
        Set<String> before = uploadedFiles();

        MvcResult result = mvc.perform(createForm(junkNamedJpg()))
                .andExpect(status().isOk()).andReturn();

        assertPhotoRefused(result, "repairs/repairCreate", "Choose a valid JPG or PNG image.");
        verify(repo, never()).save(any());
        assertEquals(before, uploadedFiles(), "nothing may be written for a refused upload");
    }

    @Test
    void aFakeJpgIsRefusedOnEditWithAFieldErrorAndTheSavedRepairIsLeftAlone() throws Exception {
        RepairRecord saved = existing();
        Set<String> before = uploadedFiles();

        MvcResult result = mvc.perform(editForm(junkNamedJpg()).param("brand", "Typed"))
                .andExpect(status().isOk()).andReturn();

        assertPhotoRefused(result, "repairs/repairEdit", "Choose a valid JPG or PNG image.");
        verify(repo, never()).save(any());
        assertEquals(before, uploadedFiles(), "nothing may be written for a refused upload");
        // the posted brand never reached the saved record, and the stored photo is still the one on file
        assertEquals("Acer", saved.getBrand());
        assertEquals("already-stored.png", saved.getImageFileName());
        assertEquals("already-stored.png", result.getModelAndView().getModel().get("currentImage"));
        assertEquals(saved, result.getModelAndView().getModel().get("repair"));
    }

    @Test
    void anOversizeFileBetweenFiveAndTenMegabytesIsRefusedOnBothForms() throws Exception {
        existing();
        MockMultipartFile sixMb = photo("big.png", "image/png", new byte[6 * 1024 * 1024]);
        Set<String> before = uploadedFiles();

        assertPhotoRefused(mvc.perform(createForm(sixMb)).andReturn(), "repairs/repairCreate",
                "Choose a JPG or PNG no larger than 5 MB.");
        assertPhotoRefused(mvc.perform(editForm(sixMb)).andReturn(), "repairs/repairEdit",
                "Choose a JPG or PNG no larger than 5 MB.");

        verify(repo, never()).save(any());
        assertEquals(before, uploadedFiles());
    }

    @Test
    void aGifIsRefusedOnBothForms() throws Exception {
        existing();
        MockMultipartFile gif = photo("anim.gif", "image/gif", image("gif"));

        assertPhotoRefused(mvc.perform(createForm(gif)).andReturn(), "repairs/repairCreate",
                "Only JPG and PNG images are supported.");
        assertPhotoRefused(mvc.perform(editForm(gif)).andReturn(), "repairs/repairEdit",
                "Only JPG and PNG images are supported.");
        verify(repo, never()).save(any());
    }

    // ---- An accepted photo ---------------------------------------------------------------------------

    @Test
    void aValidPngIsSavedOnCreate() throws Exception {
        MockMultipartFile png = photo("technician.png", "image/png", image("png"));

        mvc.perform(createForm(png))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/repair?clientId=1"));

        ArgumentCaptor<RepairRecord> saved = ArgumentCaptor.forClass(RepairRecord.class);
        verify(repo, times(2)).save(saved.capture());
        String stored = saved.getAllValues().get(0).getImageFileName();
        try {
            assertNotNull(stored);
            assertTrue(stored.endsWith("_technician.png"), stored);
            assertTrue(new File(UPLOAD_DIR.toFile(), stored).isFile(), "the accepted photo should be on disk");
        } finally {
            deleteUpload(stored);
        }
    }

    @Test
    void aValidJpegReplacesTheStoredPhotoOnEdit() throws Exception {
        RepairRecord saved = existing();
        MockMultipartFile jpeg = photo("after.jpg", "image/jpeg", image("jpg"));

        mvc.perform(editForm(jpeg))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/repair?clientId=1"));

        String stored = saved.getImageFileName();
        try {
            assertTrue(stored.endsWith("_after.jpg"), stored);
            assertTrue(new File(UPLOAD_DIR.toFile(), stored).isFile(), "the accepted photo should be on disk");
        } finally {
            deleteUpload(stored);
        }
    }

    // ---- No new upload --------------------------------------------------------------------------------

    @Test
    void noFileChosenIsUnaffectedOnCreateAndEdit() throws Exception {
        RepairRecord saved = existing();
        Set<String> before = uploadedFiles();

        mvc.perform(createForm(noFileChosen())).andExpect(status().is3xxRedirection());
        mvc.perform(editForm(noFileChosen())).andExpect(status().is3xxRedirection());

        verify(repo, times(3)).save(any());
        assertEquals(before, uploadedFiles());
        // the photo already on file is neither re-checked (the file does not even exist) nor replaced
        assertEquals("already-stored.png", saved.getImageFileName());
    }

    // ---- Only a refused photo is attributed to the photo field ---------------------------------------

    @Test
    void anUnknownClientIsNotBlamedOnThePhoto() throws Exception {
        when(clientRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ServletException.class, () -> mvc.perform(
                fields(multipart("/repair/create").file(noFileChosen()), "99")));
    }
}
