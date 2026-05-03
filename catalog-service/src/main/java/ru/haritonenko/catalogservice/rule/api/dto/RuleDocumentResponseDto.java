package ru.haritonenko.catalogservice.rule.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RuleDocumentResponseDto(
        String name,
        String fileName,
        String contentType,
        String downloadPath
) {
}