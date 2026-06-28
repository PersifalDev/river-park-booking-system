package ru.haritonenko.userservice.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.haritonenko.commonlibs.security.authorization.user.AuthUser;
import ru.haritonenko.commonlibs.security.authorization.user.UserCredentials;
import ru.haritonenko.userservice.domain.db.entity.UserEntity;
import ru.haritonenko.userservice.domain.db.repository.UserRepository;
import ru.haritonenko.userservice.domain.exception.UserNotFoundException;
import ru.haritonenko.userservice.security.jwt.manager.JwtTokenManager;
import ru.haritonenko.userservice.security.jwt.response.JwtResponse;
import ru.haritonenko.userservice.security.refresh.service.RefreshTokenService;

import static java.util.Objects.isNull;


@Slf4j
@RequiredArgsConstructor
@Service
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenManager jwtTokenManager;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public JwtResponse authenticate(UserCredentials userFromSignInRequest, String userAgent, String ipAddress) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        userFromSignInRequest.login(),
                        userFromSignInRequest.password()
                )
        );

        UserEntity user = userRepository.findByLogin(userFromSignInRequest.login())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        log.info("Generating jwt token");
        String accessToken = jwtTokenManager.generateToken(
                user.getId(),
                user.getLogin(),
                user.getUserRole().authority()
        );
        var refreshToken = refreshTokenService.issue(user, userAgent, ipAddress);
        return JwtResponse.of(
                accessToken,
                refreshToken.token(),
                jwtTokenManager.accessTokenExpiresAt(),
                refreshToken.expiresAt()
        );
    }

    @Transactional
    public JwtResponse refresh(String refreshToken, String userAgent, String ipAddress) {
        var rotated = refreshTokenService.rotate(refreshToken, userAgent, ipAddress);
        UserEntity user = rotated.user();
        String accessToken = jwtTokenManager.generateToken(
                user.getId(),
                user.getLogin(),
                user.getUserRole().authority()
        );
        return JwtResponse.of(
                accessToken,
                rotated.token(),
                jwtTokenManager.accessTokenExpiresAt(),
                rotated.expiresAt()
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
