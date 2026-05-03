package ru.haritonenko.catalogservice.photo.utils;

import java.nio.file.Files;
import java.nio.file.Path;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public final class FileUtils {


    private FileUtils() {
    }


    public static Path normalizeRelativePath(String relativePath) {
        if (isNull(relativePath) || relativePath.isEmpty()) {
            throw new IllegalArgumentException("Relative path can't be null or blank");
        }

        String pathWithReplacedSlash = relativePath.replace("\\", "/");

        String normalizedRelativePath = pathWithReplacedSlash.startsWith("/") ?
                pathWithReplacedSlash.substring(1) :
                pathWithReplacedSlash;

        return Path.of(normalizedRelativePath).normalize();
    }

    public static Path resolveFromBaseDir(Path baseDir, String relativePath) {
        if (isNull(baseDir)) {
            throw new IllegalArgumentException("Base directory can't be null");
        }
        if (isNull(relativePath) || relativePath.isEmpty()) {
            throw new IllegalArgumentException("Relative path can't be null or blank");
        }

        Path normalizedRelativePath = normalizeRelativePath(relativePath);

        return baseDir.resolve(normalizedRelativePath).normalize();

    }

    public static boolean exists(Path path) {
        return nonNull(path) && Files.exists(path);
    }

    public static boolean isRegularFile(Path path) {
        return nonNull(path) && Files.isRegularFile(path);
    }

    public static boolean isDirectory(Path path) {
        return nonNull(path) && Files.isDirectory(path);
    }

    public static String toUnixStyleRelativePath(Path path) {
        if (isNull(path)) {
            throw new IllegalArgumentException("Path must not be null");
        }

        return path.toString().replace("\\", "/");
    }

}
