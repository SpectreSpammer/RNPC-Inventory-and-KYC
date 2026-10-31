package com.rnpc.inventory.util;

import com.rnpc.inventory.util.ImageUploadValidator.InvalidPhotoException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The shared photo screen (batch V1). The rules are the ones ClientService and ProfilePhotoService
 * already had - 5 MB, PNG or JPEG by the actual bytes, 16 million pixels - and the messages are the
 * client form's, word for word. No Spring context, no files written.
 */
class ImageUploadValidatorTest {

    private static final String NOT_VALID = "Choose a valid JPG or PNG image.";
    private static final String TOO_BIG = "Choose a JPG or PNG no larger than 5 MB.";
    private static final String UNSUPPORTED = "Only JPG and PNG images are supported.";
    private static final String TOO_MANY_PIXELS = "Choose an image with no more than 16 million pixels.";

    private static byte[] encode(int width, int height, int type, String format) {
        try {
            BufferedImage image = new BufferedImage(width, height, type);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            assertEquals(true, ImageIO.write(image, format, out), "no ImageIO writer for " + format);
            return out.toByteArray();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static MockMultipartFile file(String name, String contentType, byte[] bytes) {
        return new MockMultipartFile("imageFile", name, contentType, bytes);
    }

    private static String rejectionOf(MockMultipartFile file) {
        InvalidPhotoException ex = assertThrows(InvalidPhotoException.class, () -> ImageUploadValidator.validate(file));
        return ex.getMessage();
    }

    @Test
    void aValidPngIsAccepted() {
        assertDoesNotThrow(() -> ImageUploadValidator.validate(
                file("id.png", "image/png", encode(10, 10, BufferedImage.TYPE_INT_ARGB, "png"))));
    }

    @Test
    void aValidJpegIsAccepted() {
        assertDoesNotThrow(() -> ImageUploadValidator.validate(
                file("id.jpg", "image/jpeg", encode(10, 10, BufferedImage.TYPE_INT_RGB, "jpg"))));
    }

    @Test
    void aFileOverFiveMegabytesIsRejectedWhateverItContains() {
        MockMultipartFile tooBig = file("id.png", "image/png", new byte[5 * 1024 * 1024 + 1]);

        assertEquals(TOO_BIG, rejectionOf(tooBig));
    }

    @Test
    void exactlyFiveMegabytesPassesTheSizeCheckAndIsThenJudgedOnItsBytes() {
        // The limit is "more than 5 MB": a file of exactly that size gets as far as the bytes check.
        MockMultipartFile atLimit = file("id.png", "image/png", new byte[5 * 1024 * 1024]);

        assertEquals(NOT_VALID, rejectionOf(atLimit));
    }

    @Test
    void textRenamedToJpgIsRejectedEvenWithAJpegContentType() {
        MockMultipartFile fake = file("holiday.jpg", "image/jpeg", "this is not an image".getBytes());

        assertEquals(NOT_VALID, rejectionOf(fake));
    }

    @Test
    void aRealImageInAnotherFormatIsRejectedEvenWithAPngNameAndContentType() {
        MockMultipartFile bmp = file("id.png", "image/png", encode(10, 10, BufferedImage.TYPE_INT_RGB, "bmp"));

        assertEquals(UNSUPPORTED, rejectionOf(bmp));
    }

    @Test
    void aGifIsRejected() {
        MockMultipartFile gif = file("id.gif", "image/gif", encode(10, 10, BufferedImage.TYPE_INT_RGB, "gif"));

        assertEquals(UNSUPPORTED, rejectionOf(gif));
    }

    @Test
    void aGifWithAJpgNameIsStillRejected() {
        MockMultipartFile gif = file("id.jpg", "image/jpeg", encode(10, 10, BufferedImage.TYPE_INT_RGB, "gif"));

        assertEquals(UNSUPPORTED, rejectionOf(gif));
    }

    @Test
    void anImageOverSixteenMillionPixelsIsRejected() {
        // 4100 x 4100 = 16,810,000 pixels, just over the cap. TYPE_BYTE_GRAY keeps the in-memory
        // buffer near 16 MB rather than the ~67 MB a 32-bit image of the same size would need.
        MockMultipartFile huge = file("id.png", "image/png", encode(4100, 4100, BufferedImage.TYPE_BYTE_GRAY, "png"));

        assertEquals(TOO_MANY_PIXELS, rejectionOf(huge));
    }

    @Test
    void anEmptyFileIsRejectedBecauseZeroBytesAreNotAnImage() {
        // Callers skip a null or empty upload ("no photo chosen") before calling this; asked directly,
        // the validator refuses it.
        assertEquals(NOT_VALID, rejectionOf(file("id.png", "image/png", new byte[0])));
    }

    @Test
    void aNullFileIsRejectedRatherThanThrowingANullPointer() {
        InvalidPhotoException ex = assertThrows(InvalidPhotoException.class, () -> ImageUploadValidator.validate(null));

        assertEquals(NOT_VALID, ex.getMessage());
    }

    @Test
    void theRejectionIsAnIllegalArgumentExceptionSoExistingCallersKeepWorking() {
        MockMultipartFile fake = file("holiday.jpg", "image/jpeg", "nope".getBytes());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ImageUploadValidator.validate(fake));

        assertInstanceOf(InvalidPhotoException.class, ex);
    }
}
