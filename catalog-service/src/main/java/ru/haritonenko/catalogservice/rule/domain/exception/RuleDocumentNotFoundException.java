package ru.haritonenko.catalogservice.rule.domain.exception;

public class RuleDocumentNotFoundException extends RuntimeException {

    public RuleDocumentNotFoundException(String message) {
        super(message);
    }
}