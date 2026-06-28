package ru.haritonenko.userservice.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserRegistration(
        @NotBlank(message = "User login can't be blank")
        @Size(min = 4, max = 50, message = "Min login size is 4, max is 50")
        String login,
        @NotBlank(message = "User key can't be blank")
        @Size(min = 4, max = 50, message = "Min key size is 4, max is 50")
        String key,
        @NotNull(message = "Personal data consent must be provided")
        @AssertTrue(message = "Personal data consent must be accepted")
        Boolean personalDataConsentAccepted,
        @NotNull(message = "Privacy policy must be provided")
        @AssertTrue(message = "Privacy policy must be accepted")
        Boolean privacyPolicyAccepted
) {
}
