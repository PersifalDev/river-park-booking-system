package ru.haritonenko.commonlibs.security.authorization.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserCredentials(
        @NotBlank(message = "User login can not be blank")
        @Size(min = 4, max = 50, message = "Min login size is 4, max is 50")
        String login,
        @NotBlank(message = "User password can not be blank")
        @Size(min = 4, max = 50, message = "Min password size is 4, max is 50")
        String password
) {
}
