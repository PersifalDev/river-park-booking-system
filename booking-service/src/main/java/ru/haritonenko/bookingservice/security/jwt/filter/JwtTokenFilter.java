package ru.haritonenko.bookingservice.security.jwt.filter;

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
import ru.haritonenko.bookingservice.security.jwt.manager.JwtTokenManager;
import ru.haritonenko.commonlibs.security.authorization.user.AuthUser;

import java.io.IOException;
import java.util.List;

import static java.util.Objects.isNull;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenFilter extends OncePerRequestFilter {

    private final JwtTokenManager jwtTokenManager;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (isNull(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authorizationHeader.substring(7);

        AuthUser user;
        try {
            user = jwtTokenManager.getAuthUserFromToken(jwt);
        } catch (Exception ex) {
            log.warn("Error while reading jwt", ex);
            filterChain.doFilter(request, response);
            return;
        }

        if (isNull(user) || isNull(user.id()) || isNull(user.role())) {
            filterChain.doFilter(request, response);
            return;
        }

        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                user,
                null,
                List.of(new SimpleGrantedAuthority(user.role()))
        );

        SecurityContextHolder.getContext().setAuthentication(token);
        filterChain.doFilter(request, response);
    }
}