package ru.haritonenko.paymentservice.api.handler;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.haritonenko.commonlibs.dto.error.ErrorMessageResponse;
import ru.haritonenko.paymentservice.domain.exception.IllegalPaymentStateException;
import ru.haritonenko.paymentservice.domain.exception.PaymentNotFoundException;

import java.time.OffsetDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorMessageResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        log.warn("Payment validation failed", ex);
        String details = ex.getBindingResult().getFieldErrors().stream().map(error -> error.getField() + ": " + error.getDefaultMessage()).collect(Collectors.joining(", "));
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Validation failed", details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorMessageResponse> handleConstraintViolationException(ConstraintViolationException ex) {
        log.warn("Payment constraint violation", ex);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Constraint validation failed", ex.getMessage());
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ErrorMessageResponse> handlePaymentNotFoundException(PaymentNotFoundException ex) {
        log.warn("Payment not found", ex);
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), ex.getClass().getSimpleName());
    }

    @ExceptionHandler({IllegalPaymentStateException.class, IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ErrorMessageResponse> handleConflict(RuntimeException ex) {
        log.warn("Payment business exception", ex);
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), ex.getClass().getSimpleName());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorMessageResponse> handleException(Exception ex) {
        log.error("Unexpected payment-service exception", ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", ex.getClass().getSimpleName());
    }

    private ResponseEntity<ErrorMessageResponse> buildErrorResponse(HttpStatus status, String message, String details) {
        return ResponseEntity.status(status).body(new ErrorMessageResponse(message, details, OffsetDateTime.now().toString()));
    }
}
