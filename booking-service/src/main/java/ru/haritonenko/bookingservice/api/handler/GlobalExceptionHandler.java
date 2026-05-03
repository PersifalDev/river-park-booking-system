package ru.haritonenko.bookingservice.api.handler;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.haritonenko.bookingservice.domain.exception.BookingAvailabilityException;
import ru.haritonenko.bookingservice.domain.exception.BookingNotFoundException;
import ru.haritonenko.bookingservice.domain.exception.IllegalBookingStateException;
import ru.haritonenko.commonlibs.dto.error.ErrorMessageResponse;

import java.time.OffsetDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorMessageResponse> handleValidationException(MethodArgumentNotValidException ex) {
        log.warn("Got ValidationException", ex);
        String detailedMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> (error.getField() + ": " + error.getDefaultMessage()))
                .collect(Collectors.joining(","));

        return buildErrorResponse(HttpStatus.BAD_REQUEST,"Validation Error",
                detailedMessage);
    }

    @ExceptionHandler(BookingNotFoundException.class)
    public ResponseEntity<ErrorMessageResponse> handleBookingNotFoundException(BookingNotFoundException ex) {
        log.warn("Booking not found exception", ex);
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), ex.getClass().getSimpleName());
    }

    @ExceptionHandler({BookingAvailabilityException.class, IllegalBookingStateException.class})
    public ResponseEntity<ErrorMessageResponse> handleConflictExceptions(RuntimeException ex) {
        log.warn("Booking conflict exception", ex);
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), ex.getClass().getSimpleName());
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
        log.error("Unexpected booking service exception", ex);
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
