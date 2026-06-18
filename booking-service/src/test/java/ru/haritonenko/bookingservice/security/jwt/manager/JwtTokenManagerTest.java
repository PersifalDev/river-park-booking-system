package ru.haritonenko.bookingservice.security.jwt.manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtTokenManagerTest {

    private JwtTokenManager jwtTokenManager;

    @BeforeEach
    void setUp() {
        jwtTokenManager = new JwtTokenManager();
        ReflectionTestUtils.setField(jwtTokenManager, "keyString", "01234567890123456789012345678901");
        ReflectionTestUtils.setField(jwtTokenManager, "expirationTime", 60_000L);
        jwtTokenManager.init();
    }

    @Test
    void shouldGenerateAndReadAuthUser() {
        String token = jwtTokenManager.generateToken(42L, "watson", "USER");

        var authUser = jwtTokenManager.getAuthUserFromToken(token);

        assertEquals(42L, authUser.id());
        assertEquals("watson", authUser.login());
        assertEquals("USER", authUser.role());
    }
}
