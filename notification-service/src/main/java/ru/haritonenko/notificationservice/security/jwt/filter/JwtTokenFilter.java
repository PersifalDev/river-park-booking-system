package ru.haritonenko.notificationservice.security.jwt.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.haritonenko.commonlibs.security.authorization.user.AuthUser;
import ru.haritonenko.notificationservice.security.jwt.manager.JwtTokenManager;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenFilter extends OncePerRequestFilter {

    private final JwtTokenManager jwtTokenManager;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        String jwt = authorizationHeader.substring(7);
        AuthUser authUser;
        try {
            authUser = jwtTokenManager.getAuthUserFromToken(jwt);
        } catch (Exception ex) {
            log.warn("Failed to parse notification-service jwt", ex);
            filterChain.doFilter(request, response);
            return;
        }
        if (authUser == null || authUser.id() == null || authUser.role() == null || authUser.role().isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(authUser, null, List.of(new SimpleGrantedAuthority(authUser.role())));
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        filterChain.doFilter(request, response);
    }
}
