package com.rnpc.inventory.util;

import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * The one screen for an uploaded photo before it is written anywhere (client and repair photos).
 * At most 5 MB, and the declared content type and file name are never trusted - the actual bytes
 * must decode via ImageIO as PNG or JPEG and be no more than 16 million pixels. The file is only
 * screened here, not re-encoded; ProfilePhotoService has its own copy of these rules (it also
 * re-encodes to PNG) and is deliberately not routed through this class.
 *
 * Callers decide what "no upload" means (a null or empty file is skipped before this is called);
 * given an empty file this rejects it, since zero bytes are not an image.
 */
public final class ImageUploadValidator {

    public static final long MAX_BYTES = 5L * 1024 * 1024;
    public static final long MAX_PIXELS = 16_000_000L;

    /**
     * An IllegalArgumentException so existing callers that catch that keep working, but a type of
     * its own so a controller can tell "the photo was refused" from any other bad argument (an
     * unknown client id, say) and attach the message to the photo field only.
     */
    public static class InvalidPhotoException extends IllegalArgumentException {
        public InvalidPhotoException(String message) {
            super(message);
        }
    }

    private ImageUploadValidator() {
    }

    /** Returns normally for an acceptable photo, otherwise throws InvalidPhotoException with a user-facing message. */
    public static void validate(MultipartFile file) {
        if (file == null) {
            throw new InvalidPhotoException("Choose a valid JPG or PNG image.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new InvalidPhotoException("Choose a JPG or PNG no larger than 5 MB.");
        }
        try (InputStream input = file.getInputStream(); ImageInputStream stream = ImageIO.createImageInputStream(input)) {
            if (stream == null) {
                throw new InvalidPhotoException("Choose a valid JPG or PNG image.");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
                throw new InvalidPhotoException("Choose a valid JPG or PNG image.");
            }
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName();
                if (!format.equalsIgnoreCase("png") && !format.equalsIgnoreCase("jpeg")) {
                    throw new InvalidPhotoException("Only JPG and PNG images are supported.");
                }
                reader.setInput(stream);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > MAX_PIXELS) {
                    throw new InvalidPhotoException("Choose an image with no more than 16 million pixels.");
                }
            } finally {
                reader.dispose();
            }
        } catch (IOException ex) {
            throw new InvalidPhotoException("The photo could not be read. Please choose a valid image and try again.");
        }
    }
}
