package com.rnpc.inventory.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.Iterator;

/** Private, account-scoped photos. Configure a persistent volume for multi-instance deployments. */
@Service
public class ProfilePhotoService {
    private final Path root;
    public ProfilePhotoService(@Value("${app.profile-photo-directory:./private/profile-photos}") String directory) {
        root = Path.of(directory).toAbsolutePath().normalize();
    }
    private Path path(Long userId) {
        if (userId == null || userId <= 0) throw new IllegalArgumentException("Invalid account.");
        return root.resolve(userId + ".png");
    }
    public boolean exists(Long userId) { return Files.isRegularFile(path(userId)); }
    public byte[] read(Long userId) throws IOException { return Files.readAllBytes(path(userId)); }
    public void save(Long userId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return;
        if (file.getSize() > 5L * 1024 * 1024) throw new IllegalArgumentException("Choose a JPG or PNG no larger than 5 MB.");
        BufferedImage image;
        try (InputStream input = file.getInputStream(); ImageInputStream stream = ImageIO.createImageInputStream(input)) {
            if (stream == null) throw new IllegalArgumentException("Choose a valid JPG or PNG image.");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw new IllegalArgumentException("Choose a valid JPG or PNG image.");
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName();
                if (!format.equalsIgnoreCase("png") && !format.equalsIgnoreCase("jpeg"))
                    throw new IllegalArgumentException("Only JPG and PNG images are supported.");
                reader.setInput(stream);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > 16000000)
                    throw new IllegalArgumentException("Choose an image with no more than 16 million pixels.");
                image = reader.read(0);
            } finally { reader.dispose(); }
        }
        // Decode and re-encode so uploaded filenames and embedded metadata are never served.
        Files.createDirectories(root);
        Path temp = Files.createTempFile(root, "photo-", ".tmp");
        try {
            if (!ImageIO.write(image, "png", temp.toFile())) throw new IOException("Could not encode image.");
            try { Files.move(temp, path(userId), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temp, path(userId), StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }
}
