package ru.haritonenko.userservice.security.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import ru.haritonenko.commonlibs.security.authorization.user.UserCredentials;
import ru.haritonenko.userservice.domain.UserRole;
import ru.haritonenko.userservice.domain.db.entity.UserEntity;
import ru.haritonenko.userservice.domain.db.repository.UserRepository;
import ru.haritonenko.userservice.security.jwt.manager.JwtTokenManager;
import ru.haritonenko.userservice.security.refresh.service.RefreshTokenService;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenManager jwtTokenManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    void shouldAuthenticateAndReturnJwt() {
        UserCredentials credentials = new UserCredentials("watson", "secret");
        UserEntity user = UserEntity.builder()
                .id(7L)
                .login("watson")
                .userRole(UserRole.USER)
                .build();
        OffsetDateTime accessExpiresAt = OffsetDateTime.now().plusHours(1);
        OffsetDateTime refreshExpiresAt = OffsetDateTime.now().plusDays(30);
        when(userRepository.findByLogin("watson")).thenReturn(Optional.of(user));
        when(jwtTokenManager.generateToken(7L, "watson", "USER")).thenReturn("jwt-token");
        when(jwtTokenManager.accessTokenExpiresAt()).thenReturn(accessExpiresAt);
        when(refreshTokenService.issue(any(UserEntity.class), any(), any()))
                .thenReturn(new RefreshTokenService.IssuedRefreshToken(null, "refresh-token", refreshExpiresAt));

        var actual = authenticationService.authenticate(credentials, "JUnit", "127.0.0.1");

        assertEquals("jwt-token", actual.jwt());
        assertEquals("refresh-token", actual.refreshToken());
        ArgumentCaptor<UsernamePasswordAuthenticationToken> captor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertEquals("watson", captor.getValue().getPrincipal());
        assertEquals("secret", captor.getValue().getCredentials());
        verify(jwtTokenManager).generateToken(7L, "watson", "USER");
    }
}
