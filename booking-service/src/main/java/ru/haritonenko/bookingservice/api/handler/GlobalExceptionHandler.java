package ru.haritonenko.bookingservice.api.handler;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.haritonenko.bookingservice.domain.exception.BookingAvailabilityException;
import ru.haritonenko.bookingservice.domain.exception.BookingIdempotencyConflictException;
import ru.haritonenko.bookingservice.domain.exception.BookingNotFoundException;
import ru.haritonenko.bookingservice.domain.exception.BookingTariffNotApplicableException;
import ru.haritonenko.bookingservice.domain.exception.BookingTariffNotFoundException;
import ru.haritonenko.bookingservice.domain.exception.IllegalBookingStateException;
import ru.haritonenko.bookingservice.domain.exception.BookingHoldFailedException;
import ru.haritonenko.bookingservice.external.circuit.exception.ExternalCircuitBreakerOpenException;
import ru.haritonenko.bookingservice.kafka.outbox.exception.KafkaBookingEventIllegalStateException;
import ru.haritonenko.bookingservice.kafka.outbox.exception.KafkaEventNotFoundException;
import ru.haritonenko.bookingservice.tasks.domain.exception.AsyncBookingTaskNotFoundException;
import ru.haritonenko.commonlibs.dto.error.ErrorMessageResponse;
import ru.haritonenko.commonlibs.exception.BookingGuestsOverloadedException;
import ru.haritonenko.commonlibs.exception.CategoryIllegalArgumentException;
import ru.haritonenko.commonlibs.exception.RoomCategoryNotFoundException;
import ru.haritonenko.commonlibs.exception.UserIllegalArgumentException;

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

    @ExceptionHandler({
            BookingNotFoundException.class,
            BookingTariffNotFoundException.class,
            RoomCategoryNotFoundException.class,
            AsyncBookingTaskNotFoundException.class,
            KafkaEventNotFoundException.class
    })
    public ResponseEntity<ErrorMessageResponse> handleBookingNotFoundException(RuntimeException ex) {
        log.warn("Not found exception", ex);
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), ex.getClass().getSimpleName());
    }

    @ExceptionHandler({
            BookingAvailabilityException.class,
            BookingHoldFailedException.class,
            BookingTariffNotApplicableException.class,
            BookingIdempotencyConflictException.class,
            IllegalBookingStateException.class,
            KafkaBookingEventIllegalStateException.class
    })
    public ResponseEntity<ErrorMessageResponse> handleConflictExceptions(RuntimeException ex) {
        log.warn("Booking conflict exception", ex);
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), ex.getClass().getSimpleName());
    }

    @ExceptionHandler({
            BookingGuestsOverloadedException.class,
            CategoryIllegalArgumentException.class,
            UserIllegalArgumentException.class
    })
    public ResponseEntity<ErrorMessageResponse> handleBadRequestDomainExceptions(RuntimeException ex) {
        log.warn("Bad request domain exception", ex);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), ex.getClass().getSimpleName());
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

    @ExceptionHandler(ExternalCircuitBreakerOpenException.class)
    public ResponseEntity<ErrorMessageResponse> handleExternalCircuitBreakerOpenException(ExternalCircuitBreakerOpenException ex) {
        log.warn("External circuit breaker is open", ex);
        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), ex.getClass().getSimpleName());
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
