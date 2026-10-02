package com.rnpc.inventory.service;

import com.rnpc.inventory.dto.ClientDto;
import com.rnpc.inventory.entity.Client;
import com.rnpc.inventory.repository.ClientRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cross-format matching, duplicate detection and the stored format in ClientService. The repository
 * is mocked - no Spring context, no database.
 */
class ClientServiceTest {

    private final ClientRepository repo = mock(ClientRepository.class);
    private final ClientService service = new ClientService(repo, mock(RepairRecordService.class));

    private static Client client(long id, String name, String number) {
        Client c = new Client();
        c.setClientId(id);
        c.setFullName(name);
        c.setContactNumber(number);
        return c;
    }

    private void stored(Client... clients) {
        when(repo.findAll(any(Sort.class))).thenReturn(List.of(clients));
        when(repo.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void aTicketWithAnyFormatOfTheNumberFindsTheSameClient() {
        stored(client(1, "Nand", "0917 234 5678"), client(2, "Robin", "0908 123 9876"));

        for (String typed : List.of("09172345678", "0917 234 5678", "0917-234-5678", "+639172345678",
                "+63 917 234 5678", "9172345678")) {
            assertEquals(1L, service.findByContactNumber(typed).orElseThrow().getClientId(), typed);
        }
        assertEquals(2L, service.findByContactNumber("+63 908 123 9876").orElseThrow().getClientId());
    }

    @Test
    void aStoredRowInAnOldFormatStillMatches() {
        // Rows from before the format rule hold whatever was typed.
        stored(client(1, "Nand", "+639172345678"), client(2, "Robin", "0908-123-9876"));

        assertEquals(1L, service.findByContactNumber("0917 234 5678").orElseThrow().getClientId());
        assertEquals(2L, service.findByContactNumber("09081239876").orElseThrow().getClientId());
    }

    @Test
    void aLandlineMatchesOnDigitsAlone() {
        stored(client(1, "Shop", "(02) 8123 4567"));

        assertTrue(service.findByContactNumber("02-8123-4567").isPresent());
        assertTrue(service.findByContactNumber("+63 2 8123 4567").isPresent());
    }

    @Test
    void anUnknownOrBlankNumberFindsNothing() {
        stored(client(1, "Nand", "0917 234 5678"));

        assertTrue(service.findByContactNumber("0999 000 0000").isEmpty());
        assertTrue(service.findByContactNumber("").isEmpty());
        assertTrue(service.findByContactNumber(null).isEmpty());
    }

    @Test
    void existingDuplicatesResolveToTheOldestClientInsteadOfThrowing() {
        // Two rows that share a number once normalised: the outage this batch fixes.
        stored(client(3, "Nand (old)", "0917 234 5678"), client(9, "Nand (new)", "09172345678"));

        assertEquals(3L, service.findByContactNumber("+639172345678").orElseThrow().getClientId());
        assertEquals(2, service.findAllByContactNumber("0917-234-5678").size());
    }

    @Test
    void duplicateDetectionNamesTheOtherClientAndIgnoresTheClientBeingEdited() {
        stored(client(1, "Nand Test", "0917 234 5678"), client(2, "Robin", "0908 123 9876"));

        // Adding a client (no id to exclude) with Nand's number in another format.
        Client clash = service.findOtherWithContactNumber("+63 917-234-5678", null).orElseThrow();
        assertEquals("Nand Test", clash.getFullName());
        assertEquals(1L, clash.getClientId());

        // Editing Robin to Nand's number is a clash; editing Nand and keeping his own number is not.
        assertEquals(1L, service.findOtherWithContactNumber("09172345678", 2L).orElseThrow().getClientId());
        assertTrue(service.findOtherWithContactNumber("09172345678", 1L).isEmpty());
        // A free number never clashes.
        assertTrue(service.findOtherWithContactNumber("0999 000 0000", null).isEmpty());
    }

    // ---- Fill missing details on a ticket match, never overwrite (batch 5) -------------------------

    @Test
    void fillMissingDetailsFillsOnlyTheBlankFields() {
        Client existing = client(4, null, "0917 234 5678");
        existing.setEmail("already-on-file@example.com");
        existing.setAddress(null);

        service.fillMissingDetails(existing, "Typed Full Name", "typed@example.com", "Typed Address");

        assertEquals("Typed Full Name", existing.getFullName());
        assertEquals("already-on-file@example.com", existing.getEmail());
        assertEquals("Typed Address", existing.getAddress());
        verify(repo).save(existing);
    }

    @Test
    void fillMissingDetailsNeverOverwritesAnExistingValue() {
        Client existing = client(5, "Nand Test", "0917 234 5678");
        existing.setEmail("already-on-file@example.com");
        existing.setAddress("Already on file");

        service.fillMissingDetails(existing, "A Different Typed Name", "different@example.com", "A different address");

        assertEquals("Nand Test", existing.getFullName());
        assertEquals("already-on-file@example.com", existing.getEmail());
        assertEquals("Already on file", existing.getAddress());
        verify(repo, never()).save(any());
    }

    @Test
    void fillMissingDetailsIgnoresABlankTypedValue() {
        Client existing = client(6, "Nand Test", "0917 234 5678");
        existing.setEmail(null);
        existing.setAddress(null);

        service.fillMissingDetails(existing, "Nand Test", "   ", "");

        assertNull(existing.getEmail());
        assertNull(existing.getAddress());
        verify(repo, never()).save(any());
    }

    private static ClientDto dto(String number) {
        ClientDto dto = new ClientDto();
        dto.setFullName("New Client");
        dto.setContactNumber(number);
        dto.setEmail("new@example.com");
        dto.setAddress("Somewhere");
        return dto;
    }

    @Test
    void saveStoresTheFormattedNumber() {
        stored();

        assertEquals("0917 234 5678", service.saveClient(dto("+63 917-234-5678")).getContactNumber());
        assertEquals("0917 234 5678", service.saveClient(dto("09172345678")).getContactNumber());
        assertEquals("(02) 8123 4567", service.saveClient(dto("(02)  8123 4567")).getContactNumber());
    }

    @Test
    void updateStoresTheFormattedNumber() {
        Client existing = client(5, "Nand", "09172345678");
        stored(existing);
        when(repo.findById(5L)).thenReturn(Optional.of(existing));

        assertEquals("0908 123 9876", service.updateClient(5L, dto("0908-123-9876")).getContactNumber());
    }

    // ---- Email and address: optional (batch 4) --------------------------------------------------

    @Test
    void blankEmailAndAddressAreStoredAsNullNotEmptyStrings() {
        stored();
        ClientDto dto = dto("0917 234 5678");
        dto.setEmail("  ");
        dto.setAddress("");

        Client saved = service.saveClient(dto);
        assertNull(saved.getEmail());
        assertNull(saved.getAddress());
    }

    @Test
    void emailAndAddressAreTrimmedWhenGiven() {
        stored();
        ClientDto dto = dto("0917 234 5678");
        dto.setEmail("  nand@example.com  ");
        dto.setAddress("  123 Test St  ");

        Client saved = service.saveClient(dto);
        assertEquals("nand@example.com", saved.getEmail());
        assertEquals("123 Test St", saved.getAddress());
    }

    @Test
    void updateCanClearAPreviouslySetEmailAndAddress() {
        Client existing = client(5, "Nand", "0917 234 5678");
        existing.setEmail("nand@example.com");
        existing.setAddress("123 Test St");
        stored(existing);
        when(repo.findById(5L)).thenReturn(Optional.of(existing));

        ClientDto dto = dto("0917 234 5678");
        dto.setEmail("");
        dto.setAddress("");
        Client saved = service.updateClient(5L, dto);
        assertNull(saved.getEmail());
        assertNull(saved.getAddress());
    }

    // ---- Photo validation: same limits as ProfilePhotoService -----------------------------------

    private static byte[] pngBytes(int width, int height, int type) {
        try {
            BufferedImage image = new BufferedImage(width, height, type);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static byte[] jpegBytes(int width, int height) {
        try {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static byte[] bmpBytes(int width, int height) {
        try {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "bmp", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static ClientDto dtoWithPhoto(MockMultipartFile photo) {
        ClientDto dto = dto("0917 234 5678");
        dto.setImageFile(photo);
        return dto;
    }

    // A valid photo reaches ClientService.handleFileUpload, which writes to public/images/ relative
    // to the working directory (unmocked here, same as production) - these two tests are the only
    // ones that clear validatePhoto, so they are the only ones that create a real file, and they
    // delete it again immediately, pass or fail, rather than leaving it in the working tree.
    private static void deleteFromPublicImages(String fileName) {
        if (fileName != null) {
            new java.io.File("public/images/" + fileName).delete();
        }
    }

    @Test
    void aValidPngIsAccepted() {
        stored();
        MockMultipartFile photo = new MockMultipartFile("imageFile", "id.png", "image/png", pngBytes(10, 10, BufferedImage.TYPE_INT_ARGB));
        Client saved = service.saveClient(dtoWithPhoto(photo));
        try {
            assertEquals("New Client", saved.getFullName());
        } finally {
            deleteFromPublicImages(saved.getImageFileName());
        }
    }

    @Test
    void aValidJpegIsAccepted() {
        stored();
        MockMultipartFile photo = new MockMultipartFile("imageFile", "id.jpg", "image/jpeg", jpegBytes(10, 10));
        Client saved = service.saveClient(dtoWithPhoto(photo));
        try {
            assertEquals("New Client", saved.getFullName());
        } finally {
            deleteFromPublicImages(saved.getImageFileName());
        }
    }

    @Test
    void noPhotoIsFine() {
        stored();
        MockMultipartFile empty = new MockMultipartFile("imageFile", "", "application/octet-stream", new byte[0]);
        assertEquals("New Client", service.saveClient(dtoWithPhoto(empty)).getFullName());
    }

    @Test
    void aFileOverFiveMegabytesIsRejectedRegardlessOfContent() {
        stored();
        MockMultipartFile tooBig = new MockMultipartFile("imageFile", "id.png", "image/png", new byte[5 * 1024 * 1024 + 1]);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.saveClient(dtoWithPhoto(tooBig)));
        assertTrue(ex.getMessage().contains("5 MB"), ex.getMessage());
    }

    @Test
    void bytesThatAreNotAnyRecognisedImageAreRejected() {
        stored();
        MockMultipartFile junk = new MockMultipartFile("imageFile", "id.png", "image/png", "not an image".getBytes());
        assertThrows(IllegalArgumentException.class, () -> service.saveClient(dtoWithPhoto(junk)));
    }

    @Test
    void aRealImageInAnUnsupportedFormatIsRejectedEvenWithAPngContentType() {
        stored();
        // The declared content type claims PNG; the actual bytes are a valid BMP. Detection goes by
        // the bytes (ImageIO's own format sniffing), not the browser-supplied contentType, so this
        // is rejected as an unsupported format rather than accepted or misread.
        MockMultipartFile bmpDisguisedAsPng = new MockMultipartFile("imageFile", "id.png", "image/png", bmpBytes(10, 10));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.saveClient(dtoWithPhoto(bmpDisguisedAsPng)));
        assertTrue(ex.getMessage().contains("JPG and PNG"), ex.getMessage());
    }

    @Test
    void anImageOverSixteenMillionPixelsIsRejected() {
        stored();
        // 4100 x 4100 = 16,810,000 pixels, just over the 16,000,000 cap. TYPE_BYTE_GRAY keeps this
        // a ~16 MB in-memory buffer rather than the ~67 MB a 32-bit image of the same size would need.
        MockMultipartFile huge = new MockMultipartFile("imageFile", "id.png", "image/png",
                pngBytes(4100, 4100, BufferedImage.TYPE_BYTE_GRAY));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.saveClient(dtoWithPhoto(huge)));
        assertTrue(ex.getMessage().contains("16 million pixels"), ex.getMessage());
    }

    @Test
    void photoValidationRunsOnUpdateToo() {
        Client existing = client(5, "Nand", "0917 234 5678");
        stored(existing);
        when(repo.findById(5L)).thenReturn(Optional.of(existing));

        MockMultipartFile junk = new MockMultipartFile("imageFile", "id.png", "image/png", "not an image".getBytes());
        assertThrows(IllegalArgumentException.class, () -> service.updateClient(5L, dtoWithPhoto(junk)));
    }
}
