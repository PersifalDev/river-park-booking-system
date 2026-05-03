package ru.haritonenko.commonlibs.dto.rule;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RuleDocumentResponseDto(
        String name,
        String fileName,
        String contentType,
        String downloadPath
) {
}
