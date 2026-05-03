package ru.haritonenko.notificationservice.api.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.haritonenko.commonlibs.dto.error.ErrorMessageResponse;

import java.time.OffsetDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorMessageResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Notification business exception", ex);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), ex.getClass().getSimpleName());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorMessageResponse> handleException(Exception ex) {
        log.error("Unexpected notification-service exception", ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", ex.getClass().getSimpleName());
    }

    private ResponseEntity<ErrorMessageResponse> buildErrorResponse(HttpStatus status, String message, String details) {
        return ResponseEntity.status(status).body(new ErrorMessageResponse(message, details, OffsetDateTime.now().toString()));
    }
}
