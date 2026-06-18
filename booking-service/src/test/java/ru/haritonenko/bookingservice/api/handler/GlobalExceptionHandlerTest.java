package ru.haritonenko.bookingservice.api.handler;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.haritonenko.bookingservice.domain.exception.BookingAvailabilityException;
import ru.haritonenko.bookingservice.domain.exception.BookingNotFoundException;
import ru.haritonenko.bookingservice.domain.exception.IllegalBookingStateException;
import ru.haritonenko.bookingservice.external.circuit.exception.ExternalCircuitBreakerOpenException;
import ru.haritonenko.commonlibs.exception.BookingGuestsOverloadedException;

import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestExceptionController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void shouldReturnNotFoundForBookingNotFoundException() throws Exception {
        mockMvc.perform(get("/test-exceptions/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Booking not found id=1"))
                .andExpect(jsonPath("$.detailedMessage").value("BookingNotFoundException"))
                .andExpect(jsonPath("$.dateTime").isNotEmpty());
    }

    @Test
    void shouldReturnConflictForBookingStateException() throws Exception {
        mockMvc.perform(get("/test-exceptions/state-conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Booking already inactive id=1"))
                .andExpect(jsonPath("$.detailedMessage").value("IllegalBookingStateException"));
    }

    @Test
    void shouldReturnConflictForAvailabilityException() throws Exception {
        mockMvc.perform(get("/test-exceptions/availability-conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("No available rooms"))
                .andExpect(jsonPath("$.detailedMessage").value("BookingAvailabilityException"));
    }

    @Test
    void shouldReturnBadRequestForDomainException() throws Exception {
        mockMvc.perform(get("/test-exceptions/bad-request-domain"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Too many guests"))
                .andExpect(jsonPath("$.detailedMessage").value("BookingGuestsOverloadedException"));
    }

    @Test
    void shouldReturnBadRequestForConstraintViolationException() throws Exception {
        mockMvc.perform(get("/test-exceptions/constraint"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Constraint validation failed"))
                .andExpect(jsonPath("$.detailedMessage").value("constraint failed"));
    }

    @Test
    void shouldReturnBadRequestForIllegalArgumentException() throws Exception {
        mockMvc.perform(get("/test-exceptions/illegal-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid argument"))
                .andExpect(jsonPath("$.detailedMessage").value("IllegalArgumentException"));
    }

    @Test
    void shouldReturnServiceUnavailableForOpenCircuit() throws Exception {
        mockMvc.perform(get("/test-exceptions/open-circuit"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("External service is temporarily unavailable: catalog-service"))
                .andExpect(jsonPath("$.detailedMessage").value("ExternalCircuitBreakerOpenException"));
    }

    @Test
    void shouldReturnInternalServerErrorWithoutLeakingUnexpectedExceptionMessage() throws Exception {
        mockMvc.perform(get("/test-exceptions/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Internal server error"))
                .andExpect(jsonPath("$.message").value(not(containsString("database password"))))
                .andExpect(jsonPath("$.detailedMessage").value("RuntimeException"));
    }

    @Test
    void shouldReturnValidationErrorForInvalidRequestBody() throws Exception {
        mockMvc.perform(post("/test-exceptions/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation Error"))
                .andExpect(jsonPath("$.detailedMessage").value(containsString("name: Name is required")));
    }

    @RestController
    private static class TestExceptionController {

        @GetMapping("/test-exceptions/{type}")
        void throwException(@PathVariable String type) {
            switch (type) {
                case "not-found" -> throw new BookingNotFoundException("Booking not found id=1");
                case "state-conflict" -> throw new IllegalBookingStateException("Booking already inactive id=1");
                case "availability-conflict" -> throw new BookingAvailabilityException("No available rooms");
                case "bad-request-domain" -> throw new BookingGuestsOverloadedException("Too many guests");
                case "constraint" -> throw new ConstraintViolationException("constraint failed", Set.of());
                case "illegal-argument" -> throw new IllegalArgumentException("Invalid argument");
                case "open-circuit" -> throw new ExternalCircuitBreakerOpenException("catalog-service");
                case "unexpected" -> throw new RuntimeException("database password leaked");
                default -> throw new IllegalArgumentException("Unknown exception type");
            }
        }

        @PostMapping("/test-exceptions/validation")
        void validateRequest(@Valid @RequestBody TestValidationRequest request) {
        }
    }

    private record TestValidationRequest(
            @NotBlank(message = "Name is required")
            String name
    ) {
    }
}
