package ru.haritonenko.userservice.api.handler;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.haritonenko.commonlibs.dto.error.ErrorMessageResponse;
import ru.haritonenko.userservice.domain.exception.UserAlreadyRegisteredException;
import ru.haritonenko.userservice.domain.exception.UserNotFoundException;

import java.time.OffsetDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorMessageResponse> handleUserNotFoundException(UserNotFoundException ex) {
        log.warn("User not found exception", ex);
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), ex.getClass().getSimpleName());
    }

    @ExceptionHandler(UserAlreadyRegisteredException.class)
    public ResponseEntity<ErrorMessageResponse> handleUserAlreadyRegisteredException(UserAlreadyRegisteredException ex) {
        log.warn("User already registered exception", ex);
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), ex.getClass().getSimpleName());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorMessageResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("Method argument not valid", ex);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Validation failed", details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorMessageResponse> handleConstraintViolationException(ConstraintViolationException ex) {
        log.warn("Constraint violation exception", ex);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Constraint validation failed", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorMessageResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Illegal argument exception", ex);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), ex.getClass().getSimpleName());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorMessageResponse> handleException(Exception ex) {
        log.error("Unexpected user service exception", ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", ex.getClass().getSimpleName());
    }

    private ResponseEntity<ErrorMessageResponse> buildErrorResponse(HttpStatus status, String message, String details) {
        return ResponseEntity.status(status).body(new ErrorMessageResponse(
                message,
                details,
                OffsetDateTime.now().toString()
        ));
    }
}
