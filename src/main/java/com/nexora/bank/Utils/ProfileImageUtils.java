package com.nexora.bank.Utils;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;

public final class ProfileImageUtils {
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024L * 1024L;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".png", ".jpg", ".jpeg", ".gif");

    private ProfileImageUtils() {
    }

    public static String storeProfileImage(File sourceFile, int userId) {
        if (sourceFile == null || !sourceFile.exists() || !sourceFile.isFile()) {
            throw new IllegalArgumentException("Invalid image file.");
        }
        if (userId <= 0) {
            throw new IllegalArgumentException("Invalid user id.");
        }
        if (sourceFile.length() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("Image is too large. Maximum size is 5 MB.");
        }

        String extension = extractExtension(sourceFile.getName());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Unsupported image format. Use PNG, JPG or GIF.");
        }

        Path storageDir = resolveStorageDirectory();
        try {
            Files.createDirectories(storageDir);
            String targetName = "user_" + userId + "_" + System.currentTimeMillis() + extension;
            Path targetPath = storageDir.resolve(targetName);
            Files.copy(sourceFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return targetPath.toAbsolutePath().toString();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to save profile image.", ex);
        }
    }

    public static Image loadImageOrNull(String storedPath, double requestedWidth, double requestedHeight) {
        if (!hasStoredImage(storedPath)) {
            return null;
        }
        try {
            String imageUri = Path.of(storedPath).toUri().toString();
            return new Image(imageUri, requestedWidth, requestedHeight, true, true);
        } catch (Exception ex) {
            return null;
        }
    }

    public static boolean hasStoredImage(String storedPath) {
        if (storedPath == null || storedPath.trim().isEmpty()) {
            return false;
        }
        try {
            return Files.exists(Path.of(storedPath.trim()));
        } catch (Exception ex) {
            return false;
        }
    }

    public static void applyCircularClip(ImageView imageView, double diameter) {
        if (imageView == null || diameter <= 0) {
            return;
        }
        double radius = diameter / 2.0;
        imageView.setClip(new Circle(radius, radius, radius));
    }

    private static Path resolveStorageDirectory() {
        return Path.of(System.getProperty("user.home"), ".nexora-bank", "profile-images");
    }

    private static String extractExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot >= fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot).toLowerCase(Locale.ROOT);
    }
}
