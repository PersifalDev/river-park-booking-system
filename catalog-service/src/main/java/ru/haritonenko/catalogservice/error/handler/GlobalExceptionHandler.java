package ru.haritonenko.catalogservice.error.handler;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.haritonenko.catalogservice.category.domain.exception.RoomCategoryNotFoundException;
import ru.haritonenko.catalogservice.photo.category.domain.exception.DirectoryNotFoundException;
import ru.haritonenko.catalogservice.photo.category.domain.exception.InvalidPathException;
import ru.haritonenko.catalogservice.photo.category.domain.exception.PhotoListNotFoundException;
import ru.haritonenko.catalogservice.photo.category.domain.exception.PhotoNotFoundException;
import ru.haritonenko.catalogservice.rule.domain.exception.RuleDocumentNotFoundException;
import ru.haritonenko.catalogservice.services.domain.exception.ServiceItemNotFoundException;
import ru.haritonenko.commonlibs.dto.error.ErrorMessageResponse;


import java.time.LocalDateTime;
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
        var errorDto = getErrorMessageResponse("Validation Error",
                detailedMessage);

        return ResponseEntity.
                status(HttpStatus.BAD_REQUEST)
                .body(errorDto);
    }

    @ExceptionHandler(RoomCategoryNotFoundException.class)
    public ResponseEntity<ErrorMessageResponse> handleRoomCategoryNotFoundException(
            RoomCategoryNotFoundException ex
    ) {
        log.warn("Got RoomCategoryNotFoundException", ex);
        var errorDto = getErrorMessageResponse("Category search error",
                ex.getMessage());
        return ResponseEntity.
                status(HttpStatus.NOT_FOUND)
                .body(errorDto);
    }

    @ExceptionHandler(DirectoryNotFoundException.class)
    public ResponseEntity<ErrorMessageResponse> handleDirectoryNotFoundException(
            DirectoryNotFoundException ex
    ) {
        log.warn("Got DirectoryNotFoundException", ex);
        var errorDto = getErrorMessageResponse("Directory search error",
                ex.getMessage());
        return ResponseEntity.
                status(HttpStatus.NOT_FOUND)
                .body(errorDto);
    }

    @ExceptionHandler(InvalidPathException.class)
    public ResponseEntity<ErrorMessageResponse> handleInvalidPathException(
            InvalidPathException ex
    ) {
        log.warn("Got InvalidPathException", ex);
        var errorDto = getErrorMessageResponse("Invalid path error",
                ex.getMessage());
        return ResponseEntity.
                status(HttpStatus.BAD_REQUEST)
                .body(errorDto);
    }

    @ExceptionHandler(PhotoNotFoundException.class)
    public ResponseEntity<ErrorMessageResponse> handlePhotoNotFoundException(
            PhotoNotFoundException ex
    ) {
        log.error("Got PhotoNotFoundException", ex);
        var errorDto = getErrorMessageResponse("Photo search error",
                ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorDto);
    }

    @ExceptionHandler(PhotoListNotFoundException.class)
    public ResponseEntity<ErrorMessageResponse> handlePhotoListNotFoundException(
            PhotoListNotFoundException ex
    ) {
        log.warn("Got PhotoListNotFoundException", ex);
        var errorDto = getErrorMessageResponse("Photo list search error",
                ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorDto);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorMessageResponse> handleIllegalArgumentException(
            IllegalArgumentException ex
    ) {
        log.warn("Got IllegalArgumentException", ex);
        var errorDto = getErrorMessageResponse("Illegal argument error",
                ex.getMessage());
        return ResponseEntity.
                status(HttpStatus.BAD_REQUEST)
                .body(errorDto);
    }

    @ExceptionHandler(ServiceItemNotFoundException.class)
    public ResponseEntity<ErrorMessageResponse> handleServiceItemNotFoundException(
            ServiceItemNotFoundException ex
    ) {
        log.warn("Got ServiceItemNotFoundException", ex);
        var errorDto = getErrorMessageResponse("Hotel service search error",
                ex.getMessage());
        return ResponseEntity.
                status(HttpStatus.NOT_FOUND)
                .body(errorDto);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorMessageResponse> handleIllegalStateException(
            IllegalStateException ex
    ) {
        log.warn("Got IllegalStateException", ex);
        var errorDto = getErrorMessageResponse("Illegal state error",
                ex.getMessage());
        return ResponseEntity.
                status(HttpStatus.CONFLICT)
                .body(errorDto);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorMessageResponse> handleConstraintViolationException(
            ConstraintViolationException ex
    ) {
        log.warn("Got ConstraintViolationException", ex);
        String detailedMessage = ex.getConstraintViolations()
                .stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining(","));
        var errorDto = getErrorMessageResponse("Validation Error", detailedMessage);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorDto);
    }

    @ExceptionHandler(RuleDocumentNotFoundException.class)
    public ResponseEntity<ErrorMessageResponse> handleRuleDocumentNotFoundException(
            RuleDocumentNotFoundException ex
    ) {
        log.warn("Got RuleDocumentNotFoundException", ex);
        var errorDto = getErrorMessageResponse("Rule document search error", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorDto);
    }

    private ErrorMessageResponse getErrorMessageResponse(
            String message,
            String detailedMessage
    ) {
        return new ErrorMessageResponse(
                message,
                detailedMessage,
                LocalDateTime.now().toString()
        );
    }
}