package ru.haritonenko.catalogservice.rule.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import ru.haritonenko.catalogservice.photo.category.domain.exception.DirectoryNotFoundException;
import ru.haritonenko.catalogservice.photo.utils.FileUtils;
import ru.haritonenko.catalogservice.rule.api.dto.RuleDocumentResponseDto;
import ru.haritonenko.catalogservice.rule.domain.exception.RuleDocumentNotFoundException;
import java.nio.file.Path;

@Service
@Slf4j
@RequiredArgsConstructor
public class RuleDocumentService {

    private static final String RULE_FILE_NAME = "Pravila_uslug_2025.pdf";
    private static final String RULE_FILE_RELATIVE_PATH = "docs/" + RULE_FILE_NAME;
    private static final String RULE_CONTENT_TYPE = "application/pdf";
    private static final String RULE_DOWNLOAD_PATH = "/api/v1/catalog/rules/document/file";
    private static final String RULE_DOCUMENT_NAME = "Правила проживания и услуги";

    @Value("${app.base-dir}")
    private String baseDir;

    public RuleDocumentResponseDto getRuleDocumentInfo() {
        log.info("Getting hotel rule document metadata");

        validateRuleFile();

        return RuleDocumentResponseDto.builder()
                .name(RULE_DOCUMENT_NAME)
                .fileName(RULE_FILE_NAME)
                .contentType(RULE_CONTENT_TYPE)
                .downloadPath(RULE_DOWNLOAD_PATH)
                .build();
    }

    public Resource getRuleDocumentResource() {
        log.info("Getting hotel rule document resource");

        Path filePath = validateRuleFile();
        return new FileSystemResource(filePath);
    }

    public String getRuleFileName() {
        return RULE_FILE_NAME;
    }

    private Path validateRuleFile() {
        Path basePath = Path.of(baseDir);

        if (!FileUtils.exists(basePath) || !FileUtils.isDirectory(basePath)) {
            throw new DirectoryNotFoundException("Base directory was not found: " + basePath);
        }

        Path filePath = basePath.resolve(RULE_FILE_RELATIVE_PATH).normalize();

        if (!FileUtils.exists(filePath) || !FileUtils.isRegularFile(filePath)) {
            throw new RuleDocumentNotFoundException("Rule document was not found by path: " + filePath);
        }

        return filePath;
    }
}