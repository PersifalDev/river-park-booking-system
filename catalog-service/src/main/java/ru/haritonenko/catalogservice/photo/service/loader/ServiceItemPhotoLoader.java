package ru.haritonenko.catalogservice.photo.service.loader;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.haritonenko.catalogservice.photo.category.domain.exception.PhotoNotFoundException;
import ru.haritonenko.catalogservice.photo.utils.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@Slf4j
@Service
public class ServiceItemPhotoLoader {

    @Value("${app.base-dir}")
    private String baseDir;

    public Path validateRelativePath(String relativePathString) {
        Path normalizedPath = FileUtils.normalizeRelativePath(relativePathString);
        Path basePath = Path.of(baseDir);
        Path resolvedPath = FileUtils.resolveFromBaseDir(basePath, normalizedPath.toString());

        boolean existsAndFile = FileUtils.exists(resolvedPath) && FileUtils.isRegularFile(resolvedPath);
        if (existsAndFile) {
            return normalizedPath;
        }

        Path fallbackPath = resolveFallbackPath(basePath, normalizedPath);
        if (fallbackPath != null) {
            log.info("Resolved fallback service photo path={} for requestedPath={}", fallbackPath, normalizedPath);
            return fallbackPath;
        }

        log.warn("Service photo was not found or it is not a regular file, path={}", resolvedPath);
        throw new PhotoNotFoundException("Photo was not found by path: " + resolvedPath);
    }

    private Path resolveFallbackPath(Path basePath, Path normalizedPath) {
        Path directory = basePath.resolve(normalizedPath).getParent();
        if (directory == null || !Files.isDirectory(directory)) {
            return null;
        }

        String requestedFileName = normalizedPath.getFileName().toString().toLowerCase(Locale.ROOT);
        String requestedStem = removeExtension(requestedFileName);

        try (Stream<Path> stream = Files.list(directory)) {
            List<Path> imageFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(this::isImageFile)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
                    .toList();

            for (Path imageFile : imageFiles) {
                String candidateName = imageFile.getFileName().toString().toLowerCase(Locale.ROOT);
                String candidateStem = removeExtension(candidateName);
                if (candidateName.equals(requestedFileName)
                        || candidateStem.equals(requestedStem)
                        || candidateStem.replace("-", "_").equals(requestedStem.replace("-", "_"))) {
                    return basePath.relativize(imageFile);
                }
            }

            if (imageFiles.size() == 1) {
                return basePath.relativize(imageFiles.getFirst());
            }
        } catch (IOException exception) {
            log.warn("Failed to scan service photo directory={}", directory, exception);
        }

        return null;
    }

    private boolean isImageFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return fileName.endsWith(".png")
                || fileName.endsWith(".jpg")
                || fileName.endsWith(".jpeg")
                || fileName.endsWith(".webp");
    }

    private String removeExtension(String fileName) {
        int extensionIndex = fileName.lastIndexOf('.');
        return extensionIndex < 0 ? fileName : fileName.substring(0, extensionIndex);
    }

    public String getUnixStylePath(Path path) {
        return FileUtils.toUnixStyleRelativePath(path);
    }
}
