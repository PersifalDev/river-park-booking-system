package ru.haritonenko.paymentservice.security.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import ru.haritonenko.commonlibs.security.authorization.user.AuthUser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthenticationServiceTest {

    private final AuthenticationService authenticationService = new AuthenticationService();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnCurrentAuthenticatedUser() {
        AuthUser authUser = AuthUser.builder().id(10L).login("watson").role("USER").build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(authUser, null)
        );

        AuthUser actual = authenticationService.getCurrentAuthenticatedUser();

        assertEquals(10L, actual.id());
        assertEquals("watson", actual.login());
    }

    @Test
    void shouldThrowWhenAuthenticationIsMissing() {
        assertThrows(IllegalStateException.class, authenticationService::getCurrentAuthenticatedUser);
    }
}
