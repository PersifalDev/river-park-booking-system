package ru.haritonenko.catalogservice.photo.category.loader;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.haritonenko.catalogservice.photo.category.domain.exception.PhotoNotFoundException;
import ru.haritonenko.catalogservice.photo.utils.FileUtils;
import java.nio.file.Path;

@Slf4j
@Service
public class RoomCategoryPhotoLoader {

    @Value("${app.base-dir}")
    private String baseDir;

    public Path validateRelativePath(String relativePathString) {
        log.debug("Resolving photo by relative path={}", relativePathString);

        Path normalizedPath = FileUtils.normalizeRelativePath(relativePathString);
        Path resolvedPath = FileUtils.resolveFromBaseDir(Path.of(baseDir), normalizedPath.toString());

        boolean existsAndFile = checkBothFileExistsAndItIsFile(resolvedPath);

        if (!existsAndFile) {
            logPhotoNotFoundOrItIsNotRegularFile(resolvedPath);
            throw new PhotoNotFoundException(
                    "Photo was not found by path: " + resolvedPath
            );
        }

        return normalizedPath;
    }

    public String getUnixStylePath(Path path){
        return FileUtils.toUnixStyleRelativePath(path);
    }

    private boolean checkBothFileExistsAndItIsFile(Path path) {
        return FileUtils.exists(path) && FileUtils.isRegularFile(path);
    }

    private void logPhotoNotFoundOrItIsNotRegularFile(Path path) {
        log.warn("Photo was not found or it is not a regular file, path={}", path);
    }
}