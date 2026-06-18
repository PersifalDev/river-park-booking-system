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
import ru.haritonenko.userservice.domain.User;
import ru.haritonenko.userservice.domain.UserRole;
import ru.haritonenko.userservice.domain.service.UserService;
import ru.haritonenko.userservice.security.jwt.manager.JwtTokenManager;

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
    private UserService userService;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    void shouldAuthenticateAndReturnJwt() {
        UserCredentials credentials = new UserCredentials("watson", "secret");
        when(userService.findByLogin("watson")).thenReturn(new User(7L, "watson", UserRole.USER));
        when(jwtTokenManager.generateToken(7L, "watson", "USER")).thenReturn("jwt-token");

        String actual = authenticationService.authenticate(credentials);

        assertEquals("jwt-token", actual);
        ArgumentCaptor<UsernamePasswordAuthenticationToken> captor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertEquals("watson", captor.getValue().getPrincipal());
        assertEquals("secret", captor.getValue().getCredentials());
        verify(jwtTokenManager).generateToken(7L, "watson", "USER");
    }
}
