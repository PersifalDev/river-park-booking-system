package ru.haritonenko.catalogservice.rule.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import ru.haritonenko.catalogservice.rule.api.dto.RuleDocumentResponseDto;
import ru.haritonenko.catalogservice.rule.domain.service.RuleDocumentService;

@RestController
@RequestMapping("/api/v1/catalog/rules")
@RequiredArgsConstructor
@Slf4j
public class RuleDocumentController {

    private final RuleDocumentService ruleDocumentService;

    @GetMapping("/document")
    public ResponseEntity<RuleDocumentResponseDto> getRuleDocumentInfo() {
        log.info("Request to get hotel rule document metadata");
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ruleDocumentService.getRuleDocumentInfo());
    }

    @GetMapping("/document/file")
    public ResponseEntity<Resource> downloadRuleDocument() {
        log.info("Request to download hotel rule document");

        Resource resource = ruleDocumentService.getRuleDocumentResource();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(ruleDocumentService.getRuleFileName())
                                .build()
                                .toString()
                )
                .body(resource);
    }
}