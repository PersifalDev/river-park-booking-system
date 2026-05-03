package ru.haritonenko.userservice.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import ru.haritonenko.commonlibs.security.authorization.user.AuthUser;
import ru.haritonenko.commonlibs.security.authorization.user.UserCredentials;
import ru.haritonenko.userservice.domain.User;
import ru.haritonenko.userservice.domain.service.UserService;
import ru.haritonenko.userservice.security.jwt.manager.JwtTokenManager;

import static java.util.Objects.isNull;


@Slf4j
@RequiredArgsConstructor
@Service
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenManager jwtTokenManager;
    private final UserService userService;

    public String authenticate(UserCredentials userFromSignInRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        userFromSignInRequest.login(),
                        userFromSignInRequest.password()
                )
        );

        User user = userService.findByLogin(userFromSignInRequest.login());

        log.info("Generating jwt token");
        return jwtTokenManager.generateToken(
                user.id(),
                user.login(),
                user.role().toString()
        );
    }

    public AuthUser getCurrentAuthenticatedUser() {
        log.info("Getting authenticated user");
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (isNull(authentication)) {
            log.warn("Error while getting authenticated user");
            throw new IllegalStateException("Authentication not present");
        }
        return (AuthUser) authentication.getPrincipal();
    }
}