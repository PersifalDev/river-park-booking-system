package ru.haritonenko.paymentservice.security.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import ru.haritonenko.commonlibs.security.authorization.user.AuthUser;

@Service
@Slf4j
public class AuthenticationService {

    public AuthUser getCurrentAuthenticatedUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUser authUser)) {
            log.warn("Authenticated user not found in payment-service security context");
            throw new IllegalStateException("Authenticated user not found");
        }
        return authUser;
    }
}
